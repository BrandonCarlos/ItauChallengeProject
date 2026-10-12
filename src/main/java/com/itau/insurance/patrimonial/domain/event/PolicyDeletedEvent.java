package com.itau.insurance.patrimonial.domain.event;

import com.itau.insurance.patrimonial.domain.model.Policy;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the deletion (cancellation) of a Policy.
 * This event is raised when a policy is removed from the system.
 */
public record PolicyDeletedEvent(
        String eventId,
        String policyId,
        String customerId,
        String reason,
        LocalDateTime occurredAt
) implements DomainEvent, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public PolicyDeletedEvent {
        eventId = eventId == null ? UUID.randomUUID().toString() : eventId;
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
        reason = reason == null ? "No reason provided" : reason;
    }

    /**
     * Factory method to create a PolicyDeletedEvent from a Policy entity
     * 
     * @param policy the policy that was deleted
     * @param reason the reason for deletion
     * @return a new PolicyDeletedEvent
     */
    public static PolicyDeletedEvent from(Policy policy, String reason) {
        return new PolicyDeletedEvent(
                null,
                policy.getPolicyId(),
                policy.getCustomerId(),
                reason,
                null
        );
    }

    @Override
    public LocalDateTime timestamp() {
        return occurredAt;
    }

    @Override
    public String eventType() {
        return "PolicyDeletedEvent";
    }
}
