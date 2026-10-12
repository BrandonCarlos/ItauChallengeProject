package com.itau.insurance.patrimonial.domain.exception;

/**
 * Exception thrown when a duplicate policy identifier is detected.
 */
public class DuplicatePolicyException extends DomainException {

    private static final String MESSAGE = "Policy with identifier already exists: %s";
    private final String policyIdentifier;

    public DuplicatePolicyException(String policyIdentifier) {
        super(String.format(MESSAGE, policyIdentifier));
        this.policyIdentifier = policyIdentifier;
    }

    public String getPolicyIdentifier() {
        return policyIdentifier;
    }
}
