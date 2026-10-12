package com.itau.insurance.patrimonial.application.service;

import com.itau.insurance.patrimonial.application.dto.PolicyResponseDTO;
import com.itau.insurance.patrimonial.application.mapper.PolicyMapper;
import com.itau.insurance.patrimonial.domain.exception.DuplicatePolicyException;
import com.itau.insurance.patrimonial.domain.event.PolicyCreatedEvent;
import com.itau.insurance.patrimonial.domain.event.PolicyDeletedEvent;
import com.itau.insurance.patrimonial.domain.event.PolicyUpdatedEvent;
import com.itau.insurance.patrimonial.domain.exception.IdempotencyKeyConflictException;
import com.itau.insurance.patrimonial.domain.exception.InvalidPolicyException;
import com.itau.insurance.patrimonial.domain.exception.PolicyNotFoundException;
import com.itau.insurance.patrimonial.domain.model.Policy;
import com.itau.insurance.patrimonial.domain.ports.PolicyRepositoryPort;
import com.itau.insurance.patrimonial.domain.ports.PolicyUseCasePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.annotation.Timed;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application service that orchestrates policy-related use cases.
 * This service acts as a facade between the REST controllers and the domain layer,
 * implementing the PolicyUseCasePort interface and using the repository port for persistence.
 * 
 * The service handles:
 * - Use case orchestration
 * - Transaction management
 * - Domain event publishing (conceptual - events are created but need publishing infrastructure)
 * - DTO-to-Domain and Domain-to-DTO transformations via the mapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PolicyApplicationService implements PolicyUseCasePort {

    private final PolicyRepositoryPort policyRepository;
    private final PolicyMapper policyMapper;
    private final PolicyEventPublisher policyEventPublisher;

    @Override
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "create"})
    public Policy createPolicy(Policy policy, String idempotencyKey) {
        log.info("Creating new policy for customer: {}", policy.getCustomerId());

        // Check for idempotency key conflict
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Policy> existingPolicy = policyRepository.findByIdempotencyKey(idempotencyKey);
            if (existingPolicy.isPresent()) {
                log.warn("Idempotency key conflict detected: {}", idempotencyKey);
                throw new IdempotencyKeyConflictException(idempotencyKey, existingPolicy.get().getPolicyId());
            }
            policy.setIdempotencyKey(idempotencyKey);
        }

        // Validate policy before saving
        policy.validate();
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());

        if (policyRepository.findByPolicyNumber(policy.getPolicyNumber()).isPresent()) {
            throw new DuplicatePolicyException(policy.getPolicyNumber());
        }

        // Save policy
        Policy savedPolicy;
        try {
            savedPolicy = policyRepository.save(policy);
        } catch (DuplicateKeyException ex) {
            throw new DuplicatePolicyException(policy.getPolicyNumber());
        }
        log.info("Policy created successfully with ID: {}", savedPolicy.getPolicyId());

        // Publish event
        PolicyCreatedEvent event = PolicyCreatedEvent.from(savedPolicy);
        policyEventPublisher.publishPolicyCreated(event);

        return savedPolicy;
    }

    @Override
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "update"})
    public Policy updatePolicy(String policyId, Policy updatedPolicy, String changeJustification) {
        log.info("Updating policy: {}", policyId);

        Policy existingPolicy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
        ensurePolicyIsNotCancelled(existingPolicy);

        // Store previous premium for event
        Long previousPremium = existingPolicy.getPremium();

        boolean policyNumberChanged = updatedPolicy.getPolicyNumber() != null
                && !updatedPolicy.getPolicyNumber().equals(existingPolicy.getPolicyNumber());
        boolean customerIdChanged = updatedPolicy.getCustomerId() != null
                && !updatedPolicy.getCustomerId().equals(existingPolicy.getCustomerId());

        if ((policyNumberChanged || customerIdChanged)
                && (changeJustification == null || changeJustification.isBlank())) {
            throw new InvalidPolicyException(
                    "Updating immutable identifiers requires explicit justification");
        }

        if (policyNumberChanged) {
            policyRepository.findByPolicyNumber(updatedPolicy.getPolicyNumber())
                    .filter(existing -> !existing.getPolicyId().equals(existingPolicy.getPolicyId()))
                    .ifPresent(existing -> {
                        throw new DuplicatePolicyException(updatedPolicy.getPolicyNumber());
                    });
            existingPolicy.setPolicyNumber(updatedPolicy.getPolicyNumber());
        }
        if (customerIdChanged) {
            existingPolicy.setCustomerId(updatedPolicy.getCustomerId());
        }

        // Update only non-null fields
        if (updatedPolicy.getCoverage() != null) {
            existingPolicy.setCoverage(updatedPolicy.getCoverage());
        }
        if (updatedPolicy.getPremium() != null) {
            existingPolicy.setPremium(updatedPolicy.getPremium());
        }
        if (updatedPolicy.getStartDate() != null) {
            existingPolicy.setStartDate(updatedPolicy.getStartDate());
        }
        if (updatedPolicy.getEndDate() != null) {
            existingPolicy.setEndDate(updatedPolicy.getEndDate());
        }
        if (updatedPolicy.getPropertyDescription() != null) {
            existingPolicy.setPropertyDescription(updatedPolicy.getPropertyDescription());
        }
        if (updatedPolicy.getPropertyAddress() != null) {
            existingPolicy.setPropertyAddress(updatedPolicy.getPropertyAddress());
        }

        existingPolicy.setUpdatedAt(LocalDateTime.now());
        existingPolicy.validate();

        Policy savedPolicy;
        try {
            savedPolicy = policyRepository.update(existingPolicy);
        } catch (DuplicateKeyException ex) {
            throw new DuplicatePolicyException(existingPolicy.getPolicyNumber());
        }
        log.info("Policy updated successfully: {}", policyId);

        // Publish event
        PolicyUpdatedEvent event = PolicyUpdatedEvent.from(savedPolicy, previousPremium);
        policyEventPublisher.publishPolicyUpdated(event);

        return savedPolicy;
    }

    @Override
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "read"})
    public Optional<Policy> getPolicyById(String policyId) {
        log.debug("Retrieving policy: {}", policyId);
        return getPolicyById(policyId, false);
    }

    public Optional<Policy> getPolicyById(String policyId, boolean includeDeleted) {
        log.debug("Retrieving policy: {} includeDeleted={}", policyId, includeDeleted);
        return includeDeleted ? policyRepository.findByIdIncludingDeleted(policyId) : policyRepository.findById(policyId);
    }

    @Override
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "customerPolicies"})
    public List<Policy> getCustomerPolicies(String customerId) {
        log.debug("Retrieving policies for customer: {}", customerId);
        return policyRepository.findByCustomerId(customerId);
    }

    @Override
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "delete"})
    public void deletePolicy(String policyId, String reason) {
        log.info("Deleting policy: {}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));

        policyRepository.deleteById(policyId, reason);
        log.info("Policy deleted: {}", policyId);

        // Publish event
        PolicyDeletedEvent event = PolicyDeletedEvent.from(policy, reason);
        policyEventPublisher.publishPolicyDeleted(event);
    }

    @Override
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "cancel"})
    public void cancelPolicy(String policyId) {
        log.info("Cancelling policy: {}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));

        policy.cancel();
        policyRepository.update(policy);

        log.info("Policy cancelled: {}", policyId);

        // Publish event
        PolicyUpdatedEvent event = PolicyUpdatedEvent.from(policy, null);
        policyEventPublisher.publishPolicyUpdated(event);
    }

    @Override
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "suspend"})
    public void suspendPolicy(String policyId) {
        log.info("Suspending policy: {}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));

        policy.suspend();
        policyRepository.update(policy);

        log.info("Policy suspended: {}", policyId);

        // Publish event
        PolicyUpdatedEvent event = PolicyUpdatedEvent.from(policy, null);
        policyEventPublisher.publishPolicyUpdated(event);
    }

    @Override
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "reactivate"})
    public void reactivatePolicy(String policyId) {
        log.info("Reactivating policy: {}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));

        policy.reactivate();
        policyRepository.update(policy);

        log.info("Policy reactivated: {}", policyId);

        // Publish event
        PolicyUpdatedEvent event = PolicyUpdatedEvent.from(policy, null);
        policyEventPublisher.publishPolicyUpdated(event);
    }

    @Override
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "updatePremium"})
    public Policy updatePremium(String policyId, Long newPremium) {
        log.info("Updating premium for policy: {} to: {}", policyId, newPremium);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
        ensurePolicyIsNotCancelled(policy);

        Long previousPremium = policy.getPremium();
        policy.updatePremium(newPremium);

        Policy updatedPolicy = policyRepository.update(policy);
        log.info("Premium updated for policy: {}", policyId);

        // Publish event
        PolicyUpdatedEvent event = PolicyUpdatedEvent.from(updatedPolicy, previousPremium);
        policyEventPublisher.publishPolicyUpdated(event);

        return updatedPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "listAll"})
    public List<Policy> getAllPolicies() {
        log.debug("Retrieving all policies");
        return policyRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "listPage"})
    public List<Policy> getPoliciesPage(int page, int size, String sortBy, String sortDirection) {
        return getPoliciesPage(page, size, sortBy, sortDirection, false);
    }

    @Transactional(readOnly = true)
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "listPageIncludeDeleted"})
    public List<Policy> getPoliciesPage(int page, int size, String sortBy, String sortDirection, boolean includeDeleted) {
        boolean ascending = !"desc".equalsIgnoreCase(sortDirection);
        return policyRepository.findAll(page, size, sortBy, ascending, includeDeleted);
    }

    @Transactional(readOnly = true)
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "count"})
    public long getTotalPolicies() {
        return getTotalPolicies(false);
    }

    @Transactional(readOnly = true)
    @Timed(value = "insurance.policy.service", extraTags = {"operation", "countIncludeDeleted"})
    public long getTotalPolicies(boolean includeDeleted) {
        return policyRepository.count(includeDeleted);
    }

    /**
     * Converts a Policy domain entity to a response DTO
     * 
     * @param policy the domain entity
     * @return the response DTO
     */
    public PolicyResponseDTO toPolicyResponseDTO(Policy policy) {
        return policyMapper.domainToResponseDTO(policy);
    }

    /**
     * Converts a list of Policy domain entities to response DTOs
     * 
     * @param policies the domain entities
     * @return the response DTOs
     */
    public List<PolicyResponseDTO> toPolicyResponseDTOs(List<Policy> policies) {
        return policies.stream()
                .map(policyMapper::domainToResponseDTO)
                .collect(Collectors.toList());
    }

    private void ensurePolicyIsNotCancelled(Policy policy) {
        if (policy.getStatus() == Policy.PolicyStatus.CANCELLED) {
            throw new InvalidPolicyException("Cancelled policies cannot be changed");
        }
    }
}
