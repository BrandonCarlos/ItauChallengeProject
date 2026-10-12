package com.itau.insurance.patrimonial.application.service;

import com.itau.insurance.patrimonial.domain.event.PolicyCreatedEvent;
import com.itau.insurance.patrimonial.domain.event.PolicyDeletedEvent;
import com.itau.insurance.patrimonial.domain.event.PolicyUpdatedEvent;

/**
 * Port for publishing domain events to event subscribers.
 * This port allows the application service to publish domain events while remaining
 * independent of the specific event publishing infrastructure.
 */
public interface PolicyEventPublisher {

    /**
     * Publishes a PolicyCreatedEvent
     * 
     * @param event the event to publish
     */
    void publishPolicyCreated(PolicyCreatedEvent event);

    /**
     * Publishes a PolicyUpdatedEvent
     * 
     * @param event the event to publish
     */
    void publishPolicyUpdated(PolicyUpdatedEvent event);

    /**
     * Publishes a PolicyDeletedEvent
     * 
     * @param event the event to publish
     */
    void publishPolicyDeleted(PolicyDeletedEvent event);
}
