package com.itau.insurance.patrimonial.domain.ports;

import com.itau.insurance.patrimonial.domain.model.Policy;

import java.util.List;
import java.util.Optional;

/**
 * Output Port (Repository Pattern) for persisting and retrieving Policy entities.
 * This port abstracts the persistence layer, allowing the domain to remain independent
 * of specific database implementations.
 */
public interface PolicyRepositoryPort {

    /**
     * Saves a new Policy to the repository
     * 
     * @param policy the policy to save
     * @return the saved policy
     */
    Policy save(Policy policy);

    /**
     * Updates an existing Policy in the repository
     * 
     * @param policy the policy to update
     * @return the updated policy
     */
    Policy update(Policy policy);

    /**
     * Retrieves a Policy by its ID
     * 
     * @param policyId the policy identifier
     * @return an Optional containing the policy if found
     */
    Optional<Policy> findById(String policyId);

    /**
     * Retrieves a Policy by its ID, including soft-deleted records.
     *
     * @param policyId the policy identifier
     * @return an Optional containing the policy if found
     */
    Optional<Policy> findByIdIncludingDeleted(String policyId);

    /**
     * Retrieves all policies for a specific customer
     * 
     * @param customerId the customer identifier
     * @return a list of policies for the customer
     */
    List<Policy> findByCustomerId(String customerId);

    /**
     * Retrieves a Policy by its policy number
     * 
     * @param policyNumber the policy number
     * @return an Optional containing the policy if found
     */
    Optional<Policy> findByPolicyNumber(String policyNumber);

    /**
     * Retrieves a Policy by its idempotency key
     * 
     * @param idempotencyKey the idempotency key
     * @return an Optional containing the policy if found
     */
    Optional<Policy> findByIdempotencyKey(String idempotencyKey);

    /**
     * Deletes a Policy by its ID
     * 
     * @param policyId the policy identifier
     */
    void deleteById(String policyId, String reason);

    /**
     * Checks if a Policy exists by its ID
     * 
     * @param policyId the policy identifier
     * @return true if policy exists, false otherwise
     */
    boolean existsById(String policyId);

    /**
     * Retrieves all policies
     * 
     * @return a list of all policies
     */
    List<Policy> findAll();

    /**
     * Retrieves a page of policies with sorting.
     *
     * @param page zero-based page index
     * @param size page size
     * @param sortBy field used to sort
     * @param ascending sort direction
     * @return policies in requested page window
     */
    List<Policy> findAll(int page, int size, String sortBy, boolean ascending);

    /**
     * Retrieves a page of policies with sorting, optionally including soft-deleted records.
     *
     * @param page zero-based page index
     * @param size page size
     * @param sortBy field used to sort
     * @param ascending sort direction
     * @param includeDeleted whether deleted records should be returned
     * @return policies in requested page window
     */
    List<Policy> findAll(int page, int size, String sortBy, boolean ascending, boolean includeDeleted);

    /**
     * Counts total number of policies
     * 
     * @return the count of policies
     */
    long count();

    /**
     * Counts total number of policies, optionally including soft-deleted records.
     *
     * @param includeDeleted whether deleted records should be counted
     * @return the count of policies
     */
    long count(boolean includeDeleted);
}
