package ch.pritz.chatservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Haupteinstiegspunkt für den Chat-Service (Live-Zustellung).
 */
@SpringBootApplication
public class ChatServiceApplication {

    /**
     * Startet die Spring Boot Anwendung des Chat-Services.
     *
     * @param args Kommandozeilenargumente
     */
    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}