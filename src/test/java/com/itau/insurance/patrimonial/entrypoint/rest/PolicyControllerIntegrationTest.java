package com.itau.insurance.patrimonial.entrypoint.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itau.insurance.patrimonial.application.dto.PolicyRequestDTO;
import com.itau.insurance.patrimonial.application.dto.PolicyResponseDTO;
import com.itau.insurance.patrimonial.application.service.PolicyApplicationService;
import com.itau.insurance.patrimonial.infrastructure.cache.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PolicyController.class, InsurancePatrimonialController.class, GlobalExceptionHandler.class})
@Import(GlobalExceptionHandler.class)
class PolicyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PolicyApplicationService policyApplicationService;
    @MockitoBean
    private IdempotencyService idempotencyService;

    @Test
    void createAndGetPolicyEndpoints_shouldReturnSuccess() throws Exception {
        PolicyResponseDTO saved = new PolicyResponseDTO(
                "pol-123",
                "POL-123",
                "cust-1",
                "RESIDENTIAL",
                1000L,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "ACTIVE",
                "House",
                "Street A",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "idem-123"
        );

        when(idempotencyService.getRequest(any())).thenReturn(Optional.empty());
        when(policyApplicationService.createPolicy(any(), any())).thenReturn(com.itau.insurance.patrimonial.domain.model.Policy.builder()
                .policyId("pol-123")
                .policyNumber("POL-123")
                .customerId("cust-1")
                .coverage("RESIDENTIAL")
                .premium(1000L)
                .status(com.itau.insurance.patrimonial.domain.model.Policy.PolicyStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        when(policyApplicationService.toPolicyResponseDTO(any())).thenReturn(saved);
        when(policyApplicationService.getPolicyById("pol-123", false)).thenReturn(Optional.of(com.itau.insurance.patrimonial.domain.model.Policy.builder()
                .policyId("pol-123")
                .policyNumber("POL-123")
                .customerId("cust-1")
                .coverage("RESIDENTIAL")
                .premium(1000L)
                .status(com.itau.insurance.patrimonial.domain.model.Policy.PolicyStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));

        PolicyRequestDTO request = new PolicyRequestDTO(
                "POL-123",
                "cust-1",
                "RESIDENTIAL",
                1000L,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "House",
                "Street A",
                null,
                "idem-123"
        );

        mockMvc.perform(post("/api/v1/policies")
                        .header("Idempotency-Key", "idem-123")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policyId").value("pol-123"));

        mockMvc.perform(get("/api/v1/policies/pol-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value("pol-123"));
    }

    @Test
    void listPolicies_shouldReturnPaginatedEnvelope() throws Exception {
        when(policyApplicationService.getPoliciesPage(0, 10, "createdAt", "desc", false))
                .thenReturn(List.of(com.itau.insurance.patrimonial.domain.model.Policy.builder()
                        .policyId("pol-1")
                        .policyNumber("POL-1")
                        .customerId("cust-1")
                        .coverage("HOME")
                        .premium(1000L)
                        .status(com.itau.insurance.patrimonial.domain.model.Policy.PolicyStatus.ACTIVE)
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now().plusDays(30))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
        when(policyApplicationService.getTotalPolicies(false)).thenReturn(1L);
        when(policyApplicationService.toPolicyResponseDTO(any())).thenReturn(new PolicyResponseDTO(
                "pol-1",
                "POL-1",
                "cust-1",
                "HOME",
                1000L,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "ACTIVE",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        ));

        mockMvc.perform(get("/api/v1/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getPolicyById_includeDeleted_shouldUseExplicitFlag() throws Exception {
        when(policyApplicationService.getPolicyById("pol-123", true)).thenReturn(Optional.of(com.itau.insurance.patrimonial.domain.model.Policy.builder()
                .policyId("pol-123")
                .policyNumber("POL-123")
                .customerId("cust-1")
                .coverage("RESIDENTIAL")
                .premium(1000L)
                .status(com.itau.insurance.patrimonial.domain.model.Policy.PolicyStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
        when(policyApplicationService.toPolicyResponseDTO(any())).thenReturn(new PolicyResponseDTO(
                "pol-123",
                "POL-123",
                "cust-1",
                "RESIDENTIAL",
                1000L,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "ACTIVE",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        ));

        mockMvc.perform(get("/api/v1/policies/pol-123").param("includeDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value("pol-123"));
    }

    @Test
    void cancelPolicy_shouldReturnNoContent() throws Exception {
        doNothing().when(policyApplicationService).cancelPolicy("pol-123");

        mockMvc.perform(post("/api/v1/policies/pol-123/cancel"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePolicy_shouldReturnNoContent() throws Exception {
        doNothing().when(policyApplicationService).deletePolicy("pol-123", "user-requested");

        mockMvc.perform(delete("/api/v1/policies/pol-123")
                        .param("reason", "user-requested")
                        .header("Idempotency-Key", "idem-delete-123"))
                .andExpect(status().isNoContent());
    }

    @Test
    void insurancePatrimonialEndpoint_shouldReturnOpenInsuranceShape() throws Exception {
        var policy = com.itau.insurance.patrimonial.domain.model.Policy.builder()
                .policyId("pol-oi-1")
                .coverage("RESIDENTIAL")
                .premium(1000L)
                .status(com.itau.insurance.patrimonial.domain.model.Policy.PolicyStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(policyApplicationService.getPoliciesPage(any(Integer.class), any(Integer.class), any(String.class), any(String.class)))
                .thenReturn(List.of(policy));
        when(policyApplicationService.toPolicyResponseDTO(any())).thenReturn(new PolicyResponseDTO(
                "pol-oi-1",
                "POL-OI-1",
                "cust-1",
                "RESIDENTIAL",
                1000L,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "ACTIVE",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        ));
        when(policyApplicationService.getTotalPolicies()).thenReturn(1L);

        mockMvc.perform(get("/insurance-patrimonial")
                        .header("Authorization", "Bearer token")
                        .header("x-fapi-interaction-id", "550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.totalRecords").value(1));
    }
}
