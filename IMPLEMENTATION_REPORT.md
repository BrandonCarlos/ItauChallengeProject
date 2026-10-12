# Itaú Insurance Patrimonial API - Implementation Report

**Project**: Open Insurance Patrimonial API  
**Version**: 1.0.0  
**Date**: 2026-10-12  
**Status**: Complete

---

## Executive Summary

The service is implemented with **Java 25** and **Spring Boot 3.5.16**, following **Clean + Hexagonal Architecture** with:

- MongoDB persistence
- Redis idempotency
- Kafka event streaming
- Transactional outbox for reliable event publication
- REST APIs for Open Insurance reads and policy lifecycle management

---

## Current Runtime and Framework Versions

- **Java**: 25
- **Spring Boot**: 3.5.16
- **Springdoc OpenAPI**: 2.8.9
- **MongoDB**: 7.0
- **Redis**: 7-alpine
- **Kafka / Zookeeper**: Confluent 7.5.0

---

## Implemented API Surface

### Open Insurance Endpoints (read-only)

- `GET /insurance-patrimonial`
- `GET /insurance-patrimonial/{policyId}/policy-info`
- `GET /insurance-patrimonial/{policyId}/premium`
- `GET /insurance-patrimonial/{policyId}/claim`

### Policy Management Endpoints

- `POST /api/v1/policies`
- `GET /api/v1/policies/{policyId}`
- `GET /api/v1/policies`
- `PUT /api/v1/policies/{policyId}`
- `DELETE /api/v1/policies/{policyId}`
- `POST /api/v1/policies/{policyId}/cancel`
- `POST /api/v1/policies/{policyId}/suspend`
- `POST /api/v1/policies/{policyId}/reactivate`
- `PUT /api/v1/policies/{policyId}/premium` (hidden/internal)
- `GET /api/v1/policies/customer/{customerId}` (hidden/internal)

---

## Data and Persistence

### MongoDB

Primary collections:

- `policies`
- `outbox_events`

Model/index highlights:

- Primary key by `_id` (`@Id`)
- Unique sparse indexes on `policy_number` and `idempotency_key`
- Compound/indexes for common query paths (customer/status/date)
- Soft delete strategy via `deleted` flag

Mongo URI resolution:

- `SPRING_DATA_MONGODB_URI` (default: `mongodb://localhost:27017/patrimonial`)

### Redis

Used for idempotency state and cached write responses:

- key pattern: `idempotency:{idempotency-key}`
- status lifecycle includes: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `EXPIRED`
- TTL driven by `app.idempotency.ttl-minutes` (default 1440 / 24h)

---

## Event Architecture

### Kafka Topics in Use

- `policy-events`
- `domain-events`

### Producer/Consumer Components

- Producer path:
  - `PolicyEventPublisherAdapter` creates outbox events
  - `OutboxPublisher` publishes from outbox to Kafka
- Consumer path:
  - `PolicyEventConsumer.consumePolicyEvent(...)`
  - `PolicyEventConsumer.consumeDomainEvent(...)`

### Outbox Reliability Flow

1. Business operation persists policy data.
2. Outbox event is persisted in Mongo (`PENDING`).
3. Scheduled poller publishes pending events.
4. Success marks event as `PUBLISHED`.
5. Failure path tracks attempts and retries.

Relevant runtime properties:

- `app.outbox.enabled=true`
- `app.outbox.poll-interval-ms=5000`
- `app.outbox.batch-size=100`
- Retry controls in code defaults (`max-retries`, retry delays/intervals)

Delivery semantic: **at-least-once** publication behavior.

---

## Configuration Snapshot (current)

From `application.yaml`:

- Kafka bootstrap: `${KAFKA_BOOTSTRAP_SERVERS...}` (default `localhost:9092`)
- Consumer group: `${KAFKA_CONSUMER_GROUP:patrimonial-service}`
- Redis host/port/password via `SPRING_DATA_REDIS_*`
- Mongo auto-index creation enabled
- Actuator health/info/metrics/prometheus exposed
- Swagger UI at `/swagger-ui.html`, OpenAPI JSON at `/v3/api-docs`

---

## Observability and Operations

- Correlation ID logging filter enabled
- Health indicators include Mongo/Redis/Kafka readiness contributors
- Outbox polling, publish, and retry behavior is visible in application logs

Typical healthy event path in logs:

1. `Found N pending outbox events...`
2. `Publishing outbox event...`
3. `Outbox event published successfully...`
4. `Consuming policy event...`

---

## Architecture Notes

- Core business logic depends on ports/interfaces, not infrastructure classes
- Infrastructure (Mongo/Redis/Kafka) is implemented through adapters
- This keeps business rules testable with mocks/fakes and reduces migration impact for infra changes

---

## Known Current Characteristics

- Open Insurance endpoints require contract headers, but OAuth2 resource-server enforcement is not fully wired in this service code.
- Existing environments may contain pre-created collections without all expected indexes; verify index usage with Mongo `explain()` (`IXSCAN` vs `COLLSCAN`).

---

## Final Status

The implementation is aligned with the current repository state and runtime stack:

- Java 25
- Spring Boot 3.5.16
- MongoDB + Redis + Kafka
- Hexagonal architecture
- Transactional outbox + idempotency

