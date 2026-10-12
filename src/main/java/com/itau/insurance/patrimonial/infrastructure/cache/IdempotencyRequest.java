package com.itau.insurance.patrimonial.infrastructure.cache;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

public record IdempotencyRequest(
    @JsonProperty("idempotency_key")
    String idempotencyKey,
    
    @JsonProperty("request_method")
    String requestMethod,
    
    @JsonProperty("request_path")
    String requestPath,
    
    @JsonProperty("request_headers")
    Map<String, String> requestHeaders,
    
    @JsonProperty("request_body")
    String requestBody,
    
    @JsonProperty("request_id")
    String requestId,
    
    @JsonProperty("correlation_id")
    String correlationId,
    
    @JsonProperty("response_status")
    Integer responseStatus,
    
    @JsonProperty("response_body")
    String responseBody,
    
    @JsonProperty("response_headers")
    Map<String, String> responseHeaders,
    
    @JsonProperty("error_message")
    String errorMessage,
    
    @JsonProperty("status")
    String status,
    
    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    
    @JsonProperty("processed_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime processedAt,
    
    @JsonProperty("expired_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime expiredAt,
    
    @JsonProperty("metadata")
    Map<String, Object> metadata,
    
    @JsonProperty("user_id")
    String userId,
    
    @JsonProperty("tenant_id")
    String tenantId,
    
    @JsonProperty("idempotency_key_version")
    Integer idempotencyKeyVersion
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    public IdempotencyRequest {
        if (status == null) {
            status = "PENDING";
        }
        if (idempotencyKeyVersion == null) {
            idempotencyKeyVersion = 1;
        }
    }

    public enum RequestStatus {
        PENDING("PENDING"),
        PROCESSING("PROCESSING"),
        COMPLETED("COMPLETED"),
        FAILED("FAILED"),
        EXPIRED("EXPIRED");

        private final String value;

        RequestStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static RequestStatus fromValue(String value) {
            for (RequestStatus status : RequestStatus.values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid status: " + value);
        }
    }

    public boolean isCompleted() {
        return RequestStatus.COMPLETED.value.equals(this.status);
    }

    public boolean isPending() {
        return RequestStatus.PENDING.value.equals(this.status);
    }

    public boolean isExpired(LocalDateTime now) {
        return expiredAt != null && expiredAt.isBefore(now);
    }
}
