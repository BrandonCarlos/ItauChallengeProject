# Itaú Insurance Patrimonial API

## Overview

This is a comprehensive Java application for managing Patrimonial Insurance policies for Itaú Insurance. The application follows **Clean Architecture** with **Hexagonal Architecture (Ports and Adapters)** principles, ensuring a highly maintainable, testable, and scalable codebase.

## Architecture

### Clean Architecture Principles

The application is organized into clear, independent layers:

```
┌─────────────────────────────────────────────────────┐
│           Entrypoint Layer (REST)                   │
│  PolicyController, InsurancePatrimonialController   │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│         Application Layer (Use Cases)               │
│  PolicyApplicationService, Mappers, DTOs            │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│           Domain Layer (Business Logic)             │
│  Policy, Events, Exceptions, Ports                  │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│        Adapter Layer (Infrastructure)               │
│  Repository Implementations, Event Publishing       │
└─────────────────────────────────────────────────────┘
```

### Hexagonal Architecture (Ports & Adapters)

The application uses ports and adapters to maintain independence from external frameworks:

- **Input Ports**: Define what use cases clients can invoke
  - `PolicyUseCasePort`: Orchestrates policy-related operations
  
- **Output Ports**: Abstracts persistence and external services
  - `PolicyRepositoryPort`: Policy data persistence
  - `PolicyEventPublisher`: Event publishing to external systems

- **Adapters**: Implement the ports
  - REST Controllers: Adapt HTTP requests to use cases
  - Repository Implementations: Adapt to database operations
  - Event Publisher Adapter: Adapts to event infrastructure

## Project Structure

```
src/main/java/com/itau/insurance/patrimonial/
├── domain/                          # Domain Layer (Core Business Logic)
│   ├── model/
│   │   └── Policy.java              # Policy aggregate root
│   ├── event/
│   │   ├── DomainEvent.java         # Event interface
│   │   ├── PolicyCreatedEvent.java   # Creation event
│   │   ├── PolicyUpdatedEvent.java   # Update event
│   │   └── PolicyDeletedEvent.java   # Deletion event
│   ├── exception/
│   │   ├── DomainException.java     # Base exception
│   │   ├── PolicyNotFoundException.java
│   │   ├── InvalidPolicyException.java
│   │   └── IdempotencyKeyConflictException.java
│   └── ports/
│       ├── PolicyUseCasePort.java   # Input port (Use cases)
│       └── PolicyRepositoryPort.java # Output port (Repository)
├── application/                     # Application Layer (Use Cases & DTOs)
│   ├── service/
│   │   ├── PolicyApplicationService.java # Main use case orchestrator
│   │   └── PolicyEventPublisher.java    # Event publisher port
│   ├── dto/
│   │   ├── PolicyRequestDTO.java    # Input DTO
│   │   └── PolicyResponseDTO.java   # Output DTO
│   ├── mapper/
│   │   └── PolicyMapper.java        # MapStruct mapper
│   └── adapter/
│       └── PolicyEventPublisherAdapter.java # Event publisher impl
├── entrypoint/                      # Adapter Layer (REST)
│   └── rest/
│       ├── PolicyController.java            # Main REST controller
│       ├── InsurancePatrimonialController.java # Open Insurance API
│       ├── GlobalExceptionHandler.java      # Exception handling
│       └── dto/
│           ├── InsuranceProductDTO.java     # Open Insurance DTO
│           ├── OpenInsuranceResponseDTO.java # Open Insurance response
│           └── ErrorResponseDTO.java         # Error response
└── config/                          # Configuration
    └── PatrimonialInsuranceConfig.java
```

## Key Components

### 1. Domain Model (Policy)
- Core business entity representing an insurance policy
- Contains all business rules and validations
- Encapsulates policy lifecycle (ACTIVE, SUSPENDED, CANCELLED, EXPIRED)
- Provides domain methods: `validate()`, `cancel()`, `suspend()`, `reactivate()`, etc.

### 2. Domain Events
Domain events capture important business occurrences:
- **PolicyCreatedEvent**: Published when a policy is created
- **PolicyUpdatedEvent**: Published when a policy is modified
- **PolicyDeletedEvent**: Published when a policy is deleted

These events enable event-driven architecture and can be consumed by other bounded contexts.

### 3. Domain Ports
- **PolicyUseCasePort**: Input port defining available use cases
- **PolicyRepositoryPort**: Output port for data persistence
- Ports are implementation-agnostic, allowing easy swapping of adapters

### 4. Application Service
`PolicyApplicationService` orchestrates use cases:
- Implements `PolicyUseCasePort`
- Manages transactions
- Publishes domain events
- Converts between DTOs and domain models
- Provides consistent error handling

### 5. REST Controllers
- **PolicyController**: CRUD operations for policies
- **InsurancePatrimonialController**: Open Insurance API compliance

### 6. Exception Handling
Comprehensive exception hierarchy:
- `DomainException`: Base exception for all domain errors
- `PolicyNotFoundException`: Policy not found
- `InvalidPolicyException`: Business rule violations
- `IdempotencyKeyConflictException`: Duplicate operation prevention

## API Endpoints

### Policy Management (v1)

#### Create Policy
```
POST /api/v1/policies
Content-Type: application/json
Idempotency-Key: optional-key-for-deduplication

{
  "customerId": "CUST-001",
  "coverage": "RESIDENTIAL",
  "premium": 50000,
  "startDate": "2024-01-01",
  "endDate": "2025-01-01",
  "propertyDescription": "2 bedroom apartment",
  "propertyAddress": "Rua Principal, 123"
}
```

#### Get Policy
```
GET /api/v1/policies/{policyId}
```

#### Get Customer Policies
```
GET /api/v1/policies/customer/{customerId}
```

#### Update Policy
```
PUT /api/v1/policies/{policyId}
Content-Type: application/json

{
  "customerId": "CUST-001",
  "coverage": "RESIDENTIAL",
  "premium": 55000,
  "startDate": "2024-01-01",
  "endDate": "2025-01-01",
  "propertyDescription": "2 bedroom apartment",
  "propertyAddress": "Rua Principal, 123"
}
```

#### Update Premium
```
PUT /api/v1/policies/{policyId}/premium?newPremium=55000
```

#### Cancel Policy
```
POST /api/v1/policies/{policyId}/cancel
```

#### Suspend Policy
```
POST /api/v1/policies/{policyId}/suspend
```

#### Reactivate Policy
```
POST /api/v1/policies/{policyId}/reactivate
```

#### Delete Policy
```
DELETE /api/v1/policies/{policyId}?reason=Customer%20request
```

### Open Insurance API

#### Get Products
```
GET /open-insurance/insurance/patrimonial/products
```

#### Get Product by ID
```
GET /open-insurance/insurance/patrimonial/products/{policyId}
```

#### Health Check
```
GET /open-insurance/insurance/patrimonial/health
```

## Technology Stack

- **Java 11+**: Programming language
- **Spring Boot 2.7+**: Framework
- **Spring Data JPA**: Data access
- **MapStruct**: Object mapping
- **Lombok**: Boilerplate reduction
- **Jakarta Bean Validation**: Input validation
- **SLF4J / Logback**: Logging
- **Spring Transaction Management**: Transaction handling

## Design Patterns Used

1. **Hexagonal Architecture (Ports & Adapters)**: Maintains independence from external frameworks
2. **Clean Architecture**: Separation of concerns across layers
3. **Repository Pattern**: Abstracts data access
4. **Mapper Pattern**: Transforms between DTOs and domain models
5. **Exception Handling Pattern**: Comprehensive exception hierarchy
6. **Event Sourcing (Conceptual)**: Domain events capture business events
7. **Dependency Injection**: Spring-managed dependencies
8. **Service Layer Pattern**: Business logic encapsulation

## How to Extend

### Adding a New Use Case
1. Add method to `PolicyUseCasePort` interface
2. Implement in `PolicyApplicationService`
3. Add controller method in appropriate REST controller
4. Create domain event if needed
5. Update mapper if DTOs are affected

### Implementing Repository Port
1. Create implementation of `PolicyRepositoryPort`
2. Use `@Repository` annotation
3. Use JPA/Spring Data for database operations
4. Inject into `PolicyApplicationService`

### Publishing Events
1. Extend `PolicyEventPublisher` implementation
2. Integrate with message broker (RabbitMQ, Kafka, AWS SQS, etc.)
3. Current implementation logs events (ready for enhancement)

## Error Handling

The application provides comprehensive error responses:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Policy not found: policy-123",
  "code": "POLICY_NOT_FOUND",
  "path": "/api/v1/policies/policy-123"
}
```

## Validation

Input validation uses Jakarta Bean Validation annotations:
- `@NotBlank`: Required non-empty strings
- `@NotNull`: Required fields
- `@Positive`: Positive numeric values
- `@Size`: String length constraints
- `@JsonFormat`: Date format specification

## Idempotency

The API supports idempotent operations via `Idempotency-Key` header:
- Prevents duplicate policy creation when same request is retried
- Returns 409 Conflict if key already exists with different data
- Recommended for critical operations

## Logging

The application uses SLF4J with Logback:
- `@Slf4j` annotation on service and controller classes
- INFO level for business operations
- DEBUG level for detailed execution traces
- WARN level for exceptional conditions
- ERROR level for failures

## Future Enhancements

1. **Repository Implementation**: Add JPA repository implementation
2. **Event Bus Integration**: Integrate with RabbitMQ/Kafka for event publishing
3. **Caching**: Add caching layer for frequently accessed policies
4. **Audit Trail**: Track all policy changes
5. **Approval Workflow**: Add policy approval process
6. **Customer Integration**: Link policies to customer domain
7. **Claims Management**: Add claim processing domain
8. **Coverage Details**: Expand coverage types and details
9. **Documentation**: Add Swagger/OpenAPI documentation
10. **Integration Tests**: Add test layer

## References

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Spring Framework Documentation](https://spring.io/projects/spring-framework)
- [Banco Central Open Insurance](https://www.bcb.gov.br/)

---

**Version**: 1.0.0  
**Last Updated**: 2024-01-15  
**Author**: Copilot
