package com.itau.insurance.patrimonial.entrypoint.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itau.insurance.patrimonial.application.dto.PolicyRequestDTO;
import com.itau.insurance.patrimonial.application.dto.PolicyResponseDTO;
import com.itau.insurance.patrimonial.application.service.PolicyApplicationService;
import com.itau.insurance.patrimonial.domain.exception.PolicyNotFoundException;
import com.itau.insurance.patrimonial.domain.model.Policy;
import com.itau.insurance.patrimonial.domain.model.Policy.PolicyStatus;
import com.itau.insurance.patrimonial.entrypoint.rest.dto.PolicyPageResponseDTO;
import com.itau.insurance.patrimonial.infrastructure.cache.IdempotencyRequest;
import com.itau.insurance.patrimonial.infrastructure.cache.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Hidden;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Policy management operations.
 * This controller is an adapter that translates HTTP requests into domain use cases.
 * It follows REST conventions and acts as the entry point for policy operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Validated
public class PolicyController {

    private final PolicyApplicationService policyApplicationService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new insurance policy
     * 
     * @param requestDTO the policy request data
     * @param idempotencyKey optional idempotency key for deduplication
     * @return the created policy response
     */
    @PostMapping
    @Timed(value = "insurance.http.request", extraTags = {"operation", "createPolicy"})
    @Operation(
            summary = "Create policy",
            description = "Creates a policy and returns 201 Created. Reusing the same Idempotency-Key for the same request returns the same result."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content)
    })
    public ResponseEntity<PolicyResponseDTO> createPolicy(
            @Valid @RequestBody PolicyRequestDTO requestDTO,
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
            @RequestHeader(name = "Idempotency-Key", required = true) String idempotencyKey) {

        log.info("Received request to create policy for customer: {}", requestDTO.customerId());
        IdempotencyRequest existing = getCompletedRequest(idempotencyKey);
        if (existing != null) {
            return cachedPolicyResponse(existing);
        }

        try {
            markProcessing(idempotencyKey, "POST", "/api/v1/policies", requestDTO);
            Policy policy = Policy.builder()
                    .policyNumber(requestDTO.policyNumber())
                    .customerId(requestDTO.customerId())
                    .coverage(requestDTO.coverage())
                    .premium(requestDTO.premium())
                    .startDate(requestDTO.startDate())
                    .endDate(requestDTO.endDate())
                    .status(PolicyStatus.ACTIVE)
                    .propertyDescription(requestDTO.propertyDescription())
                    .propertyAddress(requestDTO.propertyAddress())
                    .build();

            Policy createdPolicy = policyApplicationService.createPolicy(policy, idempotencyKey);
            PolicyResponseDTO responseDTO = policyApplicationService.toPolicyResponseDTO(createdPolicy);
            markCompleted(idempotencyKey, HttpStatus.CREATED.value(), responseDTO);

            log.info("Policy created successfully: {}", createdPolicy.getPolicyId());
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (RuntimeException ex) {
            markFailed(idempotencyKey, ex);
            throw ex;
        }
    }

    /**
     * Retrieves a policy by its ID
     * 
     * @param policyId the policy identifier
     * @return the policy response if found
     */
    @GetMapping("/{policyId}")
    @Timed(value = "insurance.http.request", extraTags = {"operation", "getPolicyById"})
    @Operation(
            summary = "Get policy by id",
            description = "By default, soft-deleted records are hidden. Use includeDeleted=true to query deleted records."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    public ResponseEntity<PolicyResponseDTO> getPolicyById(
            @PathVariable String policyId,
            @Parameter(description = "When true, includes soft-deleted records in this query.")
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        log.info("Received request to retrieve policy: {}", policyId);

        return policyApplicationService.getPolicyById(policyId, includeDeleted)
                .map(policy -> ResponseEntity.ok(policyApplicationService.toPolicyResponseDTO(policy)))
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
    }

    /**
     * Retrieves all policies for a specific customer
     * 
     * @param customerId the customer identifier
     * @return a list of customer policies
     */
    @Hidden
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<PolicyResponseDTO>> getCustomerPolicies(@PathVariable String customerId) {
        log.info("Received request to retrieve policies for customer: {}", customerId);
        
        List<Policy> policies = policyApplicationService.getCustomerPolicies(customerId);
        List<PolicyResponseDTO> responseDTOs = policyApplicationService.toPolicyResponseDTOs(policies);
        
        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * Retrieves all policies
     * 
     * @return a list of all policies
     */
    @GetMapping
    @Timed(value = "insurance.http.request", extraTags = {"operation", "listPolicies"})
    @Operation(
            summary = "List policies",
            description = "By default, soft-deleted records are excluded. Use includeDeleted=true to include deleted records in the result."
    )
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<PolicyPageResponseDTO> getAllPolicies(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @Parameter(description = "When true, includes soft-deleted records in the list and totals.")
            @RequestParam(defaultValue = "false") boolean includeDeleted) {

        log.info("Received request to retrieve policies page={}, size={}, includeDeleted={}", page, size, includeDeleted);
        List<Policy> policies = policyApplicationService.getPoliciesPage(page, size, sortBy, sortDirection, includeDeleted);
        List<PolicyResponseDTO> responseDTOs = policyApplicationService.toPolicyResponseDTOs(policies);
        long totalElements = policyApplicationService.getTotalPolicies(includeDeleted);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        PolicyPageResponseDTO response = new PolicyPageResponseDTO(
                responseDTOs,
                page,
                size,
                totalElements,
                totalPages,
                sortBy,
                sortDirection
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing policy
     * 
     * @param policyId the policy identifier
     * @param requestDTO the updated policy data
     * @return the updated policy response
     */
    @PutMapping("/{policyId}")
    @Timed(value = "insurance.http.request", extraTags = {"operation", "updatePolicy"})
    @Operation(
            summary = "Update policy",
            description = "Updates mutable fields. Changing immutable identifiers (policyNumber/customerId) requires changeJustification. Cancelled policies cannot be changed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Invalid request or business rule violation", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    public ResponseEntity<PolicyResponseDTO> updatePolicy(
            @PathVariable String policyId,
            @Valid @RequestBody PolicyRequestDTO requestDTO,
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
            @RequestHeader(name = "Idempotency-Key", required = true) String idempotencyKey) {

        log.info("Received request to update policy: {}", policyId);
        IdempotencyRequest existing = getCompletedRequest(idempotencyKey);
        if (existing != null) {
            return cachedPolicyResponse(existing);
        }

        try {
            markProcessing(idempotencyKey, "PUT", "/api/v1/policies/" + policyId, requestDTO);
            Policy updatedPolicy = Policy.builder()
                    .policyNumber(requestDTO.policyNumber())
                    .customerId(requestDTO.customerId())
                    .coverage(requestDTO.coverage())
                    .premium(requestDTO.premium())
                    .startDate(requestDTO.startDate())
                    .endDate(requestDTO.endDate())
                    .propertyDescription(requestDTO.propertyDescription())
                    .propertyAddress(requestDTO.propertyAddress())
                    .build();

            Policy policy = policyApplicationService.updatePolicy(
                    policyId,
                    updatedPolicy,
                    requestDTO.changeJustification());
            PolicyResponseDTO responseDTO = policyApplicationService.toPolicyResponseDTO(policy);
            markCompleted(idempotencyKey, HttpStatus.OK.value(), responseDTO);

            log.info("Policy updated successfully: {}", policyId);
            return ResponseEntity.ok(responseDTO);
        } catch (RuntimeException ex) {
            markFailed(idempotencyKey, ex);
            throw ex;
        }
    }

    /**
     * Updates the premium for a policy
     * 
     * @param policyId the policy identifier
     * @param newPremium the new premium amount
     * @return the updated policy response
     */
    @Hidden
    @PutMapping("/{policyId}/premium")
    public ResponseEntity<PolicyResponseDTO> updatePremium(
            @PathVariable String policyId,
            @RequestParam Long newPremium) {
        
        log.info("Received request to update premium for policy: {} to: {}", policyId, newPremium);
        
        Policy updatedPolicy = policyApplicationService.updatePremium(policyId, newPremium);
        PolicyResponseDTO responseDTO = policyApplicationService.toPolicyResponseDTO(updatedPolicy);
        
        log.info("Premium updated successfully for policy: {}", policyId);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Cancels a policy
     * 
     * @param policyId the policy identifier
     * @return response entity with no content
     */
    @Hidden
    @PostMapping("/{policyId}/cancel")
    public ResponseEntity<Void> cancelPolicy(@PathVariable String policyId) {
        log.info("Received request to cancel policy: {}", policyId);
        
        policyApplicationService.cancelPolicy(policyId);
        
        log.info("Policy cancelled successfully: {}", policyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Suspends a policy
     * 
     * @param policyId the policy identifier
     * @return response entity with no content
     */
    @Hidden
    @PostMapping("/{policyId}/suspend")
    public ResponseEntity<Void> suspendPolicy(@PathVariable String policyId) {
        log.info("Received request to suspend policy: {}", policyId);
        
        policyApplicationService.suspendPolicy(policyId);
        
        log.info("Policy suspended successfully: {}", policyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a suspended policy
     * 
     * @param policyId the policy identifier
     * @return response entity with no content
     */
    @Hidden
    @PostMapping("/{policyId}/reactivate")
    public ResponseEntity<Void> reactivatePolicy(@PathVariable String policyId) {
        log.info("Received request to reactivate policy: {}", policyId);
        
        policyApplicationService.reactivatePolicy(policyId);
        
        log.info("Policy reactivated successfully: {}", policyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes a policy
     * 
     * @param policyId the policy identifier
     * @param reason optional reason for deletion
     * @return response entity with no content
     */
    @DeleteMapping("/{policyId}")
    @Timed(value = "insurance.http.request", extraTags = {"operation", "deletePolicy"})
    @Operation(
            summary = "Delete policy",
            description = "Performs soft delete. Deleted records are hidden from default reads and can be queried with includeDeleted=true."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    public ResponseEntity<Void> deletePolicy(
            @PathVariable String policyId,
            @Parameter(description = "Optional reason stored as deletion metadata.")
            @RequestParam(required = false) String reason,
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
            @RequestHeader(name = "Idempotency-Key", required = true) String idempotencyKey) {

        log.info("Received request to delete policy: {}", policyId);
        IdempotencyRequest existing = getCompletedRequest(idempotencyKey);
        if (existing != null) {
            HttpStatus status = HttpStatus.valueOf(existing.responseStatus() == null ? HttpStatus.NO_CONTENT.value() : existing.responseStatus());
            return ResponseEntity.status(status).build();
        }

        try {
            markProcessing(idempotencyKey, "DELETE", "/api/v1/policies/" + policyId, reason);
            policyApplicationService.deletePolicy(policyId, reason);
            markCompleted(idempotencyKey, HttpStatus.NO_CONTENT.value(), null);
            log.info("Policy deleted successfully: {}", policyId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            markFailed(idempotencyKey, ex);
            throw ex;
        }
    }

    private IdempotencyRequest getCompletedRequest(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyService.getRequest(idempotencyKey)
                .filter(request -> IdempotencyRequest.RequestStatus.COMPLETED.getValue().equals(request.status()))
                .orElse(null);
    }

    private void markProcessing(String idempotencyKey, String method, String path, Object requestBody) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        IdempotencyRequest request = new IdempotencyRequest(
                idempotencyKey,
                method,
                path,
                Map.of(),
                writeBody(requestBody),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                null,
                null,
                Map.of(),
                null,
                IdempotencyRequest.RequestStatus.PENDING.getValue(),
                LocalDateTime.now(),
                null,
                LocalDateTime.now().plusDays(1),
                Map.of(),
                null,
                null,
                1
        );
        idempotencyService.saveRequest(idempotencyKey, request);
        idempotencyService.markAsProcessing(idempotencyKey);
    }

    private void markCompleted(String idempotencyKey, int status, Object body) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        idempotencyService.markAsCompleted(idempotencyKey, status, writeBody(body));
    }

    private void markFailed(String idempotencyKey, Exception ex) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        idempotencyService.markAsFailed(idempotencyKey, ex.getMessage());
    }

    private ResponseEntity<PolicyResponseDTO> cachedPolicyResponse(IdempotencyRequest request) {
        try {
            if (request.responseBody() != null && !request.responseBody().isBlank()) {
                PolicyResponseDTO dto = objectMapper.readValue(request.responseBody(), PolicyResponseDTO.class);
                HttpStatus status = HttpStatus.valueOf(request.responseStatus() == null ? HttpStatus.OK.value() : request.responseStatus());
                return ResponseEntity.status(status).body(dto);
            }
            HttpStatus status = HttpStatus.valueOf(request.responseStatus() == null ? HttpStatus.OK.value() : request.responseStatus());
            return ResponseEntity.status(status).build();
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached idempotent response", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    private String writeBody(Object payload) {
        try {
            return payload == null ? null : objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
