package ch.pritz.batchwriter;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurationsklasse fuer RabbitMQ Buendelung und Nachrichtenumwandlung.
 */
@Configuration
public class RabbitConfig {

    @Value("${batch.size:500}")
    private int batchSize;

    @Value("${batch.timeout-ms:200}")
    private long batchTimeoutMs;

    /**
     * Erstellt das Queue-Objekt fuer chat.persist.
     *
     * @return Queue
     */
    @Bean
    public Queue chatPersistQueue() {
        return new Queue("chat.persist", true);
    }

    /**
     * Konfiguriert den JSON Converter fuer eintreffende Nachrichten.
     *
     * @return Jackson2JsonMessageConverter
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Konfiguriert die Container Factory fuer den Listener mit aktivierter Batch-Funktion.
     *
     * @param connectionFactory RabbitMQ Verbindungsfabrik
     * @return SimpleRabbitListenerContainerFactory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());

        // Aktiviert das Buendeln von Nachrichten (Batch-Konsum)
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(batchSize);
        factory.setReceiveTimeout(batchTimeoutMs);

        return factory;
    }
}