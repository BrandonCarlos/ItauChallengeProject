package com.itau.insurance.patrimonial.domain.exception;

/**
 * Base exception class for all domain-level exceptions in the Patrimonial Insurance domain.
 * This exception represents business rule violations and domain constraint violations.
 */
public abstract class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public DomainException(Throwable cause) {
        super(cause);
    }
}
