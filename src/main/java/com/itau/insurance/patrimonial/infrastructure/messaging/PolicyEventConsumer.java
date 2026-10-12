package com.itau.insurance.patrimonial.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyEventConsumer {

    private final ObjectMapper objectMapper;
    private final ExecutorService eventExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @KafkaListener(
            topics = "${app.kafka.topic.policy-events:policy-events}",
            groupId = "${app.kafka.consumer.group-id:policy-consumer-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Timed(value = "insurance.kafka.consumer", extraTags = {"operation", "consumePolicyEvent"})
    public void consumePolicyEvent(
            @Payload String message,
            @Header(name = "event-type", required = false) String eventType,
            @Header(name = "message-id", required = false) String messageId,
            @Header(name = "correlation-id", required = false) String correlationId,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(name = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(name = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        try {
            log.info("Consuming policy event: eventType={}, messageId={}, correlationId={}, topic={}, partition={}, offset={}", 
                    eventType, messageId, correlationId, topic, partition, offset);

            if (message == null || message.isEmpty()) {
                log.warn("Received empty message: messageId={}", messageId);
                return;
            }

            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);

            eventExecutor.submit(() -> processPolicyEvent(eventType, eventData, correlationId, messageId));

            log.debug("Policy event processed successfully: eventType={}, messageId={}", eventType, messageId);
        } catch (Exception e) {
            log.error("Error consuming policy event: eventType={}, messageId={}, error={}", 
                    eventType, messageId, e.getMessage(), e);
            handleEventProcessingError(eventType, messageId, e);
        }
    }

    private void handlePolicyCreated(Map<String, Object> eventData, String correlationId) {
        log.info("Handling POLICY_CREATED event: correlationId={}", correlationId);
        String policyNumber = (String) eventData.get("policyNumber");
        String customerId = (String) eventData.get("customerId");
        log.debug("Policy created: policyNumber={}, customerId={}", policyNumber, customerId);
    }

    private void handlePolicyUpdated(Map<String, Object> eventData, String correlationId) {
        log.info("Handling POLICY_UPDATED event: correlationId={}", correlationId);
        String policyNumber = (String) eventData.get("policyNumber");
        log.debug("Policy updated: policyNumber={}", policyNumber);
    }

    private void handlePolicyActivated(Map<String, Object> eventData, String correlationId) {
        log.info("Handling POLICY_ACTIVATED event: correlationId={}", correlationId);
        String policyNumber = (String) eventData.get("policyNumber");
        String customerId = (String) eventData.get("customerId");
        log.debug("Policy activated: policyNumber={}, customerId={}", policyNumber, customerId);
    }

    private void handlePolicyCancelled(Map<String, Object> eventData, String correlationId) {
        log.info("Handling POLICY_CANCELLED event: correlationId={}", correlationId);
        String policyNumber = (String) eventData.get("policyNumber");
        String reason = (String) eventData.get("reason");
        log.debug("Policy cancelled: policyNumber={}, reason={}", policyNumber, reason);
    }

    private void handlePolicyRenewed(Map<String, Object> eventData, String correlationId) {
        log.info("Handling POLICY_RENEWED event: correlationId={}", correlationId);
        String policyNumber = (String) eventData.get("policyNumber");
        log.debug("Policy renewed: policyNumber={}", policyNumber);
    }

    private void handleEventProcessingError(String eventType, String messageId, Exception e) {
        log.error("Failed to process event: eventType={}, messageId={}", eventType, messageId);
    }

    private void processPolicyEvent(String eventType, Map<String, Object> eventData, String correlationId, String messageId) {
        try {
            switch (eventType) {
                case "POLICY_CREATED" -> handlePolicyCreated(eventData, correlationId);
                case "POLICY_UPDATED" -> handlePolicyUpdated(eventData, correlationId);
                case "POLICY_ACTIVATED" -> handlePolicyActivated(eventData, correlationId);
                case "POLICY_CANCELLED" -> handlePolicyCancelled(eventData, correlationId);
                case "POLICY_RENEWED" -> handlePolicyRenewed(eventData, correlationId);
                default -> log.warn("Unknown event type received: {}", eventType);
            }
        } catch (Exception e) {
            handleEventProcessingError(eventType, messageId, e);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topic.domain-events:domain-events}",
            groupId = "${app.kafka.consumer.group-id:policy-consumer-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Timed(value = "insurance.kafka.consumer", extraTags = {"operation", "consumeDomainEvent"})
    public void consumeDomainEvent(
            @Payload String message,
            @Header(name = "event-type", required = false) String eventType,
            @Header(name = "message-id", required = false) String messageId,
            @Header(name = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic
    ) {
        try {
            log.debug("Consuming domain event: eventType={}, messageId={}, topic={}", 
                    eventType, messageId, topic);

            if (message == null || message.isEmpty()) {
                log.warn("Received empty domain event message: messageId={}", messageId);
                return;
            }

            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            eventExecutor.submit(() -> log.info(
                    "Domain event received and can be processed: eventType={}, messageId={}, payloadKeys={}",
                    eventType,
                    messageId,
                    eventData.keySet()
            ));
        } catch (Exception e) {
            log.error("Error consuming domain event: eventType={}, messageId={}, error={}", 
                    eventType, messageId, e.getMessage(), e);
        }
    }
}
