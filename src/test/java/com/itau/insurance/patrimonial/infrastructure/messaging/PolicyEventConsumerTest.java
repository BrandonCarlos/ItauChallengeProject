package com.itau.insurance.patrimonial.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PolicyEventConsumerTest {

    @Test
    void consumePolicyEvent_shouldHandleValidMessage() {
        PolicyEventConsumer consumer = new PolicyEventConsumer(new ObjectMapper());

        assertDoesNotThrow(() -> consumer.consumePolicyEvent(
                "{\"policyId\":\"p-1\",\"customerId\":\"c-1\"}",
                "POLICY_CREATED",
                "msg-1",
                "corr-1",
                "topic",
                0,
                1L));
    }

    @Test
    void consumePolicyEvent_shouldIgnoreEmptyMessage() {
        PolicyEventConsumer consumer = new PolicyEventConsumer(new ObjectMapper());

        assertDoesNotThrow(() -> consumer.consumePolicyEvent(
                "",
                "POLICY_CREATED",
                "msg-1",
                "corr-1",
                "topic",
                0,
                1L));
    }

    @Test
    void consumeDomainEvent_shouldHandleValidMessage() {
        PolicyEventConsumer consumer = new PolicyEventConsumer(new ObjectMapper());

        assertDoesNotThrow(() -> consumer.consumeDomainEvent(
                "{\"id\":\"1\"}",
                "DOMAIN_CREATED",
                "msg-2",
                "topic"));
    }

    @Test
    void privateHandlers_shouldBeInvokable() throws Exception {
        PolicyEventConsumer consumer = new PolicyEventConsumer(new ObjectMapper());
        invokePrivate(consumer, "handlePolicyCreated", Map.of("policyNumber", "POL-1", "customerId", "C-1"), "corr");
        invokePrivate(consumer, "handlePolicyUpdated", Map.of("policyNumber", "POL-1"), "corr");
        invokePrivate(consumer, "handlePolicyActivated", Map.of("policyNumber", "POL-1", "customerId", "C-1"), "corr");
        invokePrivate(consumer, "handlePolicyCancelled", Map.of("policyNumber", "POL-1", "reason", "r"), "corr");
        invokePrivate(consumer, "handlePolicyRenewed", Map.of("policyNumber", "POL-1"), "corr");
    }

    private void invokePrivate(PolicyEventConsumer consumer, String methodName, Map<String, Object> payload, String correlationId) throws Exception {
        Method method = PolicyEventConsumer.class.getDeclaredMethod(methodName, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(consumer, payload, correlationId);
    }
}
