# Umsetzungsplan: batch-writer

Vorbild: `docs/plan-chat-service.md`. Jeder Punkt unten = ein Commit, in genau dieser
Reihenfolge — so deckt sich der Plan mit dem `git log`.

1. **Maven-Modul anlegen.** `batch-writer/` im Eltern-POM registrieren, Spring-Boot-
   Grundgerüst mit `spring-boot-starter-amqp` und `spring-boot-starter-jdbc`.
   Test: Anwendung startet, loggt "Started BatchWriterApplication".
   Warum zuerst: ohne lauffähiges Grundgerüst lässt sich kein weiterer Schritt testen.

2. **Einzelne Nachricht konsumieren und einzeln einfügen.** `@RabbitListener` auf
   `chat.persist`, `JdbcTemplate.update(...)` pro Nachricht.
   Test: eine Testnachricht landet als eine Zeile in `message`.
   Warum an dieser Stelle: einfachster Pfad zuerst, bevor Bündelung dazukommt — zeigt
   früh, dass der Weg von der Queue bis in die Tabelle funktioniert.

3. **Auf Batch-Konsum umstellen.** `setConsumerBatchEnabled(true)`, Batch-Grösse und
   -Timeout aus `BATCH_SIZE`/`BATCH_TIMEOUT_MS`, `JdbcTemplate.batchUpdate(...)`.
   Test: 1000 Testnachrichten senden, Queue danach leer, 1000 Zeilen in `message` (S3).
   Warum an dieser Stelle: das ist der eigentliche Zweck des Dienstes.

4. **Duplikate behandeln.** Insert-Statement auf `ON CONFLICT (id) DO NOTHING` umstellen.
   Test: dieselbe Nachricht zweimal einfügen → genau eine Zeile, nichts in der DLQ (S5).
   Warum an dieser Stelle: ohne diesen Schutz würde eine doppelt zugestellte Nachricht
   (z. B. nach einem Verbindungsabbruch) zu einem Fehler oder einer doppelten Zeile führen.

5. **Verhalten bei Datenbankausfall.** ACK erst nach erfolgreichem Commit. Spring AMQP
   liefert eine fehlgeschlagene Nachricht standardmässig sofort erneut aus (Re-Queue) —
   ohne Pause würde das die CPU in einer Endlosschleife belasten. Deshalb vor jedem
   erneuten Versuch eine feste Pause einlegen (z. B. `Thread.sleep(2000)`; eine
   konfigurierte `BackOffPolicy` ginge auch, für den Anfang reicht die einfache Variante).
   Test: Postgres während des Sendens stoppen und wieder starten — alle Nachrichten
   kommen trotzdem an, ohne manuellen Neustart des Dienstes (S7).
   Warum an dieser Stelle: braucht die Batch-Logik aus Schritt 3, um sinnvoll testbar zu sein.

6. **Mit zwei Instanzen testen.** `docker compose up -d --build --scale batch-writer=2`.
   Test: 1000 Nachrichten, beide Instanzen aktiv, keine doppelten Zeilen (S6).
   Warum an dieser Stelle: prüft, ob Schritt 4 auch unter echter Nebenläufigkeit hält,
   nicht nur im Einzel-Instanz-Test.

7. **Neustart-Szenario gezielt testen (S4).** `batch-writer` stoppen, 1000 Nachrichten
   senden, wieder starten.
   Test: alle 1000 da, höchstens 100 Transaktionen (ergibt sich aus Batch-Grösse 500).
   Warum als eigener Schritt: deckt den Rückstau-Fall ab, den die Schritte davor nicht
   automatisch mitprüfen.

8. **Aufräumen und CLAUDE.md-Konformität.** Kommentare ergänzen, auf Streams prüfen,
   `.env` nicht im Repo, `.env.example` und README-Tabelle "Stand" nachführen.
   Test: `mvn clean test` komplett grün in einem Lauf (S1); frischer Klon +
   `docker compose up -d --build` läuft ohne veröffentlichten Port (S2).
   Warum zuletzt: letzter Schritt vor dem Tag `bewertung-1`, damit die Abgabe exakt dem
   entspricht, was geprüft wird.
