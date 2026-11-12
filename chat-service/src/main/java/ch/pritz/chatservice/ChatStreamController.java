package ch.pritz.chatservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controller zur Bereitstellung des SSE-Streams für Live-Chat-Nachrichten inklusive Last-Event-ID Support.
 */
@Controller
public class ChatStreamController {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamController.class);
    private final List<ClientSession> sessions = new CopyOnWriteArrayList<>();

    // Puffer für die letzten Nachrichten pro Raum (für Last-Event-ID Reconnects)
    private final Map<UUID, List<MessageDto>> messageHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 100;

    /**
     * Öffnet einen Server-Sent Events (SSE) Stream für einen bestimmten Raum.
     * Prüft optional den Last-Event-ID Header, um verpasste Nachrichten nachzuliefern.
     *
     * @param roomId Die ID des Chat-Raums
     * @param lastEventId Optionaler Header mit der ID der letzten empfangenen Nachricht
     * @return SseEmitter für die dauerhafte Verbindung
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream(
            @RequestParam("roomId") UUID roomId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ClientSession session = new ClientSession(roomId, emitter);
        sessions.add(session);

        emitter.onCompletion(() -> sessions.remove(session));
        emitter.onTimeout(() -> sessions.remove(session));
        emitter.onError(ex -> sessions.remove(session));

        // Asynchron verpasste Nachrichten nachliefern, falls Last-Event-ID gesetzt ist
        if (lastEventId != null && !lastEventId.isBlank()) {
            sendMissedMessages(session, lastEventId);
        }

        return emitter;
    }

    /**
     * Konsumiert Nachrichten aus der instanzspezifischen RabbitMQ-Queue und verteilt sie
     * an alle verbundenen Clients im gleichen Chat-Raum.
     *
     * @param message Die empfangene Chat-Nachricht
     */
    @RabbitListener(queues = "#{instanceDeliveryQueue.name}")
    public void handleMessage(MessageDto message) {
        if (message == null || message.getRoomId() == null) {
            return;
        }

        // Nachricht im Puffer speichern
        messageHistory.computeIfAbsent(message.getRoomId(), k -> new CopyOnWriteArrayList<>()).add(message);
        List<MessageDto> history = messageHistory.get(message.getRoomId());
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }

        for (ClientSession session : sessions) {
            if (session.roomId.equals(message.getRoomId())) {
                sendEvent(session, message);
            }
        }
    }

    /**
     * Hilfsmethode zum Senden einer Nachricht über den SseEmitter.
     */
    private void sendEvent(ClientSession session, MessageDto message) {
        try {
            session.emitter.send(SseEmitter.event()
                    .id(message.getId() != null ? message.getId().toString() : null)
                    .data(message, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.error("Fehler beim Senden der Nachricht an SSE-Client", e);
            sessions.remove(session);
        }
    }

    /**
     * Sendet alle Nachrichten nach der gegebenen Last-Event-ID an den reconnectenden Client nach.
     */
    private void sendMissedMessages(ClientSession session, String lastEventId) {
        List<MessageDto> history = messageHistory.get(session.roomId);
        if (history == null || history.isEmpty()) {
            return;
        }

        boolean found = false;
        for (MessageDto msg : history) {
            if (found && msg.getId() != null) {
                sendEvent(session, msg);
            }
            if (msg.getId() != null && msg.getId().toString().equals(lastEventId)) {
                found = true;
            }
        }
    }

    /**
     * Interne Hilfsklasse zur Verwaltung aktiver Client-Sessions.
     */
    private static class ClientSession {
        private final UUID roomId;
        private final SseEmitter emitter;

        public ClientSession(UUID roomId, SseEmitter emitter) {
            this.roomId = roomId;
            this.emitter = emitter;
        }
    }
}