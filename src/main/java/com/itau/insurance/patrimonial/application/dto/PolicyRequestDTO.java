package com.itau.insurance.patrimonial.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Data Transfer Object for Policy creation and update requests.
 * This DTO is used at the application boundary to receive data from external sources.
 */
public record PolicyRequestDTO(
    @NotBlank(message = "Policy number is required")
    @Size(min = 1, max = 50, message = "Policy number must be between 1 and 50 characters")
    String policyNumber,

    @NotBlank(message = "Customer ID is required")
    @Size(min = 1, max = 50, message = "Customer ID must be between 1 and 50 characters")
    String customerId,
    
    @NotBlank(message = "Coverage type is required")
    @Size(min = 1, max = 50, message = "Coverage must be between 1 and 50 characters")
    String coverage,
    
    @NotNull(message = "Premium is required")
    @Positive(message = "Premium must be a positive value")
    Long premium,
    
    @NotNull(message = "Start date is required")
    LocalDate startDate,
    
    @NotNull(message = "End date is required")
    LocalDate endDate,
    
    @Size(max = 500, message = "Property description must not exceed 500 characters")
    String propertyDescription,
    
    @Size(max = 200, message = "Property address must not exceed 200 characters")
    String propertyAddress,

    @Size(max = 500, message = "Change justification must not exceed 500 characters")
    String changeJustification,
     
    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    String idempotencyKey
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
}
