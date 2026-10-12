package com.itau.insurance.patrimonial.infrastructure.persistence.repository;

import com.itau.insurance.patrimonial.infrastructure.persistence.document.PolicyDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyMongoRepository extends MongoRepository<PolicyDocument, String> {

    Optional<PolicyDocument> findByIdAndDeletedFalse(String id);

    boolean existsByIdAndDeletedFalse(String id);

    Optional<PolicyDocument> findByPolicyNumber(String policyNumber);

    Optional<PolicyDocument> findByIdempotencyKey(String idempotencyKey);

    Optional<PolicyDocument> findByExternalIdAndExternalSystem(String externalId, String externalSystem);

    List<PolicyDocument> findByCustomerIdAndDeletedFalse(String customerId);

    List<PolicyDocument> findByCustomerIdAndStatus(String customerId, String status);

    List<PolicyDocument> findByStatus(String status);

    List<PolicyDocument> findByStatusAndCreatedAtGreaterThan(String status, LocalDateTime createdAt);

    @Query("{'status': ?0, 'startDate': {$gte: ?1, $lte: ?2}}")
    List<PolicyDocument> findByStatusAndDateRange(String status, LocalDateTime startDate, LocalDateTime endDate);

    @Query("{'customerId': ?0, 'status': {$in: ?1}}")
    List<PolicyDocument> findByCustomerIdAndStatusIn(String customerId, List<String> statuses);

    @Query("{'status': ?0, 'active': true, 'deleted': false}")
    List<PolicyDocument> findActiveByStatus(String status);

    List<PolicyDocument> findByExternalSystem(String externalSystem);

    @Query("{'syncStatus': ?0, 'deleted': false}")
    List<PolicyDocument> findBySyncStatus(String syncStatus);

    @Query("{'syncStatus': ?0, 'lastSyncAt': {$lte: ?1}}")
    List<PolicyDocument> findBySyncStatusAndLastSyncAtBefore(String syncStatus, LocalDateTime beforeDate);

    @Query("{'active': true, 'deleted': false, 'createdAt': {$gte: ?0}}")
    List<PolicyDocument> findActivePoliciesCreatedAfter(LocalDateTime createdAt);

    @Query(value = "{'customerId': ?0, 'status': ?1, 'deleted': false}", count = true)
    long countByCustomerIdAndStatus(String customerId, String status);

    @Query("{'cpf': ?0, 'deleted': false}")
    List<PolicyDocument> findByCpf(String cpf);

    @Query("{'externalId': ?0, 'externalSystem': ?1, 'deleted': false}")
    Optional<PolicyDocument> findActiveByExternalIdAndSystem(String externalId, String externalSystem);

    @Query("{'status': ?0, 'createdAt': {$gte: ?1, $lte: ?2}, 'deleted': false}")
    List<PolicyDocument> findByStatusAndCreatedAtBetween(String status, LocalDateTime startDate, LocalDateTime endDate);

    @Query("{'policyNumber': ?0, 'deleted': false}")
    Optional<PolicyDocument> findActiveByPolicyNumber(String policyNumber);

    @Override
    Page<PolicyDocument> findAll(Pageable pageable);

    List<PolicyDocument> findByDeletedFalse();

    Page<PolicyDocument> findByDeletedFalse(Pageable pageable);

    long countByDeletedFalse();
}
