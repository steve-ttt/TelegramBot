package telegrambot;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
    @JsonProperty("botToken")
    private String botToken;

    @JsonProperty("ollamaUrl")
    private String ollamaUrl;

    @JsonProperty("ollamaModel")
    private String ollamaModel;

    @JsonProperty("systemPrompt")
    private String systemPrompt;

    @JsonProperty("maxHistoryMessages")
    private int maxHistoryMessages;
    
    @JsonProperty("numCtx")
    private int numCtx;

    @JsonProperty("temperature")
    private double temperature;

    // Getters for the fields
    public String getBotToken() {
        return botToken;
    }

    public String getOllamaUrl() {
        return ollamaUrl;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public int getMaxHistoryMessages() {
        return maxHistoryMessages;
    }
    
    public int getNumCtx() {
        return numCtx;
    }

    public double getTemperature() {
        return temperature;
    }
}