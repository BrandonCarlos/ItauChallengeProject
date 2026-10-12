package com.itau.insurance.patrimonial.application.service;

import com.itau.insurance.patrimonial.application.mapper.PolicyMapper;
import com.itau.insurance.patrimonial.domain.exception.DuplicatePolicyException;
import com.itau.insurance.patrimonial.domain.exception.IdempotencyKeyConflictException;
import com.itau.insurance.patrimonial.domain.exception.InvalidPolicyException;
import com.itau.insurance.patrimonial.domain.exception.PolicyNotFoundException;
import com.itau.insurance.patrimonial.domain.model.Policy;
import com.itau.insurance.patrimonial.domain.ports.PolicyRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyApplicationServiceTest {

    @Mock
    private PolicyRepositoryPort policyRepository;
    @Mock
    private PolicyMapper policyMapper;
    @Mock
    private PolicyEventPublisher policyEventPublisher;
    @InjectMocks
    private PolicyApplicationService policyApplicationService;

    @Test
    void createPolicy_shouldPersistAndReturnPolicy() {
        Policy policy = validPolicy();
        Policy saved = validPolicy();
        saved.setPolicyId("p-1");
        when(policyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(policyRepository.findByPolicyNumber("POL-001")).thenReturn(Optional.empty());
        when(policyRepository.save(any(Policy.class))).thenReturn(saved);

        Policy result = policyApplicationService.createPolicy(policy, "idem-1");

        assertEquals("p-1", result.getPolicyId());
        verify(policyRepository).save(any(Policy.class));
        verify(policyEventPublisher).publishPolicyCreated(any());
    }

    @Test
    void createPolicy_shouldThrowOnIdempotencyConflict() {
        Policy policy = validPolicy();
        Policy existing = validPolicy();
        existing.setPolicyId("existing");
        when(policyRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        assertThrows(IdempotencyKeyConflictException.class,
                () -> policyApplicationService.createPolicy(policy, "idem-1"));
        verify(policyRepository, never()).save(any(Policy.class));
    }

    @Test
    void createPolicy_shouldThrowOnDuplicatePolicyNumber() {
        Policy policy = validPolicy();
        Policy existing = validPolicy();
        existing.setPolicyId("existing");
        when(policyRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
        when(policyRepository.findByPolicyNumber("POL-001")).thenReturn(Optional.of(existing));

        assertThrows(DuplicatePolicyException.class,
                () -> policyApplicationService.createPolicy(policy, "idem-2"));
        verify(policyRepository, never()).save(any(Policy.class));
    }

    @Test
    void updatePolicy_shouldUpdateMutableFields() {
        Policy existing = validPolicy();
        existing.setPolicyId("p-1");
        existing.setCoverage("RESIDENTIAL");
        existing.setPremium(1000L);
        when(policyRepository.findById("p-1")).thenReturn(Optional.of(existing));
        when(policyRepository.update(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Policy request = validPolicy();
        request.setCoverage("HOME");
        request.setPremium(1500L);
        request.setPropertyDescription("Updated");

        Policy result = policyApplicationService.updatePolicy("p-1", request, null);

        assertEquals("HOME", result.getCoverage());
        assertEquals(1500L, result.getPremium());
        assertEquals("Updated", result.getPropertyDescription());
        verify(policyRepository).update(any(Policy.class));
        verify(policyEventPublisher).publishPolicyUpdated(any());
    }

    @Test
    void updatePolicy_shouldRejectCancelledPolicy() {
        Policy cancelled = validPolicy();
        cancelled.setPolicyId("p-1");
        cancelled.setStatus(Policy.PolicyStatus.CANCELLED);
        when(policyRepository.findById("p-1")).thenReturn(Optional.of(cancelled));

        Policy request = validPolicy();
        request.setCoverage("HOME");

        assertThrows(InvalidPolicyException.class,
                () -> policyApplicationService.updatePolicy("p-1", request, null));
        verify(policyRepository, never()).update(any(Policy.class));
    }

    @Test
    void updatePolicy_shouldRequireJustificationForImmutableChange() {
        Policy existing = validPolicy();
        existing.setPolicyId("p-1");
        when(policyRepository.findById("p-1")).thenReturn(Optional.of(existing));

        Policy request = validPolicy();
        request.setPolicyNumber("POL-002");

        assertThrows(InvalidPolicyException.class,
                () -> policyApplicationService.updatePolicy("p-1", request, null));
        verify(policyRepository, never()).update(any(Policy.class));
    }

    @Test
    void updatePolicy_shouldAllowImmutableChangeWithJustification() {
        Policy existing = validPolicy();
        existing.setPolicyId("p-1");
        when(policyRepository.findById("p-1")).thenReturn(Optional.of(existing));
        when(policyRepository.findByPolicyNumber("POL-002")).thenReturn(Optional.empty());
        when(policyRepository.update(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Policy request = validPolicy();
        request.setPolicyNumber("POL-002");
        request.setCustomerId("cust-2");

        Policy result = policyApplicationService.updatePolicy("p-1", request, "ops correction");

        assertEquals("POL-002", result.getPolicyNumber());
        assertEquals("cust-2", result.getCustomerId());
    }

    @Test
    void updatePolicy_shouldRejectDuplicateImmutableChange() {
        Policy existing = validPolicy();
        existing.setPolicyId("p-1");
        Policy other = validPolicy();
        other.setPolicyId("p-2");
        when(policyRepository.findById("p-1")).thenReturn(Optional.of(existing));
        when(policyRepository.findByPolicyNumber("POL-002")).thenReturn(Optional.of(other));

        Policy request = validPolicy();
        request.setPolicyNumber("POL-002");

        assertThrows(DuplicatePolicyException.class,
                () -> policyApplicationService.updatePolicy("p-1", request, "ops correction"));
        verify(policyRepository, never()).update(any(Policy.class));
    }

    @Test
    void deletePolicy_shouldMarkDeletionAndPublishEvent() {
        Policy existing = validPolicy();
        existing.setPolicyId("p-1");
        when(policyRepository.findById("p-1")).thenReturn(Optional.of(existing));

        policyApplicationService.deletePolicy("p-1", "requested by customer");

        verify(policyRepository).deleteById("p-1", "requested by customer");
        verify(policyEventPublisher).publishPolicyDeleted(any());
    }

    @Test
    void cancelPolicy_shouldUpdateStatusAndPublishEvent() {
        Policy existing = validPolicy();
        existing.setPolicyId("p-1");
        when(policyRepository.findById("p-1")).thenReturn(Optional.of(existing));
        when(policyRepository.update(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        policyApplicationService.cancelPolicy("p-1");

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).update(captor.capture());
        assertEquals(Policy.PolicyStatus.CANCELLED, captor.getValue().getStatus());
        verify(policyEventPublisher).publishPolicyUpdated(any());
    }

    @Test
    void suspendPolicy_shouldUpdateStatusAndPublishEvent() {
        Policy existing = validPolicy();
        existing.setPolicyId("p-1");
        when(policyRepository.findById("p-1")).thenReturn(Optional.of(existing));
        when(policyRepository.update(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        policyApplicationService.suspendPolicy("p-1");

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).update(captor.capture());
        assertEquals(Policy.PolicyStatus.SUSPENDED, captor.getValue().getStatus());
        verify(policyEventPublisher).publishPolicyUpdated(any());
    }

    @Test
    void reactivatePolicy_shouldUpdateStatusAndPublishEvent() {
        Policy existing = validPolicy();
        existing.setPolicyId("p-1");
        existing.setStatus(Policy.PolicyStatus.SUSPENDED);
        when(policyRepository.findById("p-1")).thenReturn(Optional.of(existing));
        when(policyRepository.update(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        policyApplicationService.reactivatePolicy("p-1");

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).update(captor.capture());
        assertEquals(Policy.PolicyStatus.ACTIVE, captor.getValue().getStatus());
        verify(policyEventPublisher).publishPolicyUpdated(any());
    }

    @Test
    void updatePremium_shouldUpdateAndPublishEvent() {
        Policy existing = validPolicy();
        existing.setPolicyId("p-1");
        existing.setPremium(1000L);
        when(policyRepository.findById("p-1")).thenReturn(Optional.of(existing));
        when(policyRepository.update(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Policy result = policyApplicationService.updatePremium("p-1", 2500L);

        assertEquals(2500L, result.getPremium());
        verify(policyRepository).update(any(Policy.class));
        verify(policyEventPublisher).publishPolicyUpdated(any());
    }

    @Test
    void getPolicyById_shouldDelegateToRepository() {
        Policy existing = validPolicy();
        existing.setPolicyId("p-1");
        when(policyRepository.findById("p-1")).thenReturn(Optional.of(existing));

        Optional<Policy> result = policyApplicationService.getPolicyById("p-1");

        assertTrue(result.isPresent());
        assertEquals("p-1", result.get().getPolicyId());
    }

    @Test
    void getPoliciesPage_shouldUseRepositoryPaging() {
        when(policyRepository.findAll(0, 10, "createdAt", false, false)).thenReturn(List.of(validPolicy()));

        List<Policy> result = policyApplicationService.getPoliciesPage(0, 10, "createdAt", "desc");

        assertEquals(1, result.size());
        verify(policyRepository).findAll(0, 10, "createdAt", false, false);
    }

    @Test
    void getTotalPolicies_shouldDelegateToRepository() {
        when(policyRepository.count(false)).thenReturn(2L);

        assertEquals(2L, policyApplicationService.getTotalPolicies());
    }

    private Policy validPolicy() {
        return Policy.builder()
                .policyNumber("POL-001")
                .customerId("cust-1")
                .coverage("RESIDENTIAL")
                .premium(1000L)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .status(Policy.PolicyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
