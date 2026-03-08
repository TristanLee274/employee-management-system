package com.ems.employee.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ KafkaConfig — Event-Driven Architecture Configuration ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>KAFKA PRODUCER CONFIGURATION</h2>
 * <p>
 * This configuration explicitly wires the KafkaProducer with JSON serialization
 * so Domain Events (like EmployeeEvent) can be directly broadcast without
 * manual serialization.
 * </p>
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic employeeEventsTopic() {
        return TopicBuilder.name("employee.events")
                .partitions(3).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(
            KafkaProperties properties) {
        Map<String, Object> config = properties.buildProducerProperties(null);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }
}
