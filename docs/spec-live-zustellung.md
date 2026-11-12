# Spezifikation: Live-Zustellung (Bewertung 2)

## 1. Zweck und Abgrenzung
Der Dienst `chat-service` erweitert die Chat-App um den Live-Anzeigeweg. Nachrichten, die über `POST /messages` eingehen, werden über einen Fanout-Exchange in RabbitMQ verteilt. Jede aktive Instanz des `chat-service` empfängt diese Nachrichten über eine eigene exklusive Queue und stellt sie den verbundenen Clients in Echtzeit über Server-Sent Events (SSE) an dem Endpunkt `GET /stream?roomId=...` zur Verfügung.

**Nicht Teil dieser Aufgabe:** Keycloak, Web-Gateway, React-Oberfläche, JavaFX-Client sowie das Anlegen von Räumen oder Einladen von Mitgliedern[cite: 19].

## 2. Vertrag
* **Eingang (Exchange):** Nachrichten kommen im selben JSON-Format an wie für den `batch-writer` (`MessageDto`: `id`, `roomId`, `sender`, `text`, `sentAt`).
* **Ausgang (SSE Stream):** Der Endpunkt `GET /stream?roomId=...` liefert den Content-Type `text/event-stream`. Jedes Event ist wie folgt formatiert:
  ```text
  id: <nachrichten-id>
  data: {"id":"...","roomId":"...","sender":"...","text":"...","sentAt":"..."}