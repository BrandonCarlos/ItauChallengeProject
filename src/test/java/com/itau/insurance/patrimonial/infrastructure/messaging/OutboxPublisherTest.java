package com.itau.insurance.patrimonial.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itau.insurance.patrimonial.infrastructure.persistence.document.OutboxEventDocument;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(mongoTemplate, kafkaTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(publisher, "policyEventsTopic", "policy-events");
        ReflectionTestUtils.setField(publisher, "maxRetries", 3);
        ReflectionTestUtils.setField(publisher, "retryDelayMinutes", 5);
        ReflectionTestUtils.setField(publisher, "batchSize", 100);
    }

    @Test
    void saveOutboxEvent_shouldPopulateDefaultsAndPersist() {
        OutboxEventDocument event = baseEvent();

        publisher.saveOutboxEvent(event);

        ArgumentCaptor<OutboxEventDocument> captor = ArgumentCaptor.forClass(OutboxEventDocument.class);
        verify(mongoTemplate).save(captor.capture(), eq("outbox_events"));
        assertNotNull(captor.getValue().getId());
        assertEquals("PENDING", captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getRetryCount());
    }

    @Test
    void publishOutboxEvent_shouldMarkPublishedOnSuccess() {
        OutboxEventDocument event = baseEvent();
        event.setId("evt-1");
        when(mongoTemplate.findOne(any(Query.class), eq(OutboxEventDocument.class), eq("outbox_events"))).thenReturn(event);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(sendResult()));

        publisher.publishOutboxEvent(event);

        verify(mongoTemplate).save(any(OutboxEventDocument.class), eq("outbox_events"));
    }

    @Test
    void publishOutboxEvent_shouldMarkFailedOnSendError() {
        OutboxEventDocument event = baseEvent();
        event.setId("evt-1");
        when(mongoTemplate.findOne(any(Query.class), eq(OutboxEventDocument.class), eq("outbox_events"))).thenReturn(event);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        publisher.publishOutboxEvent(event);

        verify(mongoTemplate).save(any(OutboxEventDocument.class), eq("outbox_events"));
    }

    private SendResult<String, String> sendResult() {
        ProducerRecord<String, String> record = new ProducerRecord<>("policy-events", "p-1", "{}");
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("policy-events", 0),
                0L,
                0L,
                System.currentTimeMillis(),
                Long.valueOf(0L),
                0,
                0
        );
        return new SendResult<>(record, metadata);
    }

    private OutboxEventDocument baseEvent() {
        return OutboxEventDocument.builder()
                .aggregateId("p-1")
                .aggregateType("POLICY")
                .eventType("POLICY_CREATED")
                .eventData(Map.of("policyId", "p-1"))
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .partitionKey("p-1")
                .correlationId("corr-1")
                .sourceSystem("patrimonial-api")
                .build();
    }
}
