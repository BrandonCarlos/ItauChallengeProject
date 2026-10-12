package com.itau.insurance.patrimonial.domain.ports;

import com.itau.insurance.patrimonial.domain.model.Policy;

import java.util.List;
import java.util.Optional;

/**
 * Input Port (Use Case Port) that defines the contract for Policy use cases.
 * This port represents the boundary between the application layer and domain layer,
 * ensuring that external actors interact with the policy domain through well-defined
 * use cases rather than directly accessing domain logic.
 */
public interface PolicyUseCasePort {

    /**
     * Creates a new insurance policy
     * 
     * @param policy the policy to create
     * @param idempotencyKey optional idempotency key for preventing duplicate creations
     * @return the created policy
     */
    Policy createPolicy(Policy policy, String idempotencyKey);

    /**
     * Updates an existing policy
     * 
     * @param policyId the policy identifier
     * @param policy the updated policy data
     * @param changeJustification explicit reason required to modify immutable identifiers
     * @return the updated policy
     */
    Policy updatePolicy(String policyId, Policy policy, String changeJustification);

    /**
     * Retrieves a policy by its ID
     * 
     * @param policyId the policy identifier
     * @return the policy if found
     */
    Optional<Policy> getPolicyById(String policyId);

    /**
     * Retrieves all policies for a customer
     * 
     * @param customerId the customer identifier
     * @return a list of customer policies
     */
    List<Policy> getCustomerPolicies(String customerId);

    /**
     * Deletes a policy
     * 
     * @param policyId the policy identifier
     * @param reason the reason for deletion
     */
    void deletePolicy(String policyId, String reason);

    /**
     * Cancels a policy
     * 
     * @param policyId the policy identifier
     */
    void cancelPolicy(String policyId);

    /**
     * Suspends a policy
     * 
     * @param policyId the policy identifier
     */
    void suspendPolicy(String policyId);

    /**
     * Reactivates a suspended policy
     * 
     * @param policyId the policy identifier
     */
    void reactivatePolicy(String policyId);

    /**
     * Updates the premium of a policy
     * 
     * @param policyId the policy identifier
     * @param newPremium the new premium amount
     * @return the updated policy
     */
    Policy updatePremium(String policyId, Long newPremium);

    /**
     * Retrieves all policies
     * 
     * @return a list of all policies
     */
    List<Policy> getAllPolicies();
}
