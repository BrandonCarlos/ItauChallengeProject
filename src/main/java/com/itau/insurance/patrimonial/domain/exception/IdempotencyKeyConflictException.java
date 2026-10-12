package com.itau.insurance.patrimonial.domain.exception;

/**
 * Exception thrown when an idempotency key conflict occurs.
 * This prevents duplicate operations when the same request is sent multiple times.
 */
public class IdempotencyKeyConflictException extends DomainException {

    private static final String MESSAGE = "Idempotency key conflict: %s";
    private final String idempotencyKey;
    private final String existingResourceId;

    public IdempotencyKeyConflictException(String idempotencyKey, String existingResourceId) {
        super(String.format(MESSAGE, idempotencyKey));
        this.idempotencyKey = idempotencyKey;
        this.existingResourceId = existingResourceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getExistingResourceId() {
        return existingResourceId;
    }
}
