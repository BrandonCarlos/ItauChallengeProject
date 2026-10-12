package com.itau.insurance.patrimonial.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MongoConfigTest {

    @Test
    void getDatabaseName_shouldUseDatabaseFromUriWhenExplicitDatabaseIsBlank() {
        TestMongoConfig config = new TestMongoConfig();
        ReflectionTestUtils.setField(config, "mongoUri", "mongodb://localhost:27017/patrimonial");
        ReflectionTestUtils.setField(config, "database", "");

        assertEquals("patrimonial", config.exposedDatabaseName());
        assertNotNull(config.mongoClientSettings());
    }

    @Test
    void getDatabaseName_shouldPreferExplicitDatabase() {
        TestMongoConfig config = new TestMongoConfig();
        ReflectionTestUtils.setField(config, "mongoUri", "mongodb://localhost:27017/ignored");
        ReflectionTestUtils.setField(config, "database", "customdb");

        assertEquals("customdb", config.exposedDatabaseName());
    }

    static class TestMongoConfig extends MongoConfig {
        String exposedDatabaseName() {
            return getDatabaseName();
        }
    }
}
