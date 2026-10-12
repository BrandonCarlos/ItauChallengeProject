package com.itau.insurance.patrimonial.entrypoint.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.itau.insurance.patrimonial.domain.exception.DuplicatePolicyException;
import com.itau.insurance.patrimonial.domain.exception.IdempotencyKeyConflictException;
import com.itau.insurance.patrimonial.domain.exception.InvalidPolicyException;
import com.itau.insurance.patrimonial.domain.exception.PolicyNotFoundException;
import com.itau.insurance.patrimonial.entrypoint.rest.dto.ErrorResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlePolicyNotFound_shouldReturn404() {
        ErrorResponseDTO response = handler.handlePolicyNotFound(new PolicyNotFoundException("p-1")).getBody();

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND.value(), response.status());
        assertEquals("POLICY_NOT_FOUND", response.code());
    }

    @Test
    void handleInvalidPolicy_shouldReturn400() {
        ErrorResponseDTO response = handler.handleInvalidPolicy(new InvalidPolicyException("invalid")).getBody();

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.status());
        assertEquals("INVALID_POLICY", response.code());
    }

    @Test
    void handleIdempotencyKeyConflict_shouldReturn409() {
        ErrorResponseDTO response = handler.handleIdempotencyKeyConflict(
                new IdempotencyKeyConflictException("idem-1", "p-1")).getBody();

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT.value(), response.status());
        assertEquals("IDEMPOTENCY_KEY_CONFLICT", response.code());
        assertEquals("idem-1", response.details().get("idempotencyKey"));
    }

    @Test
    void handleDuplicatePolicy_shouldReturn409() {
        ErrorResponseDTO response = handler.handleDuplicatePolicy(new DuplicatePolicyException("POL-1")).getBody();

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT.value(), response.status());
        assertEquals("DUPLICATE_POLICY_IDENTIFIER", response.code());
    }

    @Test
    void handleDomainException_shouldReturn400() {
        ErrorResponseDTO response = handler.handleDomainException(new InvalidPolicyException("domain")).getBody();

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.status());
        assertEquals("DOMAIN_ERROR", response.code());
    }

    @Test
    void handleValidationException_shouldReturnFieldErrors() throws Exception {
        Method method = Dummy.class.getMethod("dummy", Object.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dummy");
        bindingResult.addError(new FieldError("dummy", "policyNumber", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ErrorResponseDTO response = handler.handleValidationException(ex).getBody();

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.status());
        assertEquals("VALIDATION_ERROR", response.code());
        assertEquals("must not be blank", response.details().get("policyNumber"));
    }

    @Test
    void handleUnreadableMessage_shouldReturnFieldDetails() {
        InvalidFormatException cause = InvalidFormatException.from(
                (JsonParser) null,
                "Invalid date",
                "11-10-2026",
                LocalDate.class);
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad", cause, (HttpInputMessage) null);

        ErrorResponseDTO response = handler.handleUnreadableMessage(ex).getBody();

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.status());
        assertEquals("INVALID_REQUEST_BODY", response.code());
        assertNotNull(response.details());
        assertTrue(response.details().containsKey("requestBody") || response.details().containsKey("startDate"));
    }

    @Test
    void handleGenericException_shouldReturn500() {
        ErrorResponseDTO response = handler.handleGenericException(new RuntimeException("boom")).getBody();

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.status());
        assertEquals("INTERNAL_ERROR", response.code());
    }

    static class Dummy {
        public void dummy(Object value) {
        }
    }
}
