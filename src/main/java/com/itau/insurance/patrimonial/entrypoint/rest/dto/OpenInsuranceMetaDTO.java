package com.itau.insurance.patrimonial.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

@Schema(name = "OpenInsuranceMeta")
public record OpenInsuranceMetaDTO(
        Integer totalRecords,
        Integer totalPages) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
