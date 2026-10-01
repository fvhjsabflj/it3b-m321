# Spezifikation: batch-writer

## 1. Zweck und Abgrenzung

### Zweck
Der `batch-writer` liest Nachrichten aus der RabbitMQ-Queue `chat.persist` und schreibt
sie dauerhaft in die PostgreSQL-Tabelle `message`. Er ist der einzige Dienst, der in
diese Tabelle schreibt.

### Bewusst nicht Teil dieses Dienstes
- Lesen der Chat-Historie (macht der `chat-service` über `GET /api/messages`)
- Raumverwaltung und Mitgliedschaften
- Authentifizierung / Keycloak
- Das web-gateway
- Der load-generator

### Warum überhaupt bündeln?
Laut PLANUNG.md ist die Applikation auf **1'667 Nachrichten pro Sekunde** ausgelegt. Ein
einzelnes `INSERT` pro Nachricht wäre bei dieser Last der Flaschenhals. Der
`batch-writer` sammelt deshalb mehrere Nachrichten und schreibt sie in einer gemeinsamen
Transaktion.

## 2. Vertrag

Was auf `chat.persist` ankommt (Quelle: `docs/plan-chat-service.md`, der Teil, der das
Veröffentlichen von Nachrichten beschreibt):

- Header: `content_type: application/json`
- Body (JSON):

| Feld | Typ | Herkunft |
|---|---|---|
| `id` | UUID (String) | vom `chat-service` vergeben; ist der Primärschlüssel in `message` |
| `roomId` | UUID (String) | vom Client beim `POST /api/messages` mitgeschickt |
| `sender` | String | Benutzername |
| `text` | String | Nachrichtentext |
| `sentAt` | String (ISO-8601) | vom `chat-service` gesetzt, nicht von der Datenbank |

Woher ich das weiss: `docs/plan-chat-service.md` legt dieses Format beim Entwurf von
`POST /api/messages` fest. Der `chat-service` ergänzt beim Veröffentlichen zusätzlich
`id` und `sentAt`, bevor die Nachricht auf die Queue geht — der Client schickt nur
`roomId`, `sender` und `text`.

## 3. Verhalten

### Normalfall (S3)
Der `batch-writer` konsumiert mit aktivierter Batch-Funktion (Grösse 500, Zeitfenster
200 ms). Sobald 500 Nachrichten da sind oder 200 ms vergangen sind — je nachdem, was
zuerst eintritt — schreibt er alle gesammelten Nachrichten in einem `INSERT` mit
mehreren Werten und bestätigt (ACK) erst danach.

### Neustart mit Rückstau (S4)
Die Queue ist durable: Nachrichten gehen nicht verloren, während der `batch-writer`
gestoppt ist. Beim Start arbeitet er den Rückstau in Batches zu 500 ab. Bei 1000
wartenden Nachrichten sind das 2 Transaktionen — weit unter der Grenze von 100.

### Doppelte Nachricht (S5)
`id` ist Primärschlüssel. Das Insert läuft als `INSERT ... ON CONFLICT (id) DO NOTHING`.
Eine zweite Nachricht mit derselben `id` fügt keine neue Zeile ein, verursacht aber auch
keinen Fehler — sie gilt als erfolgreich verarbeitet und wird normal bestätigt (ACK).
Sie landet nicht in der Dead-Letter-Queue `chat.dlq`, weil sie nie abgelehnt
(NACK/reject) wird.

### Zwei Instanzen gleichzeitig (S6)
RabbitMQ verteilt die Nachrichten einer Queue automatisch auf mehrere Consumer
(Competing Consumers). Jede Instanz erhält disjunkte Nachrichten. Sollte es durch eine
Wiederzustellung — z. B. nach einem kurzen Verbindungsabbruch — doch zu einer
Überschneidung kommen, schützt dieselbe `ON CONFLICT (id) DO NOTHING`-Regel wie bei S5
davor, dass eine Nachricht doppelt gespeichert wird.

### Datenbank kurz nicht erreichbar (S7)
Schlägt das Schreiben einer Batch wegen eines Datenbankfehlers fehl, wirft der Listener
eine Exception. Spring AMQP liefert die Nachrichten dann standardmässig sofort erneut
aus (Re-Queue) — ohne Wartezeit würde das bei einem länger andauernden Ausfall zu einer
Endlosschleife führen, die die CPU unnötig belastet. Der `batch-writer` legt deshalb vor
jedem erneuten Versuch eine feste Pause ein (z. B. 2 Sekunden). Die Nachrichten gehen
dabei nie verloren, weil sie ohne ACK sicher in der Queue bleiben. Läuft die Datenbank
wieder, gelingt der nächste Versuch automatisch, ohne dass der Dienst neu gestartet
werden muss.

## 4. Datenmodell und Konfiguration

Der `batch-writer` legt keine eigene Tabelle an und führt selbst keine
Schema-Migrationen (DDL) aus. Das Schema in `db/01-schema.sql` wird einmalig beim ersten
Start des Postgres-Containers angewendet (`/docker-entrypoint-initdb.d`); der
`batch-writer` setzt zur Laufzeit ausschliesslich DML ab (`INSERT`).

Er schreibt in die bereits vorhandene Tabelle `message`:

| Spalte | Typ | Bemerkung |
|---|---|---|
| `id` | UUID, PK | vom `chat-service`, nicht `DEFAULT gen_random_uuid()` |
| `room_id` | UUID, FK → `room(id)` | |
| `sender` | VARCHAR(255) | |
| `text` | TEXT | |
| `sent_at` | TIMESTAMPTZ | vom `chat-service` gesetzt |

**Index:** Der Primärschlüssel `id` erzeugt automatisch einen eindeutigen B-Tree-Index.
Der ist nicht nur eine Optimierung, sondern zwingend nötig — `INSERT ... ON CONFLICT
(id)` funktioniert in PostgreSQL nur, weil für `id` ein eindeutiger Index existiert.

Umgebungsvariablen (in `.env.example`):

| Variable | Wert | Zweck |
|---|---|---|
| `POSTGRES_USER` | chat | DB-Zugangsdaten, bereits vorhanden; Docker Compose reicht sie als `SPRING_DATASOURCE_USERNAME` an den `batch-writer`-Container weiter |
| `POSTGRES_PASSWORD` | chat | s. o., als `SPRING_DATASOURCE_PASSWORD` |
| `POSTGRES_DB` | chat | s. o., Teil der `SPRING_DATASOURCE_URL` |
| `SPRING_RABBITMQ_HOST` | rabbitmq | neu: Hostname des RabbitMQ-Containers im Netz `chat-net` |
| `SPRING_RABBITMQ_PORT` | 5672 | neu: AMQP-Port |
| `BATCH_SIZE` | 500 | neu: Nachrichten pro Transaktion |
| `BATCH_TIMEOUT_MS` | 200 | neu: maximales Warten, bevor auch eine kleinere Batch geschrieben wird |

Die JDBC-URL braucht zusätzlich `reWriteBatchedInserts=true`, damit PostgreSQL die
Batch-Inserts tatsächlich gebündelt ausführt statt intern wieder einzeln.

**Netzwerk:** Der `batch-writer` bekommt in `docker-compose.yml` keinen `ports:`-Eintrag.
Er kommuniziert ausschliesslich über das interne Netz `chat-net` mit Postgres und
RabbitMQ (Anforderung S2 — kein Dienst veröffentlicht einen Port).

## 5. Abnahmekriterien

| # | Kriterium | Befehl |
|---|---|---|
| S3 | Nach ≤ 60 s stehen alle 1000 Zeilen in `message`, Queue leer | `docker compose exec postgres psql -U $POSTGRES_USER -d $POSTGRES_DB -c "SELECT COUNT(*) FROM message;"` und `docker compose exec rabbitmq rabbitmqctl list_queues` |
| S4 | Nach Neustart alle 1000 da, ≤ 100 Transaktionen | gleiche `SELECT COUNT(*)`-Abfrage; Transaktionsanzahl ergibt sich aus Batch-Grösse 500 (= 2 Transaktionen) |
| S5 | Dieselbe `id` zweimal → genau 1 Zeile, 0 in der DLQ | `SELECT COUNT(*) FROM message WHERE id = '...';` = 1; `rabbitmqctl list_queues` für die DLQ = 0 |
| S6 | 2 Instanzen, 1000 Nachrichten, keine doppelt | `SELECT COUNT(*) FROM message;` = 1000; `SELECT id, COUNT(*) FROM message GROUP BY id HAVING COUNT(*) > 1;` liefert keine Zeile |
| S7 | DB-Ausfall 15 s, nach ≤ 90 s alle 300 da, kein manueller Neustart | `docker compose ps` zeigt `batch-writer` durchgehend laufend; `SELECT COUNT(*)` erreicht 300 |
| S8 | Code folgt CLAUDE.md, `.env` nicht im Repo | `git ls-files \| grep .env` liefert nichts; Durchsicht auf Streams |
