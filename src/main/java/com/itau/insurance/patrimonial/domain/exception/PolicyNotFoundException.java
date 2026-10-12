package com.itau.insurance.patrimonial.domain.exception;

/**
 * Exception thrown when a requested Policy is not found.
 * This exception represents a domain-level error in the Policy bounded context.
 */
public class PolicyNotFoundException extends DomainException {

    private static final String MESSAGE = "Policy not found: %s";

    public PolicyNotFoundException(String policyId) {
        super(String.format(MESSAGE, policyId));
    }

    public PolicyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
