package com.itau.insurance.patrimonial.domain.exception;

/**
 * Exception thrown when a Policy violates domain business rules or constraints.
 * This exception represents validation failures at the domain level.
 */
public class InvalidPolicyException extends DomainException {

    private static final String MESSAGE = "Invalid policy: %s";

    public InvalidPolicyException(String reason) {
        super(String.format(MESSAGE, reason));
    }

    public InvalidPolicyException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InvalidPolicyException ofInvalidPremium(Long premium) {
        return new InvalidPolicyException("Premium must be positive, received: " + premium);
    }

    public static InvalidPolicyException ofInvalidCoverage(String coverage) {
        return new InvalidPolicyException("Invalid coverage type: " + coverage);
    }

    public static InvalidPolicyException ofInvalidCustomer(String customerId) {
        return new InvalidPolicyException("Invalid customer ID: " + customerId);
    }
}
