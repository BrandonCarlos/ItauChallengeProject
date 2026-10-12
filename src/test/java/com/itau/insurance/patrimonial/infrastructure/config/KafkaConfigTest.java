package com.itau.insurance.patrimonial.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaConfigTest {

    @Test
    void kafkaBeans_shouldExposeExpectedConfiguration() {
        KafkaConfig config = new KafkaConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(config, "producerAcks", "all");
        ReflectionTestUtils.setField(config, "producerRetries", 3);
        ReflectionTestUtils.setField(config, "producerBatchSize", 16384);
        ReflectionTestUtils.setField(config, "producerLingerMs", 10);
        ReflectionTestUtils.setField(config, "compressionType", "snappy");
        ReflectionTestUtils.setField(config, "consumerGroupId", "test-group");
        ReflectionTestUtils.setField(config, "autoOffsetReset", "earliest");
        ReflectionTestUtils.setField(config, "maxPollRecords", 500);
        ReflectionTestUtils.setField(config, "sessionTimeoutMs", 30000);
        ReflectionTestUtils.setField(config, "heartbeatIntervalMs", 10000);
        ReflectionTestUtils.setField(config, "securityProtocol", "PLAINTEXT");
        ReflectionTestUtils.setField(config, "saslMechanism", "");
        ReflectionTestUtils.setField(config, "saslUsername", "");
        ReflectionTestUtils.setField(config, "saslPassword", "");

        ProducerFactory<String, String> producerFactory = config.producerFactory();
        ConsumerFactory<String, String> consumerFactory = config.consumerFactory();
        KafkaTemplate<String, String> kafkaTemplate = config.kafkaTemplate(producerFactory);
        ConcurrentKafkaListenerContainerFactory<String, String> listenerFactory =
                config.kafkaListenerContainerFactory(consumerFactory);

        Map<String, Object> producerProps = producerFactory.getConfigurationProperties();
        Map<String, Object> consumerProps = consumerFactory.getConfigurationProperties();

        assertNotNull(kafkaTemplate);
        assertNotNull(listenerFactory);
        assertEquals("localhost:9092", producerProps.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(Boolean.TRUE, producerProps.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
        assertEquals("test-group", consumerProps.get(ConsumerConfig.GROUP_ID_CONFIG));
        assertEquals(ContainerProperties.AckMode.MANUAL_IMMEDIATE, listenerFactory.getContainerProperties().getAckMode());
    }
}
