package com.itau.insurance.patrimonial.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itau.insurance.patrimonial.infrastructure.persistence.adapter.PolicyRepositoryAdapter;
import com.itau.insurance.patrimonial.infrastructure.persistence.document.OutboxEventDocument;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService outboxExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${app.kafka.topic.policy-events:policy-events}")
    private String policyEventsTopic;

    @Value("${app.outbox.max-retries:3}")
    private Integer maxRetries;

    @Value("${app.outbox.retry-delay-minutes:5}")
    private Integer retryDelayMinutes;

    @Value("${app.outbox.batch-size:100}")
    private Integer batchSize;

    @Transactional
    @Timed(value = "insurance.outbox.publisher", extraTags = {"operation", "saveOutboxEvent"})
    public void saveOutboxEvent(OutboxEventDocument event) {
        try {
            if (event.getId() == null) {
                event.setId(UUID.randomUUID().toString());
            }
            event.setCreatedAt(LocalDateTime.now());
            event.setStatus(OutboxEventDocument.EventStatus.PENDING.getValue());
            event.setFailedAttemptCount(0);
            event.setRetryCount(0);

            mongoTemplate.save(event, "outbox_events");
            log.info("Outbox event saved: id={}, aggregateId={}, eventType={}", 
                    event.getId(), event.getAggregateId(), event.getEventType());
        } catch (Exception e) {
            log.error("Error saving outbox event: aggregateId={}", event.getAggregateId(), e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }

    @Timed(value = "insurance.outbox.publisher", extraTags = {"operation", "publishOutboxEvent"})
    public void publishOutboxEvent(OutboxEventDocument event) {
        try {
            log.debug("Publishing outbox event: id={}, eventType={}", event.getId(), event.getEventType());

            String payload = objectMapper.writeValueAsString(event.getEventData());
            Message<String> message = MessageBuilder.withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, policyEventsTopic)
                    .setHeader(KafkaHeaders.KEY, event.getPartitionKey() != null ? event.getPartitionKey() : event.getAggregateId())
                    .setHeader("event-type", event.getEventType())
                    .setHeader("aggregate-id", event.getAggregateId())
                    .setHeader("aggregate-type", event.getAggregateType())
                    .setHeader("message-id", event.getId())
                    .setHeader("correlation-id", event.getCorrelationId())
                    .setHeader("causation-id", event.getCausationId())
                    .setHeader("event-timestamp", System.currentTimeMillis())
                    .setHeader("source", event.getSourceSystem())
                    .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex == null) {
                    markEventAsPublished(event.getId());
                    log.info("Outbox event published successfully: id={}, topic={}, partition={}", 
                            event.getId(), policyEventsTopic, result.getRecordMetadata().partition());
                } else {
                    markEventAsFailed(event.getId(), new Exception(ex));
                    log.error("Failed to publish outbox event: id={}", event.getId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing outbox event: id={}", event.getId(), e);
            markEventAsFailed(event.getId(), e);
        }
    }

    @Scheduled(fixedRateString = "${app.outbox.poll-interval-ms:30000}")
    @Timed(value = "insurance.outbox.publisher", extraTags = {"operation", "pollAndPublishPendingEvents"})
    public void pollAndPublishPendingEvents() {
        try {
            log.debug("Polling for pending outbox events");

            Query query = new Query()
                    .addCriteria(Criteria.where("status").is(OutboxEventDocument.EventStatus.PENDING.getValue()))
                    .addCriteria(Criteria.where("deleted").is(false))
                    .limit(batchSize);

            List<OutboxEventDocument> pendingEvents = mongoTemplate.find(query, OutboxEventDocument.class, "outbox_events");

            if (!pendingEvents.isEmpty()) {
                log.info("Found {} pending outbox events for publishing", pendingEvents.size());
                pendingEvents.forEach(event -> outboxExecutor.submit(() -> publishOutboxEvent(event)));
            } else {
                log.debug("No pending outbox events found");
            }
        } catch (Exception e) {
            log.error("Error polling pending outbox events", e);
        }
    }

    @Scheduled(fixedRateString = "${app.outbox.retry-interval-ms:60000}")
    @Timed(value = "insurance.outbox.publisher", extraTags = {"operation", "retryFailedEvents"})
    public void retryFailedEvents() {
        try {
            log.debug("Retrying failed outbox events");

            LocalDateTime retryThreshold = LocalDateTime.now().minusMinutes(retryDelayMinutes);

            Query query = new Query()
                    .addCriteria(Criteria.where("status").is(OutboxEventDocument.EventStatus.FAILED.getValue()))
                    .addCriteria(Criteria.where("retryCount").lt(maxRetries))
                    .addCriteria(Criteria.where("lastErrorAt").lte(retryThreshold))
                    .addCriteria(Criteria.where("deleted").is(false))
                    .limit(batchSize);

            List<OutboxEventDocument> failedEvents = mongoTemplate.find(query, OutboxEventDocument.class, "outbox_events");

            if (!failedEvents.isEmpty()) {
                log.info("Found {} failed outbox events for retry", failedEvents.size());
                failedEvents.forEach(event -> {
                    event.setRetryCount(event.getRetryCount() + 1);
                    event.setStatus(OutboxEventDocument.EventStatus.PENDING.getValue());
                    mongoTemplate.save(event, "outbox_events");
                    outboxExecutor.submit(() -> publishOutboxEvent(event));
                });
            } else {
                log.debug("No failed outbox events to retry");
            }
        } catch (Exception e) {
            log.error("Error retrying failed outbox events", e);
        }
    }

    private void markEventAsPublished(String eventId) {
        try {
            Query query = new Query().addCriteria(Criteria.where("_id").is(eventId));
            OutboxEventDocument event = mongoTemplate.findOne(query, OutboxEventDocument.class, "outbox_events");

            if (event != null) {
                event.setStatus(OutboxEventDocument.EventStatus.PUBLISHED.getValue());
                event.setPublishedAt(LocalDateTime.now());
                mongoTemplate.save(event, "outbox_events");
                log.debug("Event marked as published: id={}", eventId);
            }
        } catch (Exception e) {
            log.error("Error marking event as published: id={}", eventId, e);
        }
    }

    private void markEventAsFailed(String eventId, Exception ex) {
        try {
            Query query = new Query().addCriteria(Criteria.where("_id").is(eventId));
            OutboxEventDocument event = mongoTemplate.findOne(query, OutboxEventDocument.class, "outbox_events");

            if (event != null) {
                event.setFailedAttemptCount(event.getFailedAttemptCount() + 1);
                event.setLastErrorMessage(ex.getMessage());
                event.setLastErrorAt(LocalDateTime.now());

                if (event.getFailedAttemptCount() >= maxRetries) {
                    event.setStatus(OutboxEventDocument.EventStatus.FAILED.getValue());
                    log.warn("Event marked as permanently failed after {} attempts: id={}", 
                            event.getFailedAttemptCount(), eventId);
                } else {
                    event.setStatus(OutboxEventDocument.EventStatus.PENDING.getValue());
                    event.setNextRetryAt(LocalDateTime.now().plusMinutes(retryDelayMinutes));
                }

                mongoTemplate.save(event, "outbox_events");
            }
        } catch (Exception e) {
            log.error("Error marking event as failed: id={}", eventId, e);
        }
    }

    @Scheduled(fixedRateString = "${app.outbox.cleanup-interval-hours:3600000}")
    @Timed(value = "insurance.outbox.publisher", extraTags = {"operation", "cleanupArchivedEvents"})
    public void cleanupArchivedEvents() {
        try {
            log.debug("Cleaning up archived outbox events");

            LocalDateTime archiveThreshold = LocalDateTime.now().minusDays(7);

            Query query = new Query()
                    .addCriteria(Criteria.where("status").is(OutboxEventDocument.EventStatus.PUBLISHED.getValue()))
                    .addCriteria(Criteria.where("publishedAt").lt(archiveThreshold));

            long deletedCount = mongoTemplate.remove(query, "outbox_events").getDeletedCount();
            log.info("Cleaned up {} archived outbox events", deletedCount);
        } catch (Exception e) {
            log.error("Error cleaning up archived outbox events", e);
        }
    }
}
