package com.itau.insurance.patrimonial.entrypoint.rest.dto;

import com.itau.insurance.patrimonial.application.dto.PolicyResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Schema(name = "PolicyPageResponseDTO")
public record PolicyPageResponseDTO(
        List<PolicyResponseDTO> content,
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages,
        String sortBy,
        String sortDirection) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
