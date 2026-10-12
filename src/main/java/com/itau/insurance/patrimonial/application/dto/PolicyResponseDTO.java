package com.itau.insurance.patrimonial.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Policy responses.
 * This DTO is used at the application boundary to send policy data to external consumers.
 */
public record PolicyResponseDTO(
    String policyId,
    String policyNumber,
    String customerId,
    String coverage,
    Long premium,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate endDate,
    
    String status,
    String propertyDescription,
    String propertyAddress,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,
    
    String idempotencyKey
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
}
