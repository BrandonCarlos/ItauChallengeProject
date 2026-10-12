# Itaú Insurance Patrimonial API — Architecture

## Overview

This service is a Java 25 / Spring Boot 3.5.16 backend for policy management and Open Insurance read APIs.  
It follows **Clean Architecture + Hexagonal Architecture (Ports & Adapters)** with event-driven integration using **Kafka + Transactional Outbox**.

## Runtime Stack

- Java 25
- Spring Boot 3.5.16
- MongoDB 7 (primary data store)
- Redis 7 (idempotency and cache-like support)
- Kafka 7.5 + Zookeeper (events and asynchronous processing)
- Docker Compose for local orchestration

## Architectural Layers

```text
Entrypoint (REST Controllers)
        |
Application (Use Cases / Orchestration)
        |
Domain (Business Rules + Ports)
        |
Infrastructure Adapters (Mongo, Redis, Kafka, Observability)
```

### 1) Entrypoint Layer

Main classes:

- `entrypoint.rest.PolicyController`
- `entrypoint.rest.InsurancePatrimonialController`
- `entrypoint.rest.GlobalExceptionHandler`

Responsibilities:

- HTTP contract handling
- request validation and mapping
- idempotency header usage for write operations
- response shaping

### 2) Application Layer

Main classes:

- `application.service.PolicyApplicationService`
- `application.service.PolicyEventPublisher` (port)
- `application.adapter.PolicyEventPublisherAdapter` (outbox event creation)
- DTOs/mappers in `application.dto` and `application.mapper`

Responsibilities:

- orchestrate policy lifecycle use cases
- call domain ports (persistence/event publishing)
- enforce application-level flows and consistency

### 3) Domain Layer

Main classes:

- `domain.model.Policy`
- `domain.event.*` (`PolicyCreatedEvent`, `PolicyUpdatedEvent`, `PolicyDeletedEvent`)
- `domain.ports.*` (`PolicyUseCasePort`, `PolicyRepositoryPort`)
- `domain.exception.*`

Responsibilities:

- business rules and invariants
- domain events
- abstractions (ports) that isolate the core from technology

### 4) Infrastructure Layer

Main classes:

- Persistence:
  - `infrastructure.persistence.document.PolicyDocument`
  - `infrastructure.persistence.repository.PolicyMongoRepository`
  - `infrastructure.persistence.adapter.PolicyRepositoryAdapter`
- Idempotency:
  - `infrastructure.cache.IdempotencyService`
  - `infrastructure.cache.IdempotencyRequest`
- Messaging:
  - `infrastructure.messaging.OutboxPublisher`
  - `infrastructure.messaging.PolicyEventConsumer`
  - `infrastructure.messaging.KafkaProducerService`
  - `infrastructure.persistence.document.OutboxEventDocument`
- Config/Observability:
  - `infrastructure.config.*` (`MongoConfig`, `MongoTransactionConfig`, `KafkaConfig`, `RedisConfig`)
  - `infrastructure.observability.*` (`CorrelationIdFilter`, `KafkaHealthIndicator`)

Responsibilities:

- implement ports
- integrate with external systems (Mongo/Redis/Kafka)
- operational concerns (health, logging, correlation IDs)

## Hexagonal Architecture Mapping

### Input Ports (use cases exposed by core)

- `domain.ports.PolicyUseCasePort`

### Output Ports (dependencies required by core)

- `domain.ports.PolicyRepositoryPort`
- `application.service.PolicyEventPublisher`

### Adapters

- Input adapters: REST controllers
- Output adapters: Mongo repository adapter, outbox publisher adapter, Redis idempotency service, Kafka producer/consumer components

## Event-Driven Design

### Domain events emitted by write flows

- `POLICY_CREATED`
- `POLICY_UPDATED`
- `POLICY_DELETED`

These are created in application/domain flow and persisted to outbox before publication.

### Kafka topics currently used

- `policy-events`
- `domain-events`

Consumers:

- `PolicyEventConsumer.consumePolicyEvent(...)`
- `PolicyEventConsumer.consumeDomainEvent(...)`

### Transactional Outbox Flow

1. Write request arrives (e.g., create/update/delete policy).
2. Business data is persisted in MongoDB.
3. Event is persisted in `outbox_events` as `PENDING`.
4. `OutboxPublisher` scheduled poll scans pending events.
5. Publisher sends to Kafka.
6. On success: event marked `PUBLISHED`.
7. On failure: event is tracked and retried; after max retries it can remain `FAILED`.

This provides reliable, decoupled publication with retry semantics (**at-least-once delivery**).

## Data Architecture

### MongoDB collections

- `policies`
- `outbox_events`

### Policy model highlights

- primary key: `_id` (`@Id`)
- unique/sparse indexes for `policy_number` and `idempotency_key`
- additional indexes/compound indexes for customer/status/date access patterns
- soft delete support (`deleted` flag)

### Redis usage

- `Idempotency-Key` deduplication for write operations
- prevents duplicate execution and returns cached prior result when applicable

## API Surface (current)

### Policy CRUD + lifecycle

- `POST /api/v1/policies`
- `GET /api/v1/policies/{policyId}`
- `GET /api/v1/policies`
- `PUT /api/v1/policies/{policyId}`
- `DELETE /api/v1/policies/{policyId}`
- `POST /api/v1/policies/{policyId}/cancel`
- `POST /api/v1/policies/{policyId}/suspend`
- `POST /api/v1/policies/{policyId}/reactivate`
- `PUT /api/v1/policies/{policyId}/premium` (hidden/internal endpoint)
- `GET /api/v1/policies/customer/{customerId}` (hidden/internal endpoint)

### Open Insurance read endpoints

- `GET /insurance-patrimonial`
- `GET /insurance-patrimonial/{policyId}/policy-info`
- `GET /insurance-patrimonial/{policyId}/premium`
- `GET /insurance-patrimonial/{policyId}/claim`

## Consistency and Reliability Strategy

- idempotent writes via Redis
- retries for outbox publication
- soft-delete semantics for policy records
- correlation ID propagation in logs
- health indicators for runtime dependencies

## Why this architecture

- **Maintainability**: core business logic isolated from frameworks
- **Testability**: domain/application tests can mock output ports
- **Replaceability**: persistence/messaging implementations can change with lower core impact
- **Resilience**: outbox + async events reduce coupling and absorb transient failures

---

**Version**: 1.0.0  
**Last Updated**: 2026-10-12
