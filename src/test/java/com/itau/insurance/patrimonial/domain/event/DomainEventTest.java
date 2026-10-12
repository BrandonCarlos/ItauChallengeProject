package com.itau.insurance.patrimonial.domain.event;

import com.itau.insurance.patrimonial.domain.model.Policy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DomainEventTest {

    @Test
    void policyCreatedEvent_fromShouldPopulateFields() {
        Policy policy = validPolicy();
        PolicyCreatedEvent event = PolicyCreatedEvent.from(policy);

        assertNotNull(event.eventId());
        assertEquals(policy.getPolicyId(), event.policyId());
        assertEquals("PolicyCreatedEvent", event.eventType());
    }

    @Test
    void policyUpdatedEvent_fromShouldPopulateFields() {
        Policy policy = validPolicy();
        PolicyUpdatedEvent event = PolicyUpdatedEvent.from(policy, 100L);

        assertNotNull(event.eventId());
        assertEquals(policy.getPolicyId(), event.policyId());
        assertEquals(Long.valueOf(100L), event.previousPremium());
        assertEquals("PolicyUpdatedEvent", event.eventType());
    }

    @Test
    void policyDeletedEvent_fromShouldUseFallbackReason() {
        Policy policy = validPolicy();
        PolicyDeletedEvent event = PolicyDeletedEvent.from(policy, null);

        assertNotNull(event.eventId());
        assertEquals("No reason provided", event.reason());
        assertEquals("PolicyDeletedEvent", event.eventType());
    }

    private Policy validPolicy() {
        return Policy.builder()
                .policyId("p-1")
                .policyNumber("POL-1")
                .customerId("cust-1")
                .coverage("HOME")
                .premium(1000L)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .status(Policy.PolicyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
