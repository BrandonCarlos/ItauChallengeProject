package com.itau.insurance.patrimonial.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaProducerService service;

    @BeforeEach
    void setUp() {
        service = new KafkaProducerService(kafkaTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(service, "policyEventsTopic", "policy-events");
        ReflectionTestUtils.setField(service, "domainEventsTopic", "domain-events");
        ReflectionTestUtils.setField(service, "enableCompression", true);
    }

    @Test
    void publishPolicyEvent_shouldSendMessageWithHeaders() {
        SendResult<String, String> result = sendResult();
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(result));

        service.publishPolicyEvent("POLICY_CREATED", Map.of("policyId", "p-1"), "corr-1");

        ArgumentCaptor<Message<String>> captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(captor.capture());
        assertEquals("policy-events", captor.getValue().getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals("POLICY_CREATED", captor.getValue().getHeaders().get("event-type"));
        assertEquals("corr-1", captor.getValue().getHeaders().get("correlation-id"));
    }

    @Test
    void publishDomainEvent_shouldSendMessageWithPartitionKey() {
        SendResult<String, String> result = sendResult();
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(result));

        service.publishDomainEvent("DOMAIN_EVENT", Map.of("id", "1"), "partition-1");

        ArgumentCaptor<Message<String>> captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(captor.capture());
        assertEquals("domain-events", captor.getValue().getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals("partition-1", captor.getValue().getHeaders().get("kafka_messageKey"));
    }

    @Test
    void sendMessage_shouldDelegateToKafkaTemplate() {
        SendResult<String, String> result = sendResult();
        when(kafkaTemplate.send("topic-1", "k1", "payload")).thenReturn(CompletableFuture.completedFuture(result));

        service.sendMessage("topic-1", "k1", "payload");

        verify(kafkaTemplate).send("topic-1", "k1", "payload");
    }

    private SendResult<String, String> sendResult() {
        @SuppressWarnings("unchecked")
        SendResult<String, String> result = org.mockito.Mockito.mock(SendResult.class);
        RecordMetadata metadata = org.mockito.Mockito.mock(RecordMetadata.class);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(1L);
        when(result.getRecordMetadata()).thenReturn(metadata);
        return result;
    }
}
