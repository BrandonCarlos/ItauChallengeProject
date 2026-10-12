package com.itau.insurance.patrimonial.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Schema(name = "OpenInsurancePremiumResponse")
public record OpenInsurancePremiumResponseDTO(
        PremiumDTO data,
        OpenInsuranceLinksDTO links,
        OpenInsuranceMetaDTO meta) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "OpenInsurancePremium")
    public record PremiumDTO(
            Integer paymentsQuantity,
            AmountDTO amount,
            List<CoverageDTO> coverages) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Schema(name = "OpenInsuranceAmount")
    public record AmountDTO(Long amount, String unit) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Schema(name = "OpenInsuranceCoverage")
    public record CoverageDTO(String type) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }
}
