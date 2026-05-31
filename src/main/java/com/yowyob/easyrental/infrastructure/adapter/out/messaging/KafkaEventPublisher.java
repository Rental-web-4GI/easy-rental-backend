package com.yowyob.easyrental.infrastructure.adapter.out.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka adapter for publishing domain events.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes an event to a Kafka topic.
     *
     * @param topic target topic
     * @param key message key
     * @param payload JSON payload
     */
    public void publish(String topic, String key, String payload) {
        kafkaTemplate.send(topic, key, payload);
    }
}
