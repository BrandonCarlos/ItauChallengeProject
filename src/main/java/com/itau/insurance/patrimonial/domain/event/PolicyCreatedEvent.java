package com.itau.insurance.patrimonial.domain.event;

import com.itau.insurance.patrimonial.domain.model.Policy;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the creation of a new Policy.
 * Domain events are used to communicate important business occurrences across the application.
 */
public record PolicyCreatedEvent(
        String eventId,
        String policyId,
        String customerId,
        String coverage,
        Long premium,
        LocalDateTime occurredAt
) implements DomainEvent, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public PolicyCreatedEvent {
        eventId = eventId == null ? UUID.randomUUID().toString() : eventId;
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }

    /**
     * Factory method to create a PolicyCreatedEvent from a Policy entity
     * 
     * @param policy the policy that was created
     * @return a new PolicyCreatedEvent
     */
    public static PolicyCreatedEvent from(Policy policy) {
        return new PolicyCreatedEvent(
                null,
                policy.getPolicyId(),
                policy.getCustomerId(),
                policy.getCoverage(),
                policy.getPremium(),
                null
        );
    }

    @Override
    public LocalDateTime timestamp() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return "PolicyCreatedEvent";
    }
}
