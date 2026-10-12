package com.itau.insurance.patrimonial.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(redisTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(service, "idempotencyTtlMinutes", 60L);
        ReflectionTestUtils.setField(service, "keyPrefix", "idempotency:");
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
        lenient().when(redisTemplate.hasKey(anyString())).thenReturn(true);
    }

    @Test
    void saveRequest_shouldPersistWithPrefixAndTtl() {
        IdempotencyRequest request = requestWithStatus("PENDING");

        service.saveRequest("key-1", request);

        ArgumentCaptor<IdempotencyRequest> captor = ArgumentCaptor.forClass(IdempotencyRequest.class);
        verify(valueOperations).set(anyString(), captor.capture(), anyLong(), any(TimeUnit.class));
        assertEquals("key-1", captor.getValue().idempotencyKey());
        assertEquals("PENDING", captor.getValue().status());
    }

    @Test
    void getRequest_shouldReturnRequestWhenPresent() {
        IdempotencyRequest request = requestWithStatus("COMPLETED");
        when(valueOperations.get("idempotency:key-1")).thenReturn(request);

        Optional<IdempotencyRequest> result = service.getRequest("key-1");

        assertTrue(result.isPresent());
        assertEquals("COMPLETED", result.get().status());
    }

    @Test
    void getRequest_shouldReturnEmptyWhenExpired() {
        IdempotencyRequest request = new IdempotencyRequest(
                "key-1",
                "POST",
                "/api",
                Map.of(),
                "{}",
                "rid",
                "cid",
                201,
                "{}",
                Map.of(),
                null,
                "COMPLETED",
                LocalDateTime.now().minusHours(2),
                null,
                LocalDateTime.now().minusMinutes(1),
                Map.of(),
                null,
                null,
                1
        );
        when(valueOperations.get("idempotency:key-1")).thenReturn(request);

        Optional<IdempotencyRequest> result = service.getRequest("key-1");

        assertFalse(result.isPresent());
        verify(redisTemplate).delete("idempotency:key-1");
    }

    @Test
    void markAsCompleted_shouldUpdateStoredRequest() {
        IdempotencyRequest request = requestWithStatus("PROCESSING");
        when(valueOperations.get("idempotency:key-1")).thenReturn(request);

        service.markAsCompleted("key-1", 201, "{\"ok\":true}");

        verify(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void markAsFailed_shouldUpdateStoredRequest() {
        IdempotencyRequest request = requestWithStatus("PROCESSING");
        when(valueOperations.get("idempotency:key-1")).thenReturn(request);

        service.markAsFailed("key-1", "boom");

        verify(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void exists_shouldDelegateToRedis() {
        assertTrue(service.exists("key-1"));
    }

    @Test
    void deleteRequest_shouldCallRedisDelete() {
        service.deleteRequest("key-1");

        verify(redisTemplate).delete("idempotency:key-1");
    }

    @Test
    void refresh_shouldExtendTtl() {
        IdempotencyRequest request = requestWithStatus("COMPLETED");
        when(valueOperations.get("idempotency:key-1")).thenReturn(request);

        service.refresh("key-1");

        verify(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    private IdempotencyRequest requestWithStatus(String status) {
        return new IdempotencyRequest(
                "key-1",
                "POST",
                "/api",
                Map.of("h", "v"),
                "{\"a\":1}",
                "rid",
                "cid",
                200,
                "{}",
                Map.of(),
                null,
                status,
                LocalDateTime.now(),
                null,
                LocalDateTime.now().plusHours(1),
                Map.of(),
                null,
                null,
                1
        );
    }
}
