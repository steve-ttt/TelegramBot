// This file is part of Telegram Bot SDK.
package telegrambot;
import java.io.IOException;
import java.util.List;
import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Bot extends TelegramLongPollingBot {

  private final Config config;
  // Use a Map to store a separate history for each chat.
  // The key is the chat ID, and the value is the History object for that chat.
  private final Map<Long, History> chatHistories;
    private final String historyFile;

  public Bot(Config config, String historyFile) {
    this.config = config;
    this.historyFile = historyFile;
    this.chatHistories = SaveLoadHistories.loadHistories(historyFile);
  }

  @Override
  public String getBotUsername() {
      return "Ada Lovelace Bot";
  }

  @Override
  public String getBotToken() {
      return config.getBotToken();
  }

  @Override
  public void onUpdateReceived(Update update) {
    // This method is called when an update is received.
    // We check if the update has a message and the message has text.
    if (update.hasMessage() && update.getMessage().hasText()) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        String user = update.getMessage().getFrom().getFirstName();
        System.out.println(user + " (" + chatId + ") wrote: " + messageText);

        // Get or create the history for this specific chat.
        History chatHistory = chatHistories.computeIfAbsent(chatId, k -> new History());

        chatHistory.add("user", messageText); // Add the new user message.
        try {
            SaveLoadHistories.saveHistories(this.historyFile, chatHistories);
        } catch (IOException e) {
            System.err.println("unable to save file: " + this.historyFile + " - " + e.getMessage());
        }
        List<History.ChatMessage> messagesToSend = chatHistory.getLastNMessages(config.getMaxHistoryMessages());
        try {
            // Let the user know we are working on it.
            // This is a good practice for potentially long-running operations.
            // sendText(chatId, "Thinking...");

            String response =
                LLMinterface.chatCompletion( 
                    messagesToSend,
                    config.getOllamaModel(),
                    config.getSystemPrompt(),
                    config.getOllamaUrl(), // Pass num_ctx
                    config.getNumCtx(),
                    config.getTemperature());
            chatHistory.add("assistant", response);
            persistCurrentHistories();
            sendText(chatId, response);
        } catch (Exception e) {
            System.err.println("Error calling LLM: " + e.getMessage());
            e.printStackTrace();
            sendText(chatId, "Sorry, I had a problem getting a response from the AI model.");
        }
    }
  }

  public void sendText(Long who, String what){
   SendMessage sm = SendMessage.builder() // Create a message object
                    .chatId(who.toString()) //Who are we sending a message to
                    .text(what)
                    .parseMode(ParseMode.HTML) // Tell Telegram to parse this message as HTML
                    .build();
   try {
        execute(sm);                        //Actually sending the message
   } catch (TelegramApiException e) {
        // Log the error instead of crashing the bot thread.
        System.err.println("Failed to send message to chat ID " + who + ". Error: " + e.getMessage());
   }
}

  private void persistCurrentHistories() {
    try {
        SaveLoadHistories.saveHistories(this.historyFile, chatHistories);
    } catch (IOException e) {
        System.err.println("Failed to persist chat histories: " + e.getMessage());
    }
  }

  /**
   * Saves the current state of all chat histories to the file.
   * This is intended for shutdown hooks to ensure data is not lost.
   */
  public void persistHistoriesOnShutdown() {
      System.out.println("Shutdown hook triggered. Saving chat histories...");
      persistCurrentHistories();
      System.out.println("Chat histories saved successfully.");
  }
}