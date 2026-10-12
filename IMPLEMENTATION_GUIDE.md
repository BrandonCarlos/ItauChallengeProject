# Itaú Insurance Patrimonial API - Implementation Guide

## Quick Start Guide

### Prerequisites
- Java 11 or higher
- Spring Boot 2.7+ or 3.0+
- Maven 3.6+
- Jakarta Bean Validation API (jakarta.validation)
- MapStruct 1.5+
- Lombok 1.18+

### Dependencies to Add to pom.xml

```xml
<!-- Spring Boot Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Jakarta Bean Validation -->
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>

<!-- SLF4J/Logback (included in spring-boot-starter-logging) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
</dependency>
```

### Plugins to Add to pom.xml

```xml
<plugins>
    <!-- Lombok Annotation Processor -->
    <plugin>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok-maven-plugin</artifactId>
        <version>1.18.20.0</version>
    </plugin>
    
    <!-- MapStruct Annotation Processor -->
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.8.1</version>
        <configuration>
            <source>11</source>
            <target>11</target>
            <annotationProcessorPaths>
                <path>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                    <version>1.18.30</version>
                </path>
                <path>
                    <groupId>org.mapstruct</groupId>
                    <artifactId>mapstruct-processor</artifactId>
                    <version>1.5.5.Final</version>
                </path>
            </annotationProcessorPaths>
        </configuration>
    </plugin>
</plugins>
```

## Component Overview

### 1. Domain Layer (/domain)

#### 1.1 Policy Model (domain/model/Policy.java)
The core business entity representing an insurance policy.

**Key Features:**
- Value objects for policy state (PolicyStatus enum)
- Business logic encapsulation
- Validation at entity level
- Lifecycle management methods

**PolicyStatus Enum:**
```java
ACTIVE       // Policy is currently active
SUSPENDED    // Policy is temporarily suspended
CANCELLED    // Policy has been cancelled
EXPIRED      // Policy has expired
```

**Key Methods:**
- `validate()`: Validates business rules
- `cancel()`: Changes status to CANCELLED
- `suspend()`: Changes status to SUSPENDED
- `reactivate()`: Reactivates suspended policy
- `updatePremium(Long)`: Updates premium with validation

#### 1.2 Domain Events (domain/event/)
Events representing important business occurrences:

- **PolicyCreatedEvent**: Published when policy is created
- **PolicyUpdatedEvent**: Published when policy is modified
- **PolicyDeletedEvent**: Published when policy is deleted

Each event:
- Implements `DomainEvent` interface
- Contains relevant business data
- Has unique event ID and timestamp
- Provides factory methods from domain entities

#### 1.3 Domain Exceptions (domain/exception/)
Business exception hierarchy:

```
DomainException (abstract)
├── PolicyNotFoundException
├── InvalidPolicyException
│   └── Factory methods: ofInvalidPremium, ofInvalidCoverage, ofInvalidCustomer
└── IdempotencyKeyConflictException
```

All extend `DomainException` which extends `RuntimeException`.

#### 1.4 Domain Ports (domain/ports/)

**PolicyUseCasePort** (Input Port):
Defines what use cases clients can invoke:
- `createPolicy()`: Create new policy
- `updatePolicy()`: Update existing policy
- `getPolicyById()`: Retrieve policy by ID
- `getCustomerPolicies()`: Get all policies for customer
- `deletePolicy()`: Delete a policy
- `cancelPolicy()`: Cancel a policy
- `suspendPolicy()`: Suspend a policy
- `reactivatePolicy()`: Reactivate suspended policy
- `updatePremium()`: Update policy premium

**PolicyRepositoryPort** (Output Port):
Abstracts data persistence:
- `save()`: Insert new policy
- `update()`: Update existing policy
- `findById()`: Retrieve by ID
- `findByCustomerId()`: Find all for customer
- `findByPolicyNumber()`: Find by policy number
- `findByIdempotencyKey()`: Find by idempotency key
- `deleteById()`: Delete policy
- `existsById()`: Check existence
- `findAll()`: Get all policies
- `count()`: Count total policies

### 2. Application Layer (/application)

#### 2.1 PolicyApplicationService (application/service/)
Main orchestrator implementing `PolicyUseCasePort`:

**Responsibilities:**
- Coordinate between layers
- Enforce use case flows
- Manage transactions
- Publish domain events
- Transform between DTOs and domain models

**Transaction Management:**
Uses `@Transactional` for ACID properties

**Event Publishing:**
Publishes domain events after each operation

**Error Handling:**
Translates domain exceptions to appropriate responses

#### 2.2 DTOs (application/dto/)

**PolicyRequestDTO**:
- Input DTO for creating/updating policies
- Jakarta validation annotations
- Immutable after deserialization
- Optional idempotency key support

**PolicyResponseDTO**:
- Output DTO for policy responses
- Read-only properties
- JSON formatted dates
- Complete policy information

#### 2.3 MapStruct Mapper (application/mapper/)

**PolicyMapper Interface**:
- Converts RequestDTO → Domain entity
- Converts Domain entity → ResponseDTO
- Updates entity from DTO
- Zero-cost abstraction at compile time

**Key Mappings:**
- Ignores generated/managed fields (ID, timestamps)
- Handles enum conversions
- Supports partial updates

#### 2.4 Event Publisher Port (application/service/)

**PolicyEventPublisher Interface**:
- `publishPolicyCreated()`: Publish creation event
- `publishPolicyUpdated()`: Publish update event
- `publishPolicyDeleted()`: Publish deletion event

#### 2.5 Event Publisher Adapter (application/adapter/)

**PolicyEventPublisherAdapter**:
- Implements PolicyEventPublisher
- Currently logs events
- Ready for integration with:
  - RabbitMQ
  - Apache Kafka
  - AWS SNS/SQS
  - Google Pub/Sub
  - Any message broker

### 3. Entrypoint Layer (/entrypoint/rest)

#### 3.1 PolicyController
RESTful API for policy operations:

**Endpoints:**
```
POST   /api/v1/policies                 - Create
GET    /api/v1/policies/{id}            - Get by ID
GET    /api/v1/policies/customer/{id}   - Get customer's
GET    /api/v1/policies                 - Get all
PUT    /api/v1/policies/{id}            - Update
PUT    /api/v1/policies/{id}/premium    - Update premium
POST   /api/v1/policies/{id}/cancel     - Cancel
POST   /api/v1/policies/{id}/suspend    - Suspend
POST   /api/v1/policies/{id}/reactivate - Reactivate
DELETE /api/v1/policies/{id}            - Delete
```

**Features:**
- Input validation via annotations
- Idempotency-Key header support
- Consistent response format
- Comprehensive logging

#### 3.2 InsurancePatrimonialController
Open Insurance API compliance:

**Endpoints:**
```
GET /open-insurance/insurance/patrimonial/products        - List all
GET /open-insurance/insurance/patrimonial/products/{id}   - Get product
GET /open-insurance/insurance/patrimonial/branches        - Branches info
GET /open-insurance/insurance/patrimonial/health          - Health check
```

**Features:**
- Open Insurance standard format
- Brazilian Central Bank compliance
- Standardized response envelope
- Metadata and pagination support

#### 3.3 GlobalExceptionHandler
Centralized exception handling:

**Handles:**
- `PolicyNotFoundException` → 404
- `InvalidPolicyException` → 400
- `IdempotencyKeyConflictException` → 409
- `DomainException` → 400
- Validation errors → 400
- Generic exceptions → 500

**Response Format:**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Policy not found",
  "code": "POLICY_NOT_FOUND",
  "details": {}
}
```

#### 3.4 DTOs (entrypoint/rest/dto/)

**InsuranceProductDTO**:
- Open Insurance API format
- Contains full product information
- JSON formatted dates

**OpenInsuranceResponseDTO**:
- Standard response envelope
- Contains data array
- Metadata (pagination)
- HATEOAS links support

**ErrorResponseDTO**:
- Standardized error format
- Includes error code
- Optional details map
- Timestamp for debugging

### 4. Configuration Layer (/config)

**PatrimonialInsuranceConfig**:
- Spring Boot configuration class
- Enables transaction management
- Placeholder for bean definitions
- Application-wide settings

## Integration Points

### Implementing PolicyRepositoryPort

Create a JPA repository implementation:

```java
@Repository
public class PolicyJpaRepository implements PolicyRepositoryPort {
    private final PolicyJpaEntity repository;
    
    @Override
    public Policy save(Policy policy) {
        PolicyJpaEntity entity = mapper.toEntity(policy);
        PolicyJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Policy> findById(String policyId) {
        return repository.findById(policyId)
            .map(mapper::toDomain);
    }
    // ... other methods
}
```

### Extending PolicyEventPublisher

Integrate with message broker:

```java
@Component
public class RabbitMQEventPublisher implements PolicyEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    
    @Override
    public void publishPolicyCreated(PolicyCreatedEvent event) {
        rabbitTemplate.convertAndSend("policy.exchange", 
            "policy.created", event);
    }
    // ... other methods
}
```

## Validation Strategy

The application uses multiple validation layers:

1. **Input Validation** (Controller level)
   - Jakarta Bean Validation annotations
   - Automatic validation via `@Valid`
   - Consistent error responses

2. **Business Rule Validation** (Domain level)
   - Domain-specific rules
   - Called in use cases
   - Raises InvalidPolicyException

3. **State Validation** (Domain level)
   - Policy lifecycle rules
   - Status transition rules
   - Enforced in domain methods

## Error Handling Strategy

1. **Domain Errors** → DomainException
   - Translated to 4xx responses
   - Handled by GlobalExceptionHandler
   - Business-readable messages

2. **Validation Errors** → MethodArgumentNotValidException
   - 400 Bad Request
   - Field-level error details
   - Consistent format

3. **System Errors** → Generic Exception
   - 500 Internal Server Error
   - Logged for debugging
   - Generic user message

## Transaction Management

- `@Transactional` on service methods
- Automatic rollback on exceptions
- Participates in Spring transaction context
- Propagation: REQUIRED (default)

## Logging Strategy

```
INFO:  Business operations (creation, updates, deletions)
DEBUG: Detailed execution flow
WARN:  Exceptional but recoverable conditions
ERROR: System failures and unrecoverable errors
```

Example:
```java
log.info("Creating new policy for customer: {}", customerId);
log.debug("Policy validation passed");
log.warn("Idempotency key conflict detected: {}", key);
log.error("Database error occurred", exception);
```

## Security Considerations

Future enhancements:
- Add Spring Security for authentication
- Implement authorization with role-based access
- Add audit trail for compliance
- Encrypt sensitive data
- Add rate limiting
- HTTPS enforcement
- CORS configuration

## Testing Strategy (To Be Implemented)

1. **Unit Tests**: Test individual components
2. **Integration Tests**: Test layer interactions
3. **Contract Tests**: Verify API contracts
4. **Acceptance Tests**: Validate business scenarios

## Performance Considerations

1. **Caching**: Add cache for frequently accessed policies
2. **Pagination**: Implement for large result sets
3. **Indexing**: Add database indexes on search fields
4. **Async Processing**: Consider for event publishing
5. **Connection Pooling**: Configure appropriate pool size

## Monitoring & Observability

1. **Logging**: SLF4J/Logback configured
2. **Metrics**: Ready for Micrometer integration
3. **Tracing**: Ready for Spring Cloud Sleuth
4. **Health Checks**: Actuator endpoints

Add to application.yml:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
```

## Deployment Considerations

1. **Database**: Choose and configure JPA persistence
2. **Message Broker**: Set up for event publishing
3. **Environment Configuration**: Externalize via application.yml
4. **Docker**: Container-ready architecture
5. **Kubernetes**: Service discovery ready

## Summary of Best Practices

✓ **Separation of Concerns**: Each layer has single responsibility
✓ **Dependency Inversion**: Depend on ports, not implementations
✓ **Open/Closed Principle**: Open for extension, closed for modification
✓ **Liskov Substitution**: Implementations are substitutable
✓ **Interface Segregation**: Small, focused interfaces
✓ **DRY Principle**: No code duplication
✓ **SOLID Principles**: Applied throughout
✓ **Domain-Driven Design**: Business logic at domain center
✓ **Event Sourcing Ready**: Events capture all changes
✓ **Testability**: Easily mockable dependencies
✓ **Maintainability**: Clear structure and naming
✓ **Scalability**: Stateless services, event-driven

---

**For detailed API documentation, see ARCHITECTURE_README.md**
