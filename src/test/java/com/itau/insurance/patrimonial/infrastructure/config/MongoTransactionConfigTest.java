package com.itau.insurance.patrimonial.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.TransactionManager;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class MongoTransactionConfigTest {

    @Test
    void transactionManager_shouldReturnMongoTransactionManager() {
        MongoTransactionConfig config = new MongoTransactionConfig();
        MongoDatabaseFactory factory = mock(MongoDatabaseFactory.class);

        TransactionManager manager = config.transactionManager(factory);

        assertInstanceOf(MongoTransactionManager.class, manager);
    }
}
