package com.itau.insurance.patrimonial.domain.model;

import com.itau.insurance.patrimonial.domain.exception.InvalidPolicyException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyTest {

    @Test
    void create_shouldBuildActivePolicy() {
        Policy policy = Policy.create("cust-1", "HOME", 1000L, LocalDate.now(), LocalDate.now().plusDays(30));

        assertNotNull(policy.getPolicyId());
        assertEquals(Policy.PolicyStatus.ACTIVE, policy.getStatus());
    }

    @Test
    void validate_shouldRejectInvalidPolicyNumber() {
        Policy policy = validPolicy();
        policy.setPolicyNumber(" ");

        assertThrows(InvalidPolicyException.class, policy::validate);
    }

    @Test
    void validate_shouldRejectInvalidCustomer() {
        Policy policy = validPolicy();
        policy.setCustomerId(" ");

        assertThrows(InvalidPolicyException.class, policy::validate);
    }

    @Test
    void validate_shouldRejectInvalidCoverage() {
        Policy policy = validPolicy();
        policy.setCoverage(" ");

        assertThrows(InvalidPolicyException.class, policy::validate);
    }

    @Test
    void validate_shouldRejectInvalidPremium() {
        Policy policy = validPolicy();
        policy.setPremium(0L);

        assertThrows(InvalidPolicyException.class, policy::validate);
    }

    @Test
    void validate_shouldRejectMissingDates() {
        Policy policy = validPolicy();
        policy.setStartDate(null);

        assertThrows(InvalidPolicyException.class, policy::validate);
    }

    @Test
    void validate_shouldRejectReverseDates() {
        Policy policy = validPolicy();
        policy.setStartDate(LocalDate.now().plusDays(1));
        policy.setEndDate(LocalDate.now());

        assertThrows(InvalidPolicyException.class, policy::validate);
    }

    @Test
    void validate_shouldRejectMissingStatus() {
        Policy policy = validPolicy();
        policy.setStatus(null);

        assertThrows(InvalidPolicyException.class, policy::validate);
    }

    @Test
    void cancelSuspendReactivateAndUpdatePremium_shouldChangeState() {
        Policy policy = validPolicy();
        policy.suspend();
        assertEquals(Policy.PolicyStatus.SUSPENDED, policy.getStatus());
        policy.reactivate();
        assertEquals(Policy.PolicyStatus.ACTIVE, policy.getStatus());
        policy.cancel();
        assertEquals(Policy.PolicyStatus.CANCELLED, policy.getStatus());
        policy.updatePremium(1500L);
        assertEquals(1500L, policy.getPremium());
    }

    private Policy validPolicy() {
        return Policy.builder()
                .policyNumber("POL-1")
                .customerId("cust-1")
                .coverage("HOME")
                .premium(1000L)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .status(Policy.PolicyStatus.ACTIVE)
                .build();
    }
}
