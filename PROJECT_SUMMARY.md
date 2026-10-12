# Itaú Insurance Patrimonial API - Project Summary

## Current status

The project is implemented and running on:

- **Java 25**
- **Spring Boot 3.5.16**
- **MongoDB 7**
- **Redis 7**
- **Kafka 7.5 + Zookeeper**

Architecture follows **Clean Architecture + Hexagonal (Ports & Adapters)**.

---

## What the service provides

### 1) Open Insurance read API

- `GET /insurance-patrimonial`
- `GET /insurance-patrimonial/{policyId}/policy-info`
- `GET /insurance-patrimonial/{policyId}/premium`
- `GET /insurance-patrimonial/{policyId}/claim`

### 2) Policy management API

- `POST /api/v1/policies`
- `GET /api/v1/policies/{policyId}`
- `GET /api/v1/policies`
- `PUT /api/v1/policies/{policyId}`
- `DELETE /api/v1/policies/{policyId}`
- `POST /api/v1/policies/{policyId}/cancel`
- `POST /api/v1/policies/{policyId}/suspend`
- `POST /api/v1/policies/{policyId}/reactivate`
- `PUT /api/v1/policies/{policyId}/premium` (internal/hidden)
- `GET /api/v1/policies/customer/{customerId}` (internal/hidden)

---

## Core architecture highlights

### Domain

- `Policy` aggregate with business rules and lifecycle transitions.
- Domain events:
  - `PolicyCreatedEvent`
  - `PolicyUpdatedEvent`
  - `PolicyDeletedEvent`

### Ports

- Input port: `PolicyUseCasePort`
- Output ports:
  - `PolicyRepositoryPort`
  - `PolicyEventPublisher`

### Adapters

- REST controllers as input adapters.
- Mongo repository adapter as persistence output adapter.
- Outbox/Kafka adapter path for event publication.
- Redis idempotency service for deduplication of write operations.

---

## Data and messaging model

### MongoDB

- Main collections:
  - `policies`
  - `outbox_events`
- Auto index creation enabled.
- Soft-delete support via `deleted` flag.

### Redis

- Stores idempotency request state keyed by `idempotency:{key}`.
- Prevents duplicate write execution for same idempotency key.

### Kafka

- Active topics:
  - `policy-events`
  - `domain-events`
- Consumers:
  - `PolicyEventConsumer.consumePolicyEvent(...)`
  - `PolicyEventConsumer.consumeDomainEvent(...)`

---

## Reliability strategy

### Transactional Outbox

1. Save business data.
2. Save outbox event (`PENDING`) in Mongo.
3. Scheduled publisher sends to Kafka.
4. Mark as `PUBLISHED` on success.
5. Retry failed publications with configured retry policy.

This gives reliable asynchronous publication (at-least-once behavior).

### Idempotency

- Write endpoints require `Idempotency-Key`.
- Repeated same request key can return cached result instead of re-executing.

---

## Operational visibility

- Correlation ID in logs.
- Actuator health endpoint:
  - `/actuator/health`
- Health/readiness includes Mongo, Redis, and Kafka components.

---

## Documentation alignment

Main docs now aligned with current implementation:

- `README.md`
- `ARCHITECTURE_README.md`
- `IMPLEMENTATION_REPORT.md`
- `QUICK_START.md`

