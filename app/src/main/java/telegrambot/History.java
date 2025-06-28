package telegrambot;

import java.util.ArrayList;
import java.util.List;

public class History {
    public record ChatMessage(String role, String content) {}
    private final List<ChatMessage> history;
    
    History() {
        history = new ArrayList<>();
    }

    public void add(String role, String content) {
        history.add(new ChatMessage(role, content));
    }

    public List<ChatMessage> getMessages() {
        return new ArrayList<>(history); // Return a copy to prevent external modification
    }
  
    public List<ChatMessage> getLastNMessages(int n) {
        if (n >= history.size()) {
            return new ArrayList<>(history); // Return all messages if n is larger than the history size
        } else {
            int startIndex = history.size() - n;
            return new ArrayList<>(history.subList(startIndex, history.size()));
        }
    }
    

}
