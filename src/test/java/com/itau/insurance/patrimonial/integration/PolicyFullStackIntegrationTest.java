package com.itau.insurance.patrimonial.integration;

import com.itau.insurance.patrimonial.application.dto.PolicyRequestDTO;
import com.itau.insurance.patrimonial.application.dto.PolicyResponseDTO;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class PolicyFullStackIntegrationTest {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongo.getReplicaSetUrl("patrimonial"));
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.outbox.poll-interval-ms", () -> "1000");
        registry.add("app.outbox.retry-interval-ms", () -> "1000");
        registry.add("app.outbox.retry-delay-minutes", () -> "0");
        registry.add("app.outbox.max-retries", () -> "5");
    }

    @Test
    void shouldExecuteFullFlowWithRealMongoRedisKafka() {
        String idempotencyKey = UUID.randomUUID().toString();
        String customerId = "cust-it-" + UUID.randomUUID();
        PolicyRequestDTO request = new PolicyRequestDTO(
                "POL-INT-" + UUID.randomUUID(),
                customerId,
                "HOME",
                12000L,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(365),
                "Integration test property",
                "Integration Avenue 100",
                null,
                null
        );

        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.setContentType(MediaType.APPLICATION_JSON);
        createHeaders.set("Idempotency-Key", idempotencyKey);
        HttpEntity<PolicyRequestDTO> createEntity = new HttpEntity<>(request, createHeaders);

        ResponseEntity<PolicyResponseDTO> createResponse =
                restTemplate.postForEntity("/api/v1/policies", createEntity, PolicyResponseDTO.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        String policyId = createResponse.getBody().policyId();
        assertNotNull(policyId);

        ResponseEntity<PolicyResponseDTO> idempotentResponse =
                restTemplate.postForEntity("/api/v1/policies", createEntity, PolicyResponseDTO.class);

        assertEquals(HttpStatus.CREATED, idempotentResponse.getStatusCode());
        assertNotNull(idempotentResponse.getBody());
        assertEquals(policyId, idempotentResponse.getBody().policyId());

        HttpHeaders duplicateHeaders = new HttpHeaders();
        duplicateHeaders.setContentType(MediaType.APPLICATION_JSON);
        duplicateHeaders.set("Idempotency-Key", UUID.randomUUID().toString());
        HttpEntity<PolicyRequestDTO> duplicateEntity = new HttpEntity<>(request, duplicateHeaders);
        ResponseEntity<String> duplicateResponse =
                restTemplate.postForEntity("/api/v1/policies", duplicateEntity, String.class);
        assertEquals(HttpStatus.CONFLICT, duplicateResponse.getStatusCode());

        ResponseEntity<PolicyResponseDTO> getResponse =
                restTemplate.getForEntity("/api/v1/policies/" + policyId, PolicyResponseDTO.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(policyId, getResponse.getBody().policyId());

        HttpHeaders openInsuranceHeaders = new HttpHeaders();
        openInsuranceHeaders.set("Authorization", "Bearer test-token");
        openInsuranceHeaders.set("x-fapi-interaction-id", UUID.randomUUID().toString());
        HttpEntity<Void> openInsuranceEntity = new HttpEntity<>(openInsuranceHeaders);

        ResponseEntity<String> policyInfoResponse = restTemplate.exchange(
                "/insurance-patrimonial/" + policyId + "/policy-info",
                HttpMethod.GET,
                openInsuranceEntity,
                String.class
        );
        assertEquals(HttpStatus.OK, policyInfoResponse.getStatusCode());
        assertNotNull(policyInfoResponse.getBody());
        assertTrue(policyInfoResponse.getBody().contains(policyId));

        boolean kafkaConsumed = waitForPolicyEventMessage(customerId, Duration.ofSeconds(30));
        assertTrue(kafkaConsumed, "Expected POLICY_CREATED message was not consumed from Kafka.");
    }

    private boolean waitForPolicyEventMessage(String customerId, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-policy-events-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        long deadline = System.currentTimeMillis() + timeout.toMillis();
        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("policy-events"));
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    if (record.value() != null && record.value().contains("\"customerId\":\"" + customerId + "\"")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
