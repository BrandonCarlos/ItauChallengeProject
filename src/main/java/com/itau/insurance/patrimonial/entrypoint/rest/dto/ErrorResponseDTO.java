package com.itau.insurance.patrimonial.entrypoint.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object for standardized error responses.
 * This DTO is used to provide consistent error information across all API endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp,
    
    Integer status,
    String error,
    String message,
    String code,
    Map<String, ?> details,
    String path
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
}
