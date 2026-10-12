package com.itau.insurance.patrimonial.domain.model;

import com.itau.insurance.patrimonial.domain.exception.InvalidPolicyException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain entity representing an Insurance Policy in the Patrimonial Insurance context.
 * This entity encapsulates the core business logic and state of an insurance policy.
 * 
 * The Policy follows the Clean Architecture principle of being independent of external frameworks
 * and is focused on domain logic and business rules.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "policyId")
@ToString
public class Policy implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Long MINIMUM_PREMIUM = 0L;

    /**
     * Unique policy identifier
     */
    private String policyId;

    /**
     * Customer ID associated with this policy
     */
    private String customerId;

    /**
     * Policy number as reference
     */
    private String policyNumber;

    /**
     * Coverage type (e.g., RESIDENTIAL, COMMERCIAL, CONDOMINIAL)
     */
    private String coverage;

    /**
     * Premium amount in centavos
     */
    private Long premium;

    /**
     * Policy start date
     */
    private LocalDate startDate;

    /**
     * Policy end date
     */
    private LocalDate endDate;

    /**
     * Current policy status
     */
    private PolicyStatus status;

    /**
     * Additional property description
     */
    private String propertyDescription;

    /**
     * Property address
     */
    private String propertyAddress;

    /**
     * Policy creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Policy last update timestamp
     */
    private LocalDateTime updatedAt;

    /**
     * Idempotency key for ensuring idempotent operations
     */
    private String idempotencyKey;

    /**
     * Validates the Policy according to domain business rules.
     * 
     * @throws InvalidPolicyException if any validation fails
     */
    public void validate() {
        if (policyNumber == null || policyNumber.isBlank()) {
            throw new InvalidPolicyException("Policy number cannot be null or empty");
        }

        if (customerId == null || customerId.isBlank()) {
            throw InvalidPolicyException.ofInvalidCustomer("Customer ID cannot be null or empty");
        }

        if (coverage == null || coverage.isBlank()) {
            throw InvalidPolicyException.ofInvalidCoverage("Coverage type cannot be null or empty");
        }

        if (premium == null || premium <= MINIMUM_PREMIUM) {
            throw InvalidPolicyException.ofInvalidPremium(premium != null ? premium : -1);
        }

        if (startDate == null || endDate == null) {
            throw new InvalidPolicyException("Start and end dates are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new InvalidPolicyException("Start date must be before or equal to end date");
        }

        if (status == null) {
            throw new InvalidPolicyException("Policy status is required");
        }
    }

    /**
     * Creates a new policy with generated ID and current timestamp.
     * 
     * @param customerId the customer ID
     * @param coverage the coverage type
     * @param premium the premium amount
     * @param startDate the start date
     * @param endDate the end date
     * @return a new Policy instance
     */
    public static Policy create(String customerId, String coverage, Long premium, 
                                 LocalDate startDate, LocalDate endDate) {
        return Policy.builder()
                .policyId(UUID.randomUUID().toString())
                .customerId(customerId)
                .coverage(coverage)
                .premium(premium)
                .startDate(startDate)
                .endDate(endDate)
                .status(PolicyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Marks the policy as cancelled
     */
    public void cancel() {
        this.status = PolicyStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks the policy as suspended
     */
    public void suspend() {
        this.status = PolicyStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reactivates a suspended policy
     */
    public void reactivate() {
        if (this.status == PolicyStatus.SUSPENDED) {
            this.status = PolicyStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Updates the premium for this policy
     * 
     * @param newPremium the new premium amount
     * @throws InvalidPolicyException if premium is invalid
     */
    public void updatePremium(Long newPremium) {
        if (newPremium == null || newPremium <= MINIMUM_PREMIUM) {
            throw InvalidPolicyException.ofInvalidPremium(newPremium != null ? newPremium : -1);
        }
        this.premium = newPremium;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Enum representing the possible states of a Policy
     */
    public enum PolicyStatus {
        ACTIVE,
        SUSPENDED,
        CANCELLED,
        EXPIRED
    }
}
