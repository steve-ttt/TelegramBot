package telegrambot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LLMinterface {

  /*
   * curl http://localhost:11434/api/generate -d '{
   * "model": "llama3",
   * "messages": [
   *   { "role": "user", "content": "why is the sky blue?" } 
   *    ],
   * "stream": false
   * }'
   *
   *   
   */

  public static String chatCompletion(
      List<History.ChatMessage> messages, String model, String systemPrompt, String ollamaUrl, int numCtx, double temperature)
      throws IOException, InterruptedException {
    ObjectMapper objectMapper = new ObjectMapper();

    // Prepare messages for the request. Prepend system prompt if provided.
    List<History.ChatMessage> requestMessages = new ArrayList<>();
    if (systemPrompt != null && !systemPrompt.isEmpty()) {
      requestMessages.add(new History.ChatMessage("system", systemPrompt));
    }
    requestMessages.addAll(messages);

    OllamaChatRequest payload = new OllamaChatRequest(model, requestMessages, numCtx, temperature);
    String requestBody = objectMapper.writeValueAsString(payload);

    // For debugging: Let's print the exact request details we are sending.
    System.out.println("--- Sending Request to Ollama ---");
    System.out.println("URL: " + ollamaUrl + "/api/chat");
    System.out.println("Payload: " + requestBody);
    System.out.println("---------------------------------");

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(ollamaUrl + "/api/chat"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    System.out.println("--- Received Response from Ollama ---");
    System.out.println("Status Code: " + response.statusCode());
    System.out.println("Body: " + response.body());
    System.out.println("-----------------------------------");

    if (response.statusCode() != 200) {
      throw new IOException(
          "Request to Ollama failed with status code "
              + response.statusCode()
              + " and body: "
              + response.body());
    }

    OllamaChatResponse ollamaResponse =
        objectMapper.readValue(response.body(), OllamaChatResponse.class);

    if (ollamaResponse.message() == null || ollamaResponse.message().content() == null) {
      throw new IOException(
          "Invalid response from Ollama: message content is null. Body: " + response.body());
    }

    return formatResponseForTelegram(ollamaResponse.message().content());
  }

  private static final int TELEGRAM_MESSAGE_LIMIT = 4096; // Telegram's message character limit

  /**
   * Formats the raw response from the LLM for display in Telegram. It extracts content within
   * {@code <think>} tags and formats it as an HTML spoiler, ensuring the total message length
   * does not exceed Telegram's limit by truncating the spoiler content if necessary.
   *
   * @param text The raw text response from the LLM.
   * @return A formatted string ready for Telegram with HTML parsing.
   */
  private static String formatResponseForTelegram(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }

    String mainAnswer = text;
    String rawThinkingProcess = "";
    String formattedThinkingProcess = "";

    // Pattern to find the <think>...</think> block.
    // (?s) enables DOTALL mode, so '.' matches newlines.
    // (.*?) non-greedy match for content before/after.
    // Group 1: text before <think>
    // Group 2: the entire <think>...</think> block
    // Group 3: content inside <think>
    // Group 4: text after </think>
    Pattern pattern = Pattern.compile("(?s)(.*?)(<think>(.*?)</think>)(.*)");
    Matcher matcher = pattern.matcher(text);

    if (matcher.matches()) {
      String beforeThink = matcher.group(1).trim();
      rawThinkingProcess = matcher.group(3).trim(); // Content inside <think>
      String afterThink = matcher.group(4).trim();

      // Combine parts that are NOT the thinking process for the main answer
      mainAnswer = (beforeThink + (beforeThink.isEmpty() || afterThink.isEmpty() ? "" : "\n") + afterThink).trim();

      // Format the thinking process as a spoiler
      if (!rawThinkingProcess.isEmpty()) {
        formattedThinkingProcess = "<tg-spoiler><b>Thinking Process:</b>\n" + rawThinkingProcess + "</tg-spoiler>";
      }
    }

    // Now, combine mainAnswer and formattedThinkingProcess
    String finalMessage = "";
    if (!formattedThinkingProcess.isEmpty()) {
      
      finalMessage += formattedThinkingProcess;
      finalMessage += "\n"; // Ensure there's a newline after the spoiler
      finalMessage += mainAnswer; // Append the main answer after the spoiler
    }

    // Check if the final message exceeds the limit and truncate the spoiler if needed
    if (finalMessage.length() > TELEGRAM_MESSAGE_LIMIT) {
      int mainAnswerLength = mainAnswer.length();
      int separatorLength = mainAnswer.isEmpty() || formattedThinkingProcess.isEmpty() ? 0 : 2; // for "\n\n"
      int spoilerWrapperOverhead = "<tg-spoiler><b>Thinking Process:</b>\n</tg-spoiler>".length();
      String ellipsis = "\n... (thinking process truncated)";

      int maxRawThinkingContentLength = TELEGRAM_MESSAGE_LIMIT - mainAnswerLength - separatorLength - spoilerWrapperOverhead - ellipsis.length();

      if (maxRawThinkingContentLength < 0) { // Not enough space for even the spoiler wrapper + ellipsis
        return mainAnswer.substring(0, Math.min(mainAnswer.length(), TELEGRAM_MESSAGE_LIMIT)).trim(); // Return main answer, truncated if necessary
      }

      String truncatedRawThinkingProcess = rawThinkingProcess.substring(0, Math.min(rawThinkingProcess.length(), maxRawThinkingContentLength)) + ellipsis;
      formattedThinkingProcess = "<tg-spoiler><b>Thinking Process:</b>\n" + truncatedRawThinkingProcess + "</tg-spoiler>";

      
      // Rebuild the final message with the truncated thinking process
      finalMessage = formattedThinkingProcess;
      finalMessage += mainAnswer;
    }

    return finalMessage.trim();
  }

  private record OllamaChatRequest(
      @JsonProperty("model") String model,
      @JsonProperty("messages") List<History.ChatMessage> messages,
      @JsonProperty("options") Options options, // New field for options
      @JsonProperty("stream") boolean stream) { // Stream remains at the top level
    /**
     * A convenience constructor to build the request payload easily. It sets stream to false.
     */
    public OllamaChatRequest(String model, List<History.ChatMessage> messages, int numCtx, double temperature) {
      this(model, messages, new Options(temperature, numCtx), false);
    }

    // Nested record for the 'options' object
    private record Options(
        @JsonProperty("temperature") Double temperature,
        @JsonProperty("num_ctx") Integer numCtx
    ) {}
  }



  @JsonIgnoreProperties(ignoreUnknown = true)
  private record OllamaChatResponse(@JsonProperty("message") History.ChatMessage message) {}
}
