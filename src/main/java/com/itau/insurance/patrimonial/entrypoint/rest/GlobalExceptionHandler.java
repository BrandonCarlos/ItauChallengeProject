package com.itau.insurance.patrimonial.entrypoint.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.itau.insurance.patrimonial.domain.exception.DomainException;
import com.itau.insurance.patrimonial.domain.exception.DuplicatePolicyException;
import com.itau.insurance.patrimonial.domain.exception.IdempotencyKeyConflictException;
import com.itau.insurance.patrimonial.domain.exception.InvalidPolicyException;
import com.itau.insurance.patrimonial.domain.exception.PolicyNotFoundException;
import com.itau.insurance.patrimonial.entrypoint.rest.dto.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers.
 * This handler translates domain exceptions and application errors into appropriate
 * HTTP responses, following REST conventions and providing consistent error formatting.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles PolicyNotFoundException
     * 
     * @param ex the exception
     * @return error response with 404 status
     */
    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handlePolicyNotFound(PolicyNotFoundException ex) {
        log.warn("Policy not found: {}", ex.getMessage());

        var errorResponse = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                "POLICY_NOT_FOUND",
                null,
                null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles InvalidPolicyException
     * 
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(InvalidPolicyException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidPolicy(InvalidPolicyException ex) {
        log.warn("Invalid policy: {}", ex.getMessage());

        var errorResponse = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                "INVALID_POLICY",
                null,
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles IdempotencyKeyConflictException
     * 
     * @param ex the exception
     * @return error response with 409 status
     */
    @ExceptionHandler(IdempotencyKeyConflictException.class)
    public ResponseEntity<ErrorResponseDTO> handleIdempotencyKeyConflict(
            IdempotencyKeyConflictException ex) {
        
        log.warn("Idempotency key conflict: {} - Existing resource: {}", 
                ex.getIdempotencyKey(), ex.getExistingResourceId());

        var errorResponse = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                "IDEMPOTENCY_KEY_CONFLICT",
                Map.of(
                        "idempotencyKey", ex.getIdempotencyKey(),
                        "existingResourceId", ex.getExistingResourceId()
                ),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles DuplicatePolicyException
     *
     * @param ex the exception
     * @return error response with 409 status
     */
    @ExceptionHandler(DuplicatePolicyException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicatePolicy(DuplicatePolicyException ex) {
        log.warn("Duplicate policy identifier: {}", ex.getPolicyIdentifier());

        var errorResponse = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                "DUPLICATE_POLICY_IDENTIFIER",
                Map.of("policyIdentifier", ex.getPolicyIdentifier()),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles generic DomainException
     * 
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponseDTO> handleDomainException(DomainException ex) {
        log.error("Domain exception occurred: {}", ex.getMessage(), ex);

        var errorResponse = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                "DOMAIN_ERROR",
                null,
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles validation errors
     * 
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        log.warn("Validation error: {}", ex.getMessage());

        var fieldErrors = new HashMap<String, String>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        });

        var errorResponse = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Request validation failed",
                "VALIDATION_ERROR",
                fieldErrors,
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles malformed request bodies such as invalid field formats.
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        log.warn("Invalid request body format: {}", ex.getMessage());
        Map<String, String> details = extractBodyFormatErrors(ex);

        var errorResponse = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Invalid request body format",
                "INVALID_REQUEST_BODY",
                details.isEmpty() ? null : details,
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    private Map<String, String> extractBodyFormatErrors(HttpMessageNotReadableException ex) {
        Map<String, String> details = new HashMap<>();
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof InvalidFormatException ife) {
                String field = resolveFieldPath(ife);
                String message = buildInvalidFormatMessage(ife);
                details.put(field, message);
                break;
            }
            cause = cause.getCause();
        }
        return details;
    }

    private String resolveFieldPath(InvalidFormatException ex) {
        if (ex.getPath() == null || ex.getPath().isEmpty()) {
            return "requestBody";
        }
        JsonMappingException.Reference last = ex.getPath().get(ex.getPath().size() - 1);
        return last.getFieldName() != null ? last.getFieldName() : "requestBody";
    }

    private String buildInvalidFormatMessage(InvalidFormatException ex) {
        Class<?> targetType = ex.getTargetType();
        if (LocalDate.class.equals(targetType)) {
            return "Invalid date format. Expected yyyy-MM-dd";
        }
        if (LocalDateTime.class.equals(targetType)) {
            return "Invalid datetime format. Expected yyyy-MM-dd'T'HH:mm:ss";
        }
        return "Invalid value format";
    }

    /**
     * Handles all other exceptions
     * 
     * @param ex the exception
     * @return error response with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        var errorResponse = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                "INTERNAL_ERROR",
                null,
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
