package ch.pritz.batchwriter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Datenuebertragungsobjekt fuer eine Chat-Nachricht aus RabbitMQ.
 */
public class MessageDto {

    private UUID id;
    private UUID roomId;
    private String sender;
    private String text;
    private OffsetDateTime sentAt;

    /**
     * Standardkonstruktor fuer Jackson Deserialisierung.
     */
    public MessageDto() {
    }

    /**
     * Konstruktor mit allen Feldern.
     *
     * @param id Eindeutige Nachrichten-ID
     * @param roomId ID des Chat-Raums
     * @param sender Absender der Nachricht
     * @param text Inhalt der Nachricht
     * @param sentAt Sendezeitpunkt
     */
    public MessageDto(UUID id, UUID roomId, String sender, String text, OffsetDateTime sentAt) {
        this.id = id;
        this.roomId = roomId;
        this.sender = sender;
        this.text = text;
        this.sentAt = sentAt;
    }

    /**
     * Gibt die Nachrichten-ID zurueck.
     *
     * @return id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Setzt die Nachrichten-ID.
     *
     * @param id Nachrichten-ID
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gibt die Raum-ID zurueck.
     *
     * @return roomId
     */
    public UUID getRoomId() {
        return roomId;
    }

    /**
     * Setzt die Raum-ID.
     *
     * @param roomId Raum-ID
     */
    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    /**
     * Gibt den Absender zurueck.
     *
     * @return sender
     */
    public String getSender() {
        return sender;
    }

    /**
     * Setzt den Absender.
     *
     * @param sender Absender
     */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
     * Gibt den Nachrichtentext zurueck.
     *
     * @return text
     */
    public String getText() {
        return text;
    }

    /**
     * Setzt den Nachrichtentext.
     *
     * @param text Nachrichtentext
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gibt den Sendezeitpunkt zurueck.
     *
     * @return sentAt
     */
    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    /**
     * Setzt den Sendezeitpunkt.
     *
     * @param sentAt Sendezeitpunkt
     */
    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }
}