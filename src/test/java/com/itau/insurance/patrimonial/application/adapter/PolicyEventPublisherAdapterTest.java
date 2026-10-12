package com.itau.insurance.patrimonial.application.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itau.insurance.patrimonial.domain.event.PolicyCreatedEvent;
import com.itau.insurance.patrimonial.domain.event.PolicyDeletedEvent;
import com.itau.insurance.patrimonial.domain.event.PolicyUpdatedEvent;
import com.itau.insurance.patrimonial.domain.model.Policy;
import com.itau.insurance.patrimonial.infrastructure.messaging.OutboxPublisher;
import com.itau.insurance.patrimonial.infrastructure.persistence.document.OutboxEventDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PolicyEventPublisherAdapterTest {

    @Mock
    private OutboxPublisher outboxPublisher;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PolicyEventPublisherAdapter adapter;

    @Test
    void publishPolicyCreated_shouldCreateOutboxEvent() {
        doReturn(java.util.Map.of("policyId", "p-1")).when(objectMapper).convertValue(any(), eq(java.util.Map.class));

        adapter.publishPolicyCreated(PolicyCreatedEvent.from(validPolicy()));

        ArgumentCaptor<OutboxEventDocument> captor = ArgumentCaptor.forClass(OutboxEventDocument.class);
        verify(outboxPublisher).saveOutboxEvent(captor.capture());
        assertEquals("POLICY_CREATED", captor.getValue().getEventType());
        assertEquals("p-1", captor.getValue().getAggregateId());
        assertNotNull(captor.getValue().getId());
    }

    @Test
    void publishPolicyUpdated_shouldCreateOutboxEvent() {
        doReturn(java.util.Map.of("policyId", "p-1")).when(objectMapper).convertValue(any(), eq(java.util.Map.class));

        adapter.publishPolicyUpdated(PolicyUpdatedEvent.from(validPolicy(), 100L));

        ArgumentCaptor<OutboxEventDocument> captor = ArgumentCaptor.forClass(OutboxEventDocument.class);
        verify(outboxPublisher).saveOutboxEvent(captor.capture());
        assertEquals("POLICY_UPDATED", captor.getValue().getEventType());
    }

    @Test
    void publishPolicyDeleted_shouldCreateOutboxEvent() {
        doReturn(java.util.Map.of("policyId", "p-1")).when(objectMapper).convertValue(any(), eq(java.util.Map.class));

        adapter.publishPolicyDeleted(PolicyDeletedEvent.from(validPolicy(), "reason"));

        ArgumentCaptor<OutboxEventDocument> captor = ArgumentCaptor.forClass(OutboxEventDocument.class);
        verify(outboxPublisher).saveOutboxEvent(captor.capture());
        assertEquals("POLICY_DELETED", captor.getValue().getEventType());
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
