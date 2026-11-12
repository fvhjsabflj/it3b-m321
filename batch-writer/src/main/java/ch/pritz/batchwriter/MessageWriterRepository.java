package ch.pritz.batchwriter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository zum Speichern von Chat-Nachrichten in PostgreSQL.
 */
@Repository
public class MessageWriterRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Konstruktor zur Injektion des JdbcTemplate.
     *
     * @param jdbcTemplate JdbcTemplate Instanz
     */
    public MessageWriterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Speichert eine Liste von Nachrichten in einem einzigen Batch-INSERT in der Datenbank.
     * Verwendet ON CONFLICT (id) DO NOTHING zur Vermeidung von Duplikaten (Szenario S5).
     *
     * @param messages Liste der zu speichernden Nachrichten
     */
    public void saveAllInBatch(List<MessageDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO message (id, room_id, sender, text, sent_at) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (id) DO NOTHING";

        List<Object[]> batchArgs = new ArrayList<>();
        for (MessageDto msg : messages) {
            Object[] params = new Object[] {
                    msg.getId(),
                    msg.getRoomId(),
                    msg.getSender(),
                    msg.getText(),
                    msg.getSentAt() != null ? Timestamp.from(msg.getSentAt().toInstant()) : null
            };
            batchArgs.add(params);
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    /**
     * Speichert eine einzelne Nachricht in der Datenbank (Fail-Safe Fallback).
     *
     * @param msg Die zu speichernde Nachricht
     */
    public void save(MessageDto msg) {
        if (msg == null || msg.getId() == null) {
            return;
        }

        String sql = "INSERT INTO message (id, room_id, sender, text, sent_at) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (id) DO NOTHING";

        jdbcTemplate.update(sql,
                msg.getId(),
                msg.getRoomId(),
                msg.getSender(),
                msg.getText(),
                msg.getSentAt() != null ? Timestamp.from(msg.getSentAt().toInstant()) : null
        );
    }
}