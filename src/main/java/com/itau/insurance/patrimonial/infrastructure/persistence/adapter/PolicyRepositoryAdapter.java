package com.itau.insurance.patrimonial.infrastructure.persistence.adapter;

import com.itau.insurance.patrimonial.domain.model.Policy;
import com.itau.insurance.patrimonial.domain.ports.PolicyRepositoryPort;
import com.itau.insurance.patrimonial.infrastructure.persistence.document.PolicyDocument;
import com.itau.insurance.patrimonial.infrastructure.persistence.repository.PolicyMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PolicyRepositoryAdapter implements PolicyRepositoryPort {

    private final PolicyMongoRepository mongoRepository;

    @Override
    public Policy save(Policy policy) {
        return toDomain(mongoRepository.save(toDocument(policy)));
    }

    @Override
    public Policy update(Policy policy) {
        return toDomain(mongoRepository.save(toDocument(policy)));
    }

    @Override
    public Optional<Policy> findById(String policyId) {
        return mongoRepository.findByIdAndDeletedFalse(policyId).map(this::toDomain);
    }

    @Override
    public Optional<Policy> findByIdIncludingDeleted(String policyId) {
        return mongoRepository.findById(policyId).map(this::toDomain);
    }

    @Override
    public List<Policy> findByCustomerId(String customerId) {
        return mongoRepository.findByCustomerIdAndDeletedFalse(customerId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Policy> findByPolicyNumber(String policyNumber) {
        return mongoRepository.findByPolicyNumber(policyNumber).map(this::toDomain);
    }

    @Override
    public Optional<Policy> findByIdempotencyKey(String idempotencyKey) {
        return mongoRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public void deleteById(String policyId, String reason) {
        mongoRepository.findById(policyId).ifPresent(document -> {
            document.setDeleted(true);
            document.setActive(false);
            document.setUpdatedAt(LocalDateTime.now());
            Map<String, Object> metadata = document.getMetadata() != null ? new HashMap<>(document.getMetadata()) : new HashMap<>();
            metadata.put("deleteReason", reason);
            metadata.put("deletedAt", LocalDateTime.now().toString());
            document.setMetadata(metadata);
            mongoRepository.save(document);
        });
    }

    @Override
    public boolean existsById(String policyId) {
        return mongoRepository.existsByIdAndDeletedFalse(policyId);
    }

    @Override
    public List<Policy> findAll() {
        return mongoRepository.findByDeletedFalse().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Policy> findAll(int page, int size, String sortBy, boolean ascending) {
        return findAll(page, size, sortBy, ascending, false);
    }

    @Override
    public List<Policy> findAll(int page, int size, String sortBy, boolean ascending, boolean includeDeleted) {
        var sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        var pageable = PageRequest.of(page, size, sort);
        var pageResult = includeDeleted ? mongoRepository.findAll(pageable) : mongoRepository.findByDeletedFalse(pageable);
        return pageResult.stream().map(this::toDomain).toList();
    }

    @Override
    public long count() {
        return count(false);
    }

    @Override
    public long count(boolean includeDeleted) {
        return includeDeleted ? mongoRepository.count() : mongoRepository.countByDeletedFalse();
    }

    private PolicyDocument toDocument(Policy policy) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = policy.getStartDate() != null ? policy.getStartDate().atStartOfDay() : null;
        LocalDateTime endDateTime = policy.getEndDate() != null ? policy.getEndDate().atStartOfDay() : null;

        return PolicyDocument.builder()
                .id(policy.getPolicyId())
                .policyNumber(policy.getPolicyNumber())
                .customerId(policy.getCustomerId())
                .status(policy.getStatus() != null ? policy.getStatus().name() : null)
                .coverageType(policy.getCoverage())
                .monthlyPremium(policy.getPremium() != null ? BigDecimal.valueOf(policy.getPremium()) : null)
                .startDate(startDateTime)
                .endDate(endDateTime)
                .createdAt(policy.getCreatedAt() != null ? policy.getCreatedAt() : now)
                .updatedAt(policy.getUpdatedAt() != null ? policy.getUpdatedAt() : now)
                .propertyDescription(policy.getPropertyDescription())
                .propertyAddress(policy.getPropertyAddress())
                .idempotencyKey(policy.getIdempotencyKey())
                .active(policy.getStatus() == null || policy.getStatus() != Policy.PolicyStatus.CANCELLED)
                .deleted(false)
                .build();
    }

    private Policy toDomain(PolicyDocument doc) {
        return Policy.builder()
                .policyId(doc.getId())
                .policyNumber(doc.getPolicyNumber())
                .customerId(doc.getCustomerId())
                .coverage(doc.getCoverageType())
                .premium(doc.getMonthlyPremium() != null ? doc.getMonthlyPremium().longValue() : null)
                .startDate(doc.getStartDate() != null ? doc.getStartDate().toLocalDate() : null)
                .endDate(doc.getEndDate() != null ? doc.getEndDate().toLocalDate() : null)
                .status(doc.getStatus() != null ? Policy.PolicyStatus.valueOf(doc.getStatus()) : null)
                .propertyDescription(doc.getPropertyDescription())
                .propertyAddress(doc.getPropertyAddress())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .idempotencyKey(doc.getIdempotencyKey())
                .build();
    }
}
