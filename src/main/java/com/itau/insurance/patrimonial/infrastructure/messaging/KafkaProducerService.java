package com.itau.insurance.patrimonial.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService eventExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${app.kafka.topic.policy-events:policy-events}")
    private String policyEventsTopic;

    @Value("${app.kafka.topic.domain-events:domain-events}")
    private String domainEventsTopic;

    @Value("${app.kafka.enable-compression:true}")
    private Boolean enableCompression;

    @Timed(value = "insurance.kafka.producer", extraTags = {"operation", "publishPolicyEvent"})
    public void publishPolicyEvent(String eventType, Object eventData) {
        publishPolicyEvent(eventType, eventData, null);
    }

    @Timed(value = "insurance.kafka.producer", extraTags = {"operation", "publishPolicyEventWithCorrelation"})
    public void publishPolicyEvent(String eventType, Object eventData, String correlationId) {
        try {
            String messageId = UUID.randomUUID().toString();
            String payload = objectMapper.writeValueAsString(eventData);

            log.debug("Publishing policy event: type={}, messageId={}, correlationId={}", 
                    eventType, messageId, correlationId);

            Message<String> message = MessageBuilder.withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, policyEventsTopic)
                    .setHeader("event-type", eventType)
                    .setHeader("message-id", messageId)
                    .setHeader("correlation-id", correlationId != null ? correlationId : UUID.randomUUID().toString())
                    .setHeader("event-timestamp", System.currentTimeMillis())
                    .setHeader("source", "insurance-platform")
                    .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Policy event published successfully: topic={}, partition={}, offset={}, messageId={}", 
                            policyEventsTopic, 
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            messageId);
                } else {
                    log.error("Failed to publish policy event: messageId={}", messageId, ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing policy event: eventType={}", eventType, e);
            throw new RuntimeException("Failed to publish policy event", e);
        }
    }

    @Timed(value = "insurance.kafka.producer", extraTags = {"operation", "publishPolicyEventAsync"})
    public CompletableFuture<Void> publishPolicyEventAsync(String eventType, Object eventData) {
        return publishPolicyEventAsync(eventType, eventData, null);
    }

    @Timed(value = "insurance.kafka.producer", extraTags = {"operation", "publishPolicyEventAsyncWithCorrelation"})
    public CompletableFuture<Void> publishPolicyEventAsync(String eventType, Object eventData, String correlationId) {
        return CompletableFuture.runAsync(
                () -> publishPolicyEvent(eventType, eventData, correlationId),
                eventExecutor
        );
    }

    @Timed(value = "insurance.kafka.producer", extraTags = {"operation", "publishDomainEvent"})
    public void publishDomainEvent(String eventType, Object eventData, String partitionKey) {
        try {
            String messageId = UUID.randomUUID().toString();
            String payload = objectMapper.writeValueAsString(eventData);

            log.debug("Publishing domain event: type={}, messageId={}", eventType, messageId);

            Message<String> message = MessageBuilder.withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, domainEventsTopic)
                    .setHeader("kafka_messageKey", partitionKey)
                    .setHeader("event-type", eventType)
                    .setHeader("message-id", messageId)
                    .setHeader("event-timestamp", System.currentTimeMillis())
                    .setHeader("source", "insurance-platform")
                    .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Domain event published successfully: topic={}, partition={}, offset={}, messageId={}", 
                            domainEventsTopic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            messageId);
                } else {
                    log.error("Failed to publish domain event: messageId={}", messageId, ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing domain event: eventType={}", eventType, e);
            throw new RuntimeException("Failed to publish domain event", e);
        }
    }

    @Timed(value = "insurance.kafka.producer", extraTags = {"operation", "sendMessage"})
    public void sendMessage(String topic, String key, String payload) {
        try {
            log.debug("Sending message to topic: {} with key: {}", topic, key);
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Message sent successfully to topic: {}, partition: {}, offset: {}", 
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send message to topic: {}", topic, ex);
                }
            });
        } catch (Exception e) {
            log.error("Error sending message to topic: {}", topic, e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }
}
