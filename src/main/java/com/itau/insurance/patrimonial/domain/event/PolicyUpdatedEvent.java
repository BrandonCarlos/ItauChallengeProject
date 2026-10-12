package com.itau.insurance.patrimonial.domain.event;

import com.itau.insurance.patrimonial.domain.model.Policy;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the update of an existing Policy.
 * This event is raised whenever a policy undergoes modifications.
 */
public record PolicyUpdatedEvent(
        String eventId,
        String policyId,
        String customerId,
        String coverage,
        Long premium,
        Long previousPremium,
        String status,
        LocalDateTime occurredAt
) implements DomainEvent, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public PolicyUpdatedEvent {
        eventId = eventId == null ? UUID.randomUUID().toString() : eventId;
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }

    /**
     * Factory method to create a PolicyUpdatedEvent from a Policy entity
     * 
     * @param policy the updated policy
     * @param previousPremium the premium value before update
     * @return a new PolicyUpdatedEvent
     */
    public static PolicyUpdatedEvent from(Policy policy, Long previousPremium) {
        return new PolicyUpdatedEvent(
                null,
                policy.getPolicyId(),
                policy.getCustomerId(),
                policy.getCoverage(),
                policy.getPremium(),
                previousPremium,
                policy.getStatus().toString(),
                null
        );
    }

    @Override
    public LocalDateTime timestamp() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return "PolicyUpdatedEvent";
    }
}
