package com.itau.insurance.patrimonial.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

@Schema(name = "OpenInsuranceLinks")
public record OpenInsuranceLinksDTO(
        String self,
        String first,
        String prev,
        String next,
        String last) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
