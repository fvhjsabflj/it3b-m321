package ch.pritz.batchwriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener-Klasse zum Empfangen von Nachrichten-Batches aus RabbitMQ.
 */
@Component
public class MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);
    private final MessageWriterRepository repository;

    /**
     * Konstruktor zur Injektion des Repositorys.
     *
     * @param repository MessageWriterRepository Instanz
     */
    public MessageListener(MessageWriterRepository repository) {
        this.repository = repository;
    }

    /**
     * Empfaengt ein Batch von Nachrichten aus der Queue chat.persist und schreibt sie in die DB.
     * Beinhaltet Fail-Safe-Logik: Ungueltige Nachrichten oder unkorrigierbare Fehler werden
     * isoliert verworfen, um Endlosschleifen (Poison Pills) zu verhindern.
     *
     * @param messages Liste der konsumierten Nachrichten
     */
    @RabbitListener(queues = "chat.persist")
    public void receiveBatch(List<MessageDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 1. Vorab-Validierung: Filtere korrupte/ungueltige Nachrichten (Poison-Pill-Schutz)
        List<MessageDto> validMessages = new ArrayList<>();
        for (MessageDto msg : messages) {
            if (msg != null && msg.getId() != null && msg.getRoomId() != null) {
                validMessages.add(msg);
            } else {
                log.warn("Ungueltige oder beschaedigte Nachricht erkannt und verworfen: {}", msg);
            }
        }

        if (validMessages.isEmpty()) {
            return;
        }

        try {
            // Versuche den schnellen Batch-Insert
            repository.saveAllInBatch(validMessages);
            log.info("{} Nachrichten erfolgreich in Datenbank geschrieben.", validMessages.size());
        } catch (Exception e) {
            log.warn("Batch-Insert fehlgeschlagen. Starte Einzel-Fallback, um Endlosschleifen zu verhindern...", e);

            // 2. Fallback: Speichere Nachrichten einzeln.
            // Wenn eine einzelne Nachricht kaputt ist, wird nur diese verworfen, der Rest gerettet.
            for (MessageDto msg : validMessages) {
                try {
                    repository.save(msg);
                } catch (Exception singleEx) {
                    log.error("Kritischer Fehler bei Nachricht ID: {}. Nachricht wird verworfen (Poison Pill).", msg.getId(), singleEx);
                    // Hier absichtlich NICHT weiterwerfen, damit die kaputte Nachricht die Queue nicht blockiert!
                }
            }
        }
    }
}