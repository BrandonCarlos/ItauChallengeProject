package com.itau.insurance.patrimonial.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Schema(name = "OpenInsuranceListResponse")
public record OpenInsuranceListResponseDTO(
        List<BrandDTO> data,
        OpenInsuranceLinksDTO links,
        OpenInsuranceMetaDTO meta) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "OpenInsuranceBrand")
    public record BrandDTO(
            String brand,
            List<CompanyDTO> companies) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Schema(name = "OpenInsuranceCompany")
    public record CompanyDTO(
            String companyName,
            String cnpjNumber,
            List<PolicySummaryDTO> policies) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Schema(name = "OpenInsurancePolicySummary")
    public record PolicySummaryDTO(
            String policyId,
            String productName) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }
}
