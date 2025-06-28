package telegrambot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// import com.fasterxml.jackson.core.exc.StreamReadException;
// import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.File;


public class SaveLoadHistories {
    private static ObjectMapper mapper = new ObjectMapper();

    public static void saveHistories(String fileName, Map<Long, History> chatHistories) throws IOException {
        try {
            mapper.writeValue(new File(fileName), chatHistories);
        } catch (IOException e) {
            System.err.println("Error saving chat histories to " + fileName + ": " + e.getMessage());
            throw e; // Re-throw the exception after logging
        }
    }

    public static Map<Long, History> loadHistories(String fileName) {
        Map<Long, History> chatHistories = new ConcurrentHashMap<>();
        try {
            chatHistories = mapper.readValue(
                new File(fileName),
                new com.fasterxml.jackson.core.type.TypeReference<ConcurrentHashMap<Long, History>>() {}
            );
        } catch (IOException e) {
            System.err.println("Error reading from chat histories file: " + fileName + " : " + e.getMessage());
        } 
        return chatHistories;
    }
}


