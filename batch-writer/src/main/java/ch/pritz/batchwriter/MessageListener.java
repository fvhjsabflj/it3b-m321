package ch.pritz.batchwriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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
     * Bei DB-Fehler wartet der Listener 2 Sekunden und wirft die Exception weiter (Szenario S7),
     * damit Spring AMQP ein Re-Queue ohne CPU-Spins durchfuehrt.
     *
     * @param messages Liste der konsumierten Nachrichten
     */
    @RabbitListener(queues = "chat.persist")
    public void receiveBatch(List<MessageDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        try {
            repository.saveAllInBatch(messages);
            log.info("{} Nachrichten erfolgreich in Datenbank geschrieben.", messages.size());
        } catch (Exception e) {
            log.error("Fehler beim Schreiben der Batch in die Datenbank. Warte 2 Sekunden vor Retry...", e);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            throw e;
        }
    }
}