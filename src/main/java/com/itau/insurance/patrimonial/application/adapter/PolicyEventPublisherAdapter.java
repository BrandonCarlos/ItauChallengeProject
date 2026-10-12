package com.itau.insurance.patrimonial.application.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itau.insurance.patrimonial.application.service.PolicyEventPublisher;
import com.itau.insurance.patrimonial.domain.event.PolicyCreatedEvent;
import com.itau.insurance.patrimonial.domain.event.PolicyDeletedEvent;
import com.itau.insurance.patrimonial.domain.event.PolicyUpdatedEvent;
import com.itau.insurance.patrimonial.infrastructure.messaging.OutboxPublisher;
import com.itau.insurance.patrimonial.infrastructure.persistence.document.OutboxEventDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Simple in-process implementation of PolicyEventPublisher.
 * This adapter currently logs domain events. In a production system, this could be
 * extended to publish events to a message broker (RabbitMQ, Kafka, etc.) or an event bus.
 * 
 * This follows the Adapter pattern from Hexagonal Architecture, allowing the domain
 * to publish events while remaining independent of specific event infrastructure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyEventPublisherAdapter implements PolicyEventPublisher {

    private static final String AGGREGATE_TYPE = "POLICY";
    private static final String SOURCE_SYSTEM = "patrimonial-api";

    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Publishes a PolicyCreatedEvent
     * 
     * @param event the event to publish
     */
    @Override
    public void publishPolicyCreated(PolicyCreatedEvent event) {
        log.info("Publishing PolicyCreatedEvent - Policy ID: {}, Customer ID: {}, Event ID: {}",
                event.policyId(), event.customerId(), event.eventId());
        log.debug("Event details: {}", event);
        outboxPublisher.saveOutboxEvent(toOutboxEvent(event.policyId(), "POLICY_CREATED", event.eventId(), event));
    }

    /**
     * Publishes a PolicyUpdatedEvent
     * 
     * @param event the event to publish
     */
    @Override
    public void publishPolicyUpdated(PolicyUpdatedEvent event) {
        log.info("Publishing PolicyUpdatedEvent - Policy ID: {}, Customer ID: {}, Event ID: {}",
                event.policyId(), event.customerId(), event.eventId());
        log.debug("Event details: {}", event);
        outboxPublisher.saveOutboxEvent(toOutboxEvent(event.policyId(), "POLICY_UPDATED", event.eventId(), event));
    }

    /**
     * Publishes a PolicyDeletedEvent
     * 
     * @param event the event to publish
     */
    @Override
    public void publishPolicyDeleted(PolicyDeletedEvent event) {
        log.info("Publishing PolicyDeletedEvent - Policy ID: {}, Customer ID: {}, Event ID: {}",
                event.policyId(), event.customerId(), event.eventId());
        log.debug("Event details: {}", event);
        outboxPublisher.saveOutboxEvent(toOutboxEvent(event.policyId(), "POLICY_DELETED", event.eventId(), event));
    }

    private OutboxEventDocument toOutboxEvent(
            String aggregateId,
            String eventType,
            String causationId,
            Object eventPayload) {

        return OutboxEventDocument.builder()
                .id(UUID.randomUUID().toString())
                .aggregateId(aggregateId)
                .aggregateType(AGGREGATE_TYPE)
                .eventType(eventType)
                .eventData(objectMapper.convertValue(eventPayload, Map.class))
                .partitionKey(aggregateId)
                .correlationId(UUID.randomUUID().toString())
                .causationId(causationId)
                .sourceSystem(SOURCE_SYSTEM)
                .build();
    }
}
