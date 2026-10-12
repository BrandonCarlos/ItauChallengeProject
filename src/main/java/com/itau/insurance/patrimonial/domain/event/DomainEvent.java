package com.itau.insurance.patrimonial.domain.event;

import java.time.LocalDateTime;

/**
 * Marker interface for all domain events.
 * Domain events represent important business occurrences within the domain.
 */
public sealed interface DomainEvent permits PolicyCreatedEvent, PolicyUpdatedEvent, PolicyDeletedEvent {

    /**
     * Gets the timestamp when this event occurred
     * 
     * @return the event timestamp
     */
    LocalDateTime timestamp();

    /**
     * Gets the type/name of this event
     * 
     * @return the event type
     */
    String eventType();

    default LocalDateTime getTimestamp() {
        return timestamp();
    }

    default String getEventType() {
        return eventType();
    }
}
