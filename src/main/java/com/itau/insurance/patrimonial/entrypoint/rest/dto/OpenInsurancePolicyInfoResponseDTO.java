package com.itau.insurance.patrimonial.entrypoint.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "OpenInsurancePolicyInfoResponse")
public record OpenInsurancePolicyInfoResponseDTO(
        PolicyInfoDTO data,
        OpenInsuranceLinksDTO links,
        OpenInsuranceMetaDTO meta) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "OpenInsurancePolicyInfo")
    public record PolicyInfoDTO(
            String policyId,
            String policyNumber,
            String productName,
            LocalDate startDate,
            LocalDate endDate,
            List<ProposalDTO> proposals,
            List<InsuredDTO> insureds) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Schema(name = "OpenInsuranceProposal")
    public record ProposalDTO(String proposalId) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Schema(name = "OpenInsuranceInsured")
    public record InsuredDTO(String identification) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }
}
