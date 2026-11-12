# Umsetzungsplan: Live-Zustellung (Bewertung 2)

Dieser Plan dient als Drehbuch für die Implementierung des SSE-Anzeigeweges im `chat-service`. Jeder Schritt ist in sich abgeschlossen, enthält einen Test und wird einzeln committet[cite: 21].

## Schritt 1: Fanout-Exchange und Instanz-Queues in RabbitMQ konfigurieren
* **Ziel:** Einen Fanout-Exchange einrichten und sicherstellen, dass jede Instanz eine temporäre, exklusive Queue mit maximal 1000 Nachrichten (Schutz vor hängenden Instanzen, Szenario S6) erhält[cite: 20].
* **Warum an dieser Stelle:** Ohne die RabbitMQ-Struktur und den Exchange können Nachrichten nicht an die Instanzen verteilt werden.
* **KI-Auftrag im Wortlaut:** "Erstelle eine RabbitMQ-Konfiguration mit einem Fanout-Exchange für Chat-Nachrichten und einer Factory für temporäre, exklusive Queues pro Instanz inklusive einer Längenbegrenzung von maximal 1000 Nachrichten."
* **Test:** Integrationstest prüft, ob die Queue am Exchange angebunden wird.
* **Geprüft und korrigiert:** [Wird nach Durchführung ausgefüllt]

## Schritt 2: Implementierung des SSE-Endpunkts (`GET /stream`)
* **Ziel:** Den Endpunkt `GET /stream?roomId=...` mit dem Content-Type `text/event-stream` implementieren, der Nachrichten im korrekten JSON-Format und mit Nachrichten-ID ausgibt[cite: 20].
* **Warum an dieser Stelle:** Dies ist der Kern des Anzeigeweges, über den Clients die Nachrichten live empfangen.
* **KI-Auftrag im Wortlaut:** "Schreibe einen Spring Boot Controller mit einem SSE-Emitter Endpunkt für `GET /stream`, der Nachrichten nach Raum-ID filtert und im SSE-Format (`id:` und `data:`) ausgibt."
* **Test:** Ein Test verifiziert den korrekten HTTP-Header `text/event-stream` und die Event-Struktur.
* **Geprüft und korrigiert:** [Wird nach Durchführung ausgefüllt]

## Schritt 3: Unterstützung von `Last-Event-ID` für Reconnects (S7)
* **Ziel:** Erkennung des Headers `Last-Event-ID` bei Neuverbindungen, um verpasste Nachrichten in der richtigen Reihenfolge nachzuliefern[cite: 20].
* **Warum an dieser Stelle:** Netzwerkausfälle bei Clients dürfen nicht zum Datenverlust führen.
* **KI-Auftrag im Wortlaut:** "Ergänze den SSE-Stream so, dass bei einem eingehenden `Last-Event-ID`-Header die dazugehörigen Puffer-Nachrichten vor dem Live-Stream an den Client gesendet werden."
* **Test:** Testfall S7 simuliert Verbindungsabbruch, sendet Nachrichten und prüft die lückenlose Nachlieferung via `Last-Event-ID`.
* **Geprüft und korrigiert:** [Wird nach Durchführung ausgefüllt]

## Schritt 4: Finale Integrationstests und Szenarien-Absicherung (S1-S8)
* **Ziel:** Alle Tests grün machen (`mvn test`) und sicherstellen, dass Multi-Instanz-Verhalten (S3, S4) und Datenbank-Pufferung (S5) fehlerfrei greifen[cite: 20].
* **Warum an dieser Stelle:** Absicherung der Gesamtlösung vor dem Setzen des Tags `bewertung-2`.
* **KI-Auftrag im Wortlaut:** "Führe alle Unit- und Integrationstests zusammen und korrigiere eventuelle Konnektivitätsprobleme mit RabbitMQ in Testumgebungen."
* **Test:** `mvn test` läuft komplett grün durch[cite: 21].
* **Geprüft und korrigiert:** [Wird nach Durchführung ausgefüllt]