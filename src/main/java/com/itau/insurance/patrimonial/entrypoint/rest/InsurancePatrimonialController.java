package com.itau.insurance.patrimonial.entrypoint.rest;

import com.itau.insurance.patrimonial.application.dto.PolicyResponseDTO;
import com.itau.insurance.patrimonial.application.service.PolicyApplicationService;
import com.itau.insurance.patrimonial.domain.exception.PolicyNotFoundException;
import com.itau.insurance.patrimonial.entrypoint.rest.dto.OpenInsuranceClaimResponseDTO;
import com.itau.insurance.patrimonial.entrypoint.rest.dto.OpenInsuranceListResponseDTO;
import com.itau.insurance.patrimonial.entrypoint.rest.dto.OpenInsuranceLinksDTO;
import com.itau.insurance.patrimonial.entrypoint.rest.dto.OpenInsuranceMetaDTO;
import com.itau.insurance.patrimonial.entrypoint.rest.dto.OpenInsurancePolicyInfoResponseDTO;
import com.itau.insurance.patrimonial.entrypoint.rest.dto.OpenInsurancePremiumResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class InsurancePatrimonialController {

    private final PolicyApplicationService policyApplicationService;

    @GetMapping("/insurance-patrimonial")
    public ResponseEntity<OpenInsuranceListResponseDTO> getInsurancePatrimonial(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @RequestHeader(value = "x-fapi-auth-date", required = false) String authDate,
            @RequestHeader(value = "x-fapi-customer-ip-address", required = false) String customerIpAddress,
            @RequestHeader(value = "x-customer-user-agent", required = false) String customerUserAgent,
            @RequestHeader(value = "x-v", required = false) String xv,
            @RequestHeader(value = "x-min-v", required = false) String xminv,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page-size", defaultValue = "25") int pageSize) {

        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        var policies = policyApplicationService.getPoliciesPage(safePage - 1, safePageSize, "createdAt", "desc")
                .stream()
                .map(policyApplicationService::toPolicyResponseDTO)
                .map(this::toPolicySummary)
                .toList();

        var company = new OpenInsuranceListResponseDTO.CompanyDTO("Itaú Seguros S.A.", "12345678901234", policies);
        var brand = new OpenInsuranceListResponseDTO.BrandDTO("Itaú Seguros", List.of(company));
        var data = List.of(brand);

        long totalRecords = policyApplicationService.getTotalPolicies();
        long totalPages = (long) Math.ceil((double) totalRecords / safePageSize);
        return ResponseEntity.ok(new OpenInsuranceListResponseDTO(
                data,
                new OpenInsuranceLinksDTO("/insurance-patrimonial", "/insurance-patrimonial", null, null, "/insurance-patrimonial"),
                new OpenInsuranceMetaDTO((int) totalRecords, (int) totalPages)));
    }

    @GetMapping("/insurance-patrimonial/{policyId}/policy-info")
    public ResponseEntity<OpenInsurancePolicyInfoResponseDTO> getPolicyInfo(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @PathVariable String policyId) {

        var policy = policyApplicationService.getPolicyById(policyId)
                .map(policyApplicationService::toPolicyResponseDTO)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));

        var data = new OpenInsurancePolicyInfoResponseDTO.PolicyInfoDTO(
                policy.policyId(),
                policy.policyNumber(),
                policy.coverage(),
                policy.startDate(),
                policy.endDate(),
                List.of(new OpenInsurancePolicyInfoResponseDTO.ProposalDTO(policy.policyId())),
                List.of(new OpenInsurancePolicyInfoResponseDTO.InsuredDTO(policy.customerId()))
        );
        return ResponseEntity.ok(new OpenInsurancePolicyInfoResponseDTO(
                data,
                new OpenInsuranceLinksDTO(
                        "/insurance-patrimonial/" + policyId + "/policy-info",
                        "/insurance-patrimonial/" + policyId + "/policy-info",
                        null,
                        null,
                        "/insurance-patrimonial/" + policyId + "/policy-info"),
                new OpenInsuranceMetaDTO(1, 1)));
    }

    @GetMapping("/insurance-patrimonial/{policyId}/premium")
    public ResponseEntity<OpenInsurancePremiumResponseDTO> getPolicyPremium(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @PathVariable String policyId) {

        var policy = policyApplicationService.getPolicyById(policyId)
                .map(policyApplicationService::toPolicyResponseDTO)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));

        var data = new OpenInsurancePremiumResponseDTO.PremiumDTO(
                1,
                new OpenInsurancePremiumResponseDTO.AmountDTO(policy.premium(), "BRL"),
                List.of(new OpenInsurancePremiumResponseDTO.CoverageDTO(policy.coverage()))
        );
        return ResponseEntity.ok(new OpenInsurancePremiumResponseDTO(
                data,
                new OpenInsuranceLinksDTO(
                        "/insurance-patrimonial/" + policyId + "/premium",
                        "/insurance-patrimonial/" + policyId + "/premium",
                        null,
                        null,
                        "/insurance-patrimonial/" + policyId + "/premium"),
                new OpenInsuranceMetaDTO(1, 1)));
    }

    @GetMapping("/insurance-patrimonial/{policyId}/claim")
    public ResponseEntity<OpenInsuranceClaimResponseDTO> getPolicyClaim(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("x-fapi-interaction-id") String interactionId,
            @PathVariable String policyId) {

        var policy = policyApplicationService.getPolicyById(policyId)
                .map(policyApplicationService::toPolicyResponseDTO)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));

        var claims = List.of(
                new OpenInsuranceClaimResponseDTO.ClaimDTO("CLM-" + policy.policyId(), policy.createdAt(), "OPEN")
        );
        return ResponseEntity.ok(new OpenInsuranceClaimResponseDTO(
                claims,
                new OpenInsuranceLinksDTO(
                        "/insurance-patrimonial/" + policyId + "/claim",
                        "/insurance-patrimonial/" + policyId + "/claim",
                        null,
                        null,
                        "/insurance-patrimonial/" + policyId + "/claim"),
                new OpenInsuranceMetaDTO(claims.size(), 1)));
    }

    private OpenInsuranceListResponseDTO.PolicySummaryDTO toPolicySummary(PolicyResponseDTO policy) {
        return new OpenInsuranceListResponseDTO.PolicySummaryDTO(policy.policyId(), policy.coverage());
    }
}
