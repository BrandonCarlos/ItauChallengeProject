package com.itau.insurance.patrimonial.entrypoint.rest.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Data Transfer Object for Open Insurance API compliant response envelope.
 * This DTO provides the standardized response structure for Open Insurance API endpoints,
 * following the format specified by the Brazilian Central Bank (Banco Central do Brasil).
 */
public record OpenInsuranceResponseDTO(
    List<InsuranceProductDTO> data,
    MetaDTO meta,
    LinksDTO links
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Record for response metadata
     */
    public record MetaDTO(
        Integer totalRecords,
        Integer totalPages,
        Integer currentPage,
        Integer pageSize
    ) implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
    }

    /**
     * Record for HATEOAS links
     */
    public record LinksDTO(
        String self,
        String first,
        String prev,
        String next,
        String last
    ) implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
