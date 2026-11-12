package ch.pritz.chatservice;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurationsklasse fuer den Fanout-Exchange und instanzspezifische Queues der Live-Zustellung.
 */
@Configuration
public class FanoutRabbitConfig {

    public static final String EXCHANGE_NAME = "chat.delivery";

    /**
     * Definiert den Fanout-Exchange fuer die Live-Verteilung von Nachrichten.
     *
     * @return FanoutExchange Instanz
     */
    @Bean
    public FanoutExchange chatDeliveryExchange() {
        return new FanoutExchange(EXCHANGE_NAME, true, false);
    }

    /**
     * Erstellt eine temporäre, exklusive Queue für die aktuelle Instanz
     * mit einer maximalen Länge von 1000 Nachrichten (Szenario S6 - Schutz vor überlaufenden Queues).
     *
     * @return Queue Instanz
     */
    @Bean
    public Queue instanceDeliveryQueue() {
        return QueueBuilder.nonDurable()
                .exclusive()
                .autoDelete()
                .withArgument("x-max-length", 1000)
                .withArgument("x-overflow", "drop-head")
                .build();
    }

    /**
     * Bindet die Instanz-Queue an den Fanout-Exchange an.
     *
     * @param instanceDeliveryQueue Die instanzspezifische Queue
     * @param chatDeliveryExchange Der Fanout-Exchange
     * @return Binding Instanz
     */
    @Bean
    public Binding bindingInstanceQueue(Queue instanceDeliveryQueue, FanoutExchange chatDeliveryExchange) {
        return BindingBuilder.bind(instanceDeliveryQueue).to(chatDeliveryExchange);
    }
}