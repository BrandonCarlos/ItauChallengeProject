package com.itau.insurance.patrimonial.entrypoint.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Open Insurance API compliant insurance product information.
 * This DTO represents an insurance product (policy) in the format required by the
 * Brazilian Open Insurance standard (Banco Central do Brasil).
 */
public record InsuranceProductDTO(
    String id,
    String name,
    String description,
    String customerId,
    String policyNumber,
    String coverage,
    String status,
    Long premiumAmount,
    String premiumCurrency,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate endDate,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
}
