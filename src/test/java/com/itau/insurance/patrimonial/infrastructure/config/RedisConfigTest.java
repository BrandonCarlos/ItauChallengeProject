package com.itau.insurance.patrimonial.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RedisConfigTest {

    @Test
    void redisTemplates_shouldBeConfiguredWithExpectedSerializers() {
        RedisConfig config = new RedisConfig();
        ReflectionTestUtils.setField(config, "redisHost", "localhost");
        ReflectionTestUtils.setField(config, "redisPort", 6379);
        ReflectionTestUtils.setField(config, "redisPassword", "");
        ReflectionTestUtils.setField(config, "redisDatabase", 0);
        ReflectionTestUtils.setField(config, "redisTimeout", Duration.ofSeconds(2));
        ReflectionTestUtils.setField(config, "maxActive", 8);
        ReflectionTestUtils.setField(config, "maxIdle", 8);
        ReflectionTestUtils.setField(config, "minIdle", 0);
        ReflectionTestUtils.setField(config, "maxWait", -1L);

        RedisConnectionFactory factory = config.redisConnectionFactory();
        RedisTemplate<String, Object> genericTemplate = config.redisTemplate(factory);
        RedisTemplate<String, Object> idempotencyTemplate = config.idempotencyRedisTemplate(factory);

        assertInstanceOf(LettuceConnectionFactory.class, factory);
        assertNotNull(genericTemplate);
        assertNotNull(idempotencyTemplate);
        assertInstanceOf(StringRedisSerializer.class, genericTemplate.getKeySerializer());
        assertInstanceOf(GenericJackson2JsonRedisSerializer.class, genericTemplate.getValueSerializer());
        assertInstanceOf(StringRedisSerializer.class, idempotencyTemplate.getHashKeySerializer());
    }
}
