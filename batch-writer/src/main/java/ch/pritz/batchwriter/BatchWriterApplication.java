package ch.pritz.batchwriter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Haupteinstiegspunkt fuer die BatchWriter Anwendung.
 */
@SpringBootApplication
public class BatchWriterApplication {

    /**
     * Startet die Spring Boot Anwendung.
     *
     * @param args Kommandozeilenargumente
     */
    public static void main(String[] args) {
        SpringApplication.run(BatchWriterApplication.class, args);
    }
}