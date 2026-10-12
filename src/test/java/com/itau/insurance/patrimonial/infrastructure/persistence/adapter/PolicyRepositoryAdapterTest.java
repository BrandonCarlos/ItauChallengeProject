package com.itau.insurance.patrimonial.infrastructure.persistence.adapter;

import com.itau.insurance.patrimonial.domain.model.Policy;
import com.itau.insurance.patrimonial.infrastructure.persistence.document.PolicyDocument;
import com.itau.insurance.patrimonial.infrastructure.persistence.repository.PolicyMongoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyRepositoryAdapterTest {

    @Mock
    private PolicyMongoRepository mongoRepository;

    @InjectMocks
    private PolicyRepositoryAdapter adapter;

    @Test
    void save_shouldMapAndPersist() {
        Policy policy = validPolicy();
        when(mongoRepository.save(any(PolicyDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Policy result = adapter.save(policy);

        assertEquals(policy.getPolicyNumber(), result.getPolicyNumber());
        verify(mongoRepository).save(any(PolicyDocument.class));
    }

    @Test
    void findById_shouldIgnoreDeleted() {
        when(mongoRepository.findByIdAndDeletedFalse("p-1")).thenReturn(Optional.of(document()));

        Optional<Policy> result = adapter.findById("p-1");

        assertTrue(result.isPresent());
        assertEquals("p-1", result.get().getPolicyId());
    }

    @Test
    void findByIdIncludingDeleted_shouldReturnDeletedRecords() {
        when(mongoRepository.findById("p-1")).thenReturn(Optional.of(document()));

        Optional<Policy> result = adapter.findByIdIncludingDeleted("p-1");

        assertTrue(result.isPresent());
    }

    @Test
    void deleteById_shouldMarkSoftDeleteAndStoreReason() {
        PolicyDocument doc = document();
        when(mongoRepository.findById("p-1")).thenReturn(Optional.of(doc));
        when(mongoRepository.save(any(PolicyDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adapter.deleteById("p-1", "customer requested");

        ArgumentCaptor<PolicyDocument> captor = ArgumentCaptor.forClass(PolicyDocument.class);
        verify(mongoRepository).save(captor.capture());
        assertTrue(captor.getValue().getDeleted());
        assertEquals("customer requested", captor.getValue().getMetadata().get("deleteReason"));
    }

    @Test
    void findAllPage_shouldFilterDeletedByDefault() {
        Page<PolicyDocument> page = new PageImpl<>(List.of(document()));
        when(mongoRepository.findByDeletedFalse(PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending())))
                .thenReturn(page);

        List<Policy> result = adapter.findAll(0, 10, "createdAt", false, false);

        assertEquals(1, result.size());
    }

    @Test
    void count_shouldUseDeletedAwareQuery() {
        when(mongoRepository.countByDeletedFalse()).thenReturn(3L);

        assertEquals(3L, adapter.count(false));
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

    private PolicyDocument document() {
        return PolicyDocument.builder()
                .id("p-1")
                .policyNumber("POL-1")
                .customerId("cust-1")
                .coverageType("HOME")
                .monthlyPremium(java.math.BigDecimal.valueOf(1000))
                .status("ACTIVE")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(false)
                .build();
    }
}
