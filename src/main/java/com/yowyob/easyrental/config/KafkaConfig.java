package com.yowyob.easyrental.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka topic configuration for domain events.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
@Configuration
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic rentalEventsTopic() {
        return new NewTopic("rental.events", 3, (short) 1);
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return new NewTopic("notification.events", 3, (short) 1);
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return new NewTopic("audit.events", 3, (short) 1);
    }
}
