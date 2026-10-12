package com.itau.insurance.patrimonial.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    @Qualifier("idempotencyRedisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.idempotency.ttl-minutes:1440}")
    private Long idempotencyTtlMinutes;

    @Value("${app.idempotency.key-prefix:idempotency:}")
    private String keyPrefix;

    public Optional<IdempotencyRequest> getRequest(String idempotencyKey) {
        try {
            String redisKey = buildKey(idempotencyKey);
            log.debug("Retrieving idempotency request with key: {}", redisKey);
            Object storedValue = redisTemplate.opsForValue().get(redisKey);
            IdempotencyRequest request = toIdempotencyRequest(storedValue);

            if (request != null) {
                if (request.isExpired(LocalDateTime.now())) {
                    log.warn("Idempotency key expired: {}", idempotencyKey);
                    deleteRequest(idempotencyKey);
                    return Optional.empty();
                }
                log.info("Idempotency request found for key: {} with status: {}", idempotencyKey, request.status());
                return Optional.of(request);
            }

            log.debug("No idempotency request found for key: {}", idempotencyKey);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving idempotency request for key: {}", idempotencyKey, e);
            return Optional.empty();
        }
    }

    public void saveRequest(String idempotencyKey, IdempotencyRequest request) {
        try {
            String redisKey = buildKey(idempotencyKey);
            log.debug("Saving idempotency request with key: {}", redisKey);

            LocalDateTime now = LocalDateTime.now();
            IdempotencyRequest requestToPersist = copyWith(
                    request,
                    idempotencyKey,
                    IdempotencyRequest.RequestStatus.PENDING.getValue(),
                    request.responseStatus(),
                    request.responseBody(),
                    request.errorMessage(),
                    request.processedAt(),
                    request.createdAt() != null ? request.createdAt() : now,
                    now.plusMinutes(idempotencyTtlMinutes)
            );

            redisTemplate.opsForValue().set(redisKey, requestToPersist, idempotencyTtlMinutes, TimeUnit.MINUTES);
            log.info("Idempotency request saved for key: {} with TTL: {} minutes", idempotencyKey, idempotencyTtlMinutes);
        } catch (Exception e) {
            log.error("Error saving idempotency request for key: {}", idempotencyKey, e);
            throw new RuntimeException("Failed to save idempotency request", e);
        }
    }

    public void markAsProcessing(String idempotencyKey) {
        try {
            Optional<IdempotencyRequest> existing = getRequest(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRequest request = existing.get();
                updateRequest(
                        idempotencyKey,
                        request,
                        IdempotencyRequest.RequestStatus.PROCESSING.getValue(),
                        request.responseStatus(),
                        request.responseBody(),
                        request.errorMessage(),
                        null
                );
                log.info("Marked idempotency key as PROCESSING: {}", idempotencyKey);
            }
        } catch (Exception e) {
            log.error("Error marking idempotency key as processing: {}", idempotencyKey, e);
        }
    }

    public void markAsCompleted(String idempotencyKey, Integer responseStatus, String responseBody) {
        try {
            Optional<IdempotencyRequest> existing = getRequest(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRequest request = existing.get();
                updateRequest(
                        idempotencyKey,
                        request,
                        IdempotencyRequest.RequestStatus.COMPLETED.getValue(),
                        responseStatus,
                        responseBody,
                        request.errorMessage(),
                        LocalDateTime.now()
                );
                log.info("Marked idempotency key as COMPLETED: {}", idempotencyKey);
            }
        } catch (Exception e) {
            log.error("Error marking idempotency key as completed: {}", idempotencyKey, e);
        }
    }

    public void markAsFailed(String idempotencyKey, String errorMessage) {
        try {
            Optional<IdempotencyRequest> existing = getRequest(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRequest request = existing.get();
                updateRequest(
                        idempotencyKey,
                        request,
                        IdempotencyRequest.RequestStatus.FAILED.getValue(),
                        request.responseStatus(),
                        request.responseBody(),
                        errorMessage,
                        LocalDateTime.now()
                );
                log.warn("Marked idempotency key as FAILED: {} - Error: {}", idempotencyKey, errorMessage);
            }
        } catch (Exception e) {
            log.error("Error marking idempotency key as failed: {}", idempotencyKey, e);
        }
    }

    public void deleteRequest(String idempotencyKey) {
        try {
            String redisKey = buildKey(idempotencyKey);
            Boolean deleted = redisTemplate.delete(redisKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Idempotency request deleted for key: {}", idempotencyKey);
            } else {
                log.debug("No idempotency request found to delete for key: {}", idempotencyKey);
            }
        } catch (Exception e) {
            log.error("Error deleting idempotency request for key: {}", idempotencyKey, e);
        }
    }

    public boolean exists(String idempotencyKey) {
        try {
            String redisKey = buildKey(idempotencyKey);
            Boolean exists = redisTemplate.hasKey(redisKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking existence of idempotency key: {}", idempotencyKey, e);
            return false;
        }
    }

    public void refresh(String idempotencyKey) {
        try {
            Optional<IdempotencyRequest> existing = getRequest(idempotencyKey);
            if (existing.isPresent()) {
                IdempotencyRequest request = existing.get();
                IdempotencyRequest refreshed = copyWith(
                        request,
                        idempotencyKey,
                        request.status(),
                        request.responseStatus(),
                        request.responseBody(),
                        request.errorMessage(),
                        request.processedAt(),
                        request.createdAt(),
                        LocalDateTime.now().plusMinutes(idempotencyTtlMinutes)
                );
                String redisKey = buildKey(idempotencyKey);
                redisTemplate.opsForValue().set(redisKey, refreshed, idempotencyTtlMinutes, TimeUnit.MINUTES);
                redisTemplate.expire(redisKey, idempotencyTtlMinutes, TimeUnit.MINUTES);
                log.debug("Refreshed idempotency key TTL: {}", idempotencyKey);
            }
        } catch (Exception e) {
            log.error("Error refreshing idempotency key: {}", idempotencyKey, e);
        }
    }

    private String buildKey(String idempotencyKey) {
        return keyPrefix + idempotencyKey;
    }

    private void updateRequest(
            String idempotencyKey,
            IdempotencyRequest original,
            String status,
            Integer responseStatus,
            String responseBody,
            String errorMessage,
            LocalDateTime processedAt) {

        String redisKey = buildKey(idempotencyKey);
        LocalDateTime now = LocalDateTime.now();
        IdempotencyRequest updated = copyWith(
                original,
                idempotencyKey,
                status,
                responseStatus,
                responseBody,
                errorMessage,
                processedAt,
                original.createdAt() != null ? original.createdAt() : now,
                now.plusMinutes(idempotencyTtlMinutes)
        );
        redisTemplate.opsForValue().set(redisKey, updated, idempotencyTtlMinutes, TimeUnit.MINUTES);
    }

    private IdempotencyRequest copyWith(
            IdempotencyRequest original,
            String idempotencyKey,
            String status,
            Integer responseStatus,
            String responseBody,
            String errorMessage,
            LocalDateTime processedAt,
            LocalDateTime createdAt,
            LocalDateTime expiredAt) {

        return new IdempotencyRequest(
                idempotencyKey,
                original.requestMethod(),
                original.requestPath(),
                original.requestHeaders(),
                original.requestBody(),
                original.requestId(),
                original.correlationId(),
                responseStatus,
                responseBody,
                original.responseHeaders(),
                errorMessage,
                status,
                createdAt,
                processedAt,
                expiredAt,
                original.metadata(),
                original.userId(),
                original.tenantId(),
                original.idempotencyKeyVersion()
        );
    }

    private IdempotencyRequest toIdempotencyRequest(Object storedValue) {
        if (storedValue == null) {
            return null;
        }
        if (storedValue instanceof IdempotencyRequest request) {
            return request;
        }
        if (storedValue instanceof Map<?, ?> mapValue) {
            return objectMapper.convertValue(mapValue, IdempotencyRequest.class);
        }
        throw new IllegalStateException("Unsupported idempotency payload type: " + storedValue.getClass().getName());
    }
}
