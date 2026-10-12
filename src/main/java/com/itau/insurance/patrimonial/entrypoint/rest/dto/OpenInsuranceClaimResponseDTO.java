package com.itau.insurance.patrimonial.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "OpenInsuranceClaimResponse")
public record OpenInsuranceClaimResponseDTO(
        List<ClaimDTO> data,
        OpenInsuranceLinksDTO links,
        OpenInsuranceMetaDTO meta) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "OpenInsuranceClaim")
    public record ClaimDTO(
            String claimId,
            LocalDateTime documentationDeliveryDate,
            String status) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }
}
