package ch.pritz.chatservice;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Datenübertragungsobjekt für eine Chat-Nachricht im Chat-Service.
 */
public class MessageDto {

    private UUID id;
    private UUID roomId;
    private String sender;
    private String text;
    private OffsetDateTime sentAt;

    /**
     * Standardkonstruktor für Jackson-Deserialisierung.
     */
    public MessageDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }
}