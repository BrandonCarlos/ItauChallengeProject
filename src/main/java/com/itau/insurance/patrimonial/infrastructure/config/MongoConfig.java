package com.itau.insurance.patrimonial.infrastructure.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.ConnectionString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Collection;
import java.util.Collections;

/**
 * MongoDB configuration for Spring Data MongoDB 3.x compatibility.
 * Configures MongoDB client settings through the connection string.
 * Connection pool and timeout settings are configured via the MongoDB URI.
 */
@Slf4j
@Configuration
@EnableMongoRepositories(
        basePackages = "com.itau.insurance.patrimonial.infrastructure.persistence.repository"
)
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/patrimonial}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:}")
    private String database;

    @Override
    protected String getDatabaseName() {
        if (database != null && !database.isBlank()) {
            return database;
        }
        String databaseFromUri = new ConnectionString(mongoUri).getDatabase();
        return databaseFromUri != null ? databaseFromUri : "patrimonial";
    }

    @Override
    protected Collection<String> getMappingBasePackages() {
        return Collections.singleton("com.itau.insurance.patrimonial.infrastructure.persistence.document");
    }

    @Override
    public MongoClientSettings mongoClientSettings() {
        log.info("Configuring MongoDB client with database: {}", getDatabaseName());

        return MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .build();
    }
}
