# Open Insurance Patrimonial API

A production-ready, enterprise-grade Java backend for the Itaú Open Insurance Patrimonial API. Built with Spring Boot 3, MongoDB, Redis, and Kafka, following Clean Architecture and Hexagonal Architecture principles.

## Overview

This application implements a complete, RESTful API for managing insurance patrimonial policies, including:

- **Original Open Insurance Specification**: 4 GET endpoints for retrieving policy information, premium data, and claims
- **Additional CRUD API**: Full Create, Read, Update, Delete operations for policy management
- **Enterprise Features**: Redis-based idempotency, Kafka event streaming, MongoDB persistence with transactions, and a transactional outbox pattern
- **Production-Ready**: Comprehensive error handling, logging, health checks, metrics, and Docker deployment

## Project Overview

```
src/
├── main/
│   ├── java/com/itau/insurance/patrimonial/
│   │   ├── domain/                 # Domain layer (business logic)
│   │   │   ├── model/              # Domain entities
│   │   │   ├── event/              # Domain events
│   │   │   ├── exception/          # Domain exceptions
│   │   │   ├── service/            # Domain services
│   │   │   └── ports/              # Port definitions (in/out)
│   │   ├── application/            # Application layer (use cases)
│   │   │   ├── dto/                # Data transfer objects
│   │   │   ├── service/            # Application services
│   │   │   └── mapper/             # DTO/Entity mappers
│   │   ├── entrypoint/             # Entry point adapters
│   │   │   └── rest/               # REST controllers
│   │   └── infrastructure/         # Infrastructure adapters
│   │       ├── persistence/        # MongoDB persistence
│   │       ├── messaging/          # Kafka producers/consumers
│   │       ├── cache/              # Redis idempotency
│   │       ├── config/             # Spring configurations
│   │       └── exception/          # Error handling
│   └── resources/
│       └── application.yaml        # Application configuration
├── test/                           # Test suite
│   ├── unit/                       # Unit tests
│   ├── integration/                # Integration tests
│   └── e2e/                        # End-to-end tests
├── openapi/
│   └── swagger.yaml               # OpenAPI 3.0 specification
├── Dockerfile                      # Docker image definition
├── compose.yaml                   # Docker Compose configuration
├── pom.xml                        # Maven configuration
└── README.md                      # This file
```

## Architecture

### Clean Architecture + Hexagonal Architecture

The application strictly separates concerns into four layers:

1. **Domain Layer** - Pure business logic, independent of frameworks
   - Entities, value objects, domain services
   - Domain events for event-driven architecture
   - Ports (interfaces) defining dependencies

2. **Application Layer** - Use case orchestration
   - Application services coordinating business logic
   - DTOs for external communication
   - Mappers between domain and presentation

3. **Entrypoint Adapters** - Input handling
   - REST controllers implementing OpenAPI specification
   - Request/response DTOs
   - Validation and error handling

4. **Infrastructure Adapters** - Output handling
   - MongoDB persistence adapters
   - Redis idempotency service
   - Kafka event producers/consumers
   - External service integrations

### Key Design Principles

- ✅ **Dependency Inversion**: High-level modules don't depend on low-level modules; both depend on abstractions
- ✅ **Single Responsibility**: Each class has one reason to change
- ✅ **Open/Closed Principle**: Open for extension, closed for modification
- ✅ **Liskov Substitution**: Subtypes are substitutable for their base types
- ✅ **Interface Segregation**: Clients depend on specific interfaces, not general ones
- ✅ **DRY**: Don't repeat yourself
- ✅ **Separation of Concerns**: Each layer has distinct responsibilities

## Technology Stack

### Core Framework
- **Java 21** - Latest LTS version
- **Spring Boot 3.3.5** - Enterprise application framework
- **Spring Web** - RESTful API development
- **Spring Validation** - Bean validation (Jakarta)
- **Spring Data MongoDB** - MongoDB persistence
- **Spring Data Redis** - Redis caching and idempotency
- **Spring Kafka** - Event streaming

### Data & Storage
- **MongoDB 7.0** - NoSQL document database with replica set support
- **Redis 7** - In-memory data store for idempotency
- **Apache Kafka 7.5** - Distributed event streaming

### Tooling & Libraries
- **Maven** - Build automation and dependency management
- **Lombok** - Boilerplate code reduction
- **MapStruct** - Type-safe mapping framework
- **Springdoc OpenAPI 2.2** - API documentation and Swagger UI
- **JUnit 5** - Testing framework
- **Mockito** - Mocking library
- **Testcontainers** - Container-based testing for infrastructure

### Deployment
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration

## Prerequisites

### Development Environment

- **Java 21 or later** (JDK)
  ```bash
  java -version
  ```

- **Maven 3.9+**
  ```bash
  mvn -version
  ```

- **Docker & Docker Compose** (for running services)
  ```bash
  docker --version
  docker-compose --version
  ```

### Runtime Services

The application requires the following services (provided via Docker Compose):

- **MongoDB 7.0** - Document database
- **Redis 7** - Caching and idempotency storage
- **Apache Kafka 7.5** - Event broker
- **Zookeeper 7.5** - Kafka coordination

## Project Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure as needed:

```bash
cp .env.example .env
```

Key environment variables:

```env
# MongoDB
SPRING_DATA_MONGODB_URI=mongodb://root:rootpassword@localhost:27017/patrimonial?authSource=admin&replicaSet=rs0

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redispassword

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CONSUMER_GROUP=patrimonial-service

# Application
SERVER_PORT=8080
SPRING_PROFILE=dev
```

### Maven Dependencies

All dependencies are configured in `pom.xml`. The project uses:

- Spring Boot 3.3.5 with Java 21
- Spring Data MongoDB for persistence
- Spring Data Redis for idempotency
- Spring Kafka for event streaming
- Testcontainers for integration testing

## Installation & Setup

### 1. Clone Repository

```bash
cd c:\Users\User\Downloads\wayon\wayon
```

### 2. Setup Local Environment

#### Using Docker Compose (Recommended)

Start all services with a single command:

```bash
docker-compose -f compose.yaml up -d
```

This starts:
- MongoDB with replica set (port 27017)
- Redis (port 6379)
- Kafka & Zookeeper (port 9092)
- Patrimonial API (port 8080)

Verify services:

```bash
docker-compose -f compose.yaml ps
```

Stop services:

```bash
docker-compose -f compose.yaml down
```

#### Without Docker (Local Development)

Install and run services locally:

1. **MongoDB** (with replica set)
   ```bash
   # Requires MongoDB running with replica set support
   mongod --replSet rs0
   # Initialize replica set:
   mongosh
   > rs.initiate()
   ```

2. **Redis**
   ```bash
   redis-server
   ```

3. **Kafka**
   - Download from https://kafka.apache.org/
   - Start Zookeeper and Kafka broker

### 3. Build Project

```bash
mvn clean package
```

Or skip tests for faster build:

```bash
mvn clean package -DskipTests
```

### 4. Run Application

#### Development (with live reload)

```bash
mvn spring-boot:run
```

#### Production (from JAR)

```bash
java -jar target/patrimonial-api-1.0.0.jar
```

#### With Custom Configuration

```bash
java -jar target/patrimonial-api-1.0.0.jar \
  --spring.data.mongodb.uri=mongodb://localhost:27017/patrimonial \
  --spring.redis.host=localhost \
  --kafka.bootstrap-servers=localhost:9092
```

## Running Tests

### Unit Tests

```bash
mvn test
```

Run specific test class:

```bash
mvn test -Dtest=PolicyApplicationServiceTest
```

### Integration Tests (requires services running)

```bash
mvn verify
```

### Test Coverage

```bash
mvn test jacoco:report
# View coverage in: target/site/jacoco/index.html
```

## API Documentation

### Swagger UI

Access the interactive API documentation:

- **URL**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

### API Endpoints

#### Open Insurance Patrimonial API

The original Open Insurance Brasil specification defines these read-only endpoints:

##### 1. List Insurance Patrimonial Policies

```
GET /insurance-patrimonial
Headers:
  - Authorization: Bearer {token}
  - x-fapi-interaction-id: {uuid}
Query Parameters:
  - page: 1 (default: 1)
  - page-size: 25 (default: 25, max: 1000)

Response (200 OK):
{
  "data": [
    {
      "brand": "Brand Name",
      "companies": [
        {
          "companyName": "Company Name",
          "cnpjNumber": "12345678901234",
          "policies": [
            {
              "policyId": "123456",
              "productName": "Product Name"
            }
          ]
        }
      ]
    }
  ],
  "links": {
    "self": "https://api.example.com/open-insurance/insurance-patrimonial/v2/insurance-patrimonial?page=1",
    "first": "https://api.example.com/open-insurance/insurance-patrimonial/v2/insurance-patrimonial?page=1",
    "last": "https://api.example.com/open-insurance/insurance-patrimonial/v2/insurance-patrimonial?page=1"
  },
  "meta": {
    "totalRecords": 10,
    "totalPages": 1
  }
}
```

##### 2. Get Policy Information

```
GET /insurance-patrimonial/{policyId}/policy-info
Headers:
  - Authorization: Bearer {token}
  - x-fapi-interaction-id: {uuid}

Response (200 OK):
{
  "data": {
    "documentType": "APOLICE_INDIVIDUAL",
    "policyId": "123456",
    "issuanceDate": "2024-01-01",
    "termStartDate": "2024-01-01",
    "termEndDate": "2025-01-01",
    "maxLMG": {
      "amount": "100000.00",
      "currency": "BRL"
    },
    "proposalId": "PROP123",
    "insureds": [],
    "insuredObjects": [],
    "coverages": []
  },
  "links": {},
  "meta": {}
}
```

##### 3. Get Premium Data

```
GET /insurance-patrimonial/{policyId}/premium
Headers:
  - Authorization: Bearer {token}
  - x-fapi-interaction-id: {uuid}

Response (200 OK):
{
  "data": {
    "paymentsQuantity": 12,
    "amount": {
      "amount": "1200.00",
      "currency": "BRL"
    },
    "coverages": [],
    "payments": []
  },
  "links": {},
  "meta": {}
}
```

##### 4. Get Claims Data

```
GET /insurance-patrimonial/{policyId}/claim
Headers:
  - Authorization: Bearer {token}
  - x-fapi-interaction-id: {uuid}

Response (200 OK):
{
  "data": [
    {
      "identification": "SINISTRO123",
      "status": "ABERTO",
      "statusAlterationDate": "2024-01-15",
      "occurrenceDate": "2024-01-10",
      "warningDate": "2024-01-11",
      "amount": {
        "amount": "5000.00",
        "currency": "BRL"
      },
      "coverages": []
    }
  ],
  "links": {},
  "meta": {}
}
```

#### Additional CRUD API

Complete policy management operations:

##### Create Policy

```
POST /api/v1/policies
Content-Type: application/json
Idempotency-Key: {unique-key}

Request:
{
  "policyNumber": "POL2024001",
  "productName": "Comprehensive Residential",
  "brand": "InsurCo",
  "companyName": "InsurCo Ltd",
  "cnpjNumber": "12345678901234",
  "issueDate": "2024-01-01",
  "startDate": "2024-01-01",
  "endDate": "2025-01-01"
}

Response (201 Created):
{
  "id": "507f1f77bcf86cd799439011",
  "policyNumber": "POL2024001",
  "productName": "Comprehensive Residential",
  "status": "ACTIVE",
  "createdAt": "2024-01-01T10:00:00Z",
  "updatedAt": "2024-01-01T10:00:00Z"
}
```

##### Get Policy by ID

```
GET /api/v1/policies/{policyId}

Response (200 OK):
{
  "id": "507f1f77bcf86cd799439011",
  "policyNumber": "POL2024001",
  "productName": "Comprehensive Residential",
  "status": "ACTIVE",
  "createdAt": "2024-01-01T10:00:00Z",
  "updatedAt": "2024-01-01T10:00:00Z"
}

Response (404 Not Found):
{
  "timestamp": "2024-01-01T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Policy not found: {policyId}"
}
```

##### List Policies

```
GET /api/v1/policies?page=0&size=20&sort=createdAt,desc

Response (200 OK):
{
  "content": [
    { /* policy objects */ }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

##### Update Policy

```
PUT /api/v1/policies/{policyId}
Content-Type: application/json
Idempotency-Key: {unique-key}

Request:
{
  "policyNumber": "POL2024001",
  "customerId": "CUSTOMER123",
  "coverage": "HOME",
  "premium": 1500,
  "startDate": "2024-01-01",
  "endDate": "2025-01-01",
  "propertyDescription": "Updated property",
  "propertyAddress": "Updated address",
  "changeJustification": "Required only when policyNumber/customerId changes"
}

Response (200 OK):
{ /* updated policy object */ }
```

##### Delete Policy

```
DELETE /api/v1/policies/{policyId}
Idempotency-Key: {unique-key}

Response (204 No Content):
```

### Deletion, cancellation, and deleted-record queries

- `DELETE /api/v1/policies/{policyId}` performs **soft delete** (record is marked deleted, not physically removed).
- Default reads hide deleted records:
  - `GET /api/v1/policies/{policyId}` returns `404` for deleted records.
  - `GET /api/v1/policies` excludes deleted records by default.
- To query deleted records explicitly, use `includeDeleted=true`:
  - `GET /api/v1/policies/{policyId}?includeDeleted=true`
  - `GET /api/v1/policies?includeDeleted=true&page=0&size=10`
- Cancellation (`/api/v1/policies/{policyId}/cancel`) is a status transition (`ACTIVE` → `CANCELLED`) and is different from deletion.

### Immutable identifier updates

- `policyNumber` and `customerId` are treated as immutable identifiers.
- Updating either one in `PUT /api/v1/policies/{policyId}` requires explicit `changeJustification` in the request body.
- Missing justification when changing immutable identifiers returns `400`.
- Changing to an already-used `policyNumber` returns `409`.
- Additional business rule: cancelled policies cannot be changed (for example via `PUT /api/v1/policies/{policyId}`), and update attempts return `400`.

## Idempotency

The CRUD API supports idempotency for safe write operations (POST, PUT, DELETE).

### Usage

Include the `Idempotency-Key` header in write requests:

```bash
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-operation-id-12345" \
  -d '{
    "policyNumber": "POL2024001",
    "productName": "Comprehensive"
  }'
```

### How It Works

1. **First Request**: Operation executes and result is cached with TTL (default: 24 hours)
2. **Duplicate Request**: Same `Idempotency-Key` returns cached result without re-execution
3. **Conflict**: Different payload with same key returns 409 Conflict
4. **In-Progress**: Concurrent requests with same key return 429 Too Many Requests

### Guarantees & Limitations

- ✅ Prevents duplicate operations within TTL window
- ✅ Handles transient failures gracefully
- ✅ Uses atomic Redis operations
- ⚠️ Not guaranteed across service restarts
- ⚠️ TTL is configurable (default 24 hours)
- ⚠️ Requires Redis availability for enforcement

## Event-Driven Architecture

### Domain Events

The system publishes domain events for policy lifecycle:

- **PolicyCreatedEvent** - Published after successful policy creation
- **PolicyUpdatedEvent** - Published after successful policy update
- **PolicyDeletedEvent** - Published after successful policy deletion

### Kafka Integration

Events are published to Kafka topics for asynchronous processing:

- **Topics**: `patrimonial.policy.created`, `patrimonial.policy.updated`, `patrimonial.policy.deleted`
- **Format**: JSON with schema versioning
- **Partitioning**: By policyId for ordering guarantees per policy

### Transactional Outbox Pattern

The implementation uses the transactional outbox pattern:

1. **Write Phase**: Policy change + outbox event persisted together in MongoDB transaction
2. **Publish Phase**: Scheduled job polls outbox for unpublished events
3. **Reliability**: Ensures events are published exactly once, even on failures

Configuration:

```yaml
app:
  outbox:
    enabled: true
    poll-interval-ms: 5000       # Poll interval for unpublished events
    batch-size: 100              # Batch size for polling
    max-retries: 3               # Maximum retry attempts
    retry-delay-ms: 1000         # Delay between retries
```

### Event Consumer Example

Implement `PolicyEventConsumer` to handle events:

```java
@KafkaListener(topics = "patrimonial.policy.created")
public void onPolicyCreated(PolicyCreatedEvent event) {
    // Handle policy creation event
    log.info("Policy created: {}", event.getPolicyId());
}
```

## MongoDB Persistence

### Database Configuration

MongoDB is configured with replica set support for transactions:

```yaml
spring:
  mongodb:
    uri: mongodb://root:rootpassword@localhost:27017/patrimonial?authSource=admin&replicaSet=rs0
```

### Collections

- **policies** - Insurance policy documents
- **outbox_events** - Transactional outbox for reliable event publishing

### Indexes

Automatic index creation is enabled. Key indexes:

```javascript
db.policies.createIndex({ "policyNumber": 1 }, { unique: true })
db.policies.createIndex({ "status": 1 })
db.policies.createIndex({ "createdAt": 1 })
db.outbox_events.createIndex({ "status": 1, "createdAt": 1 })
db.outbox_events.createIndex({ "createdAt": 1 }, { expireAfterSeconds: 604800 })
```

### Transactions

Write operations use MongoDB transactions for atomicity:

```java
// Policy creation + outbox event in single transaction
TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
```

### Domain-to-Persistence Mapping

- **Domain**: Clean domain entities with business logic
- **Persistence**: MongoDB documents with @Document annotations
- **Mapping**: PolicyRepositoryAdapter converts between layers

This separation ensures the domain layer remains framework-independent.

## Redis Idempotency

### Configuration

Redis is configured for high-performance caching:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: redispassword
      timeout: 2000ms
      jedis:
        pool:
          max-active: 20
          max-idle: 10
```

### Idempotency Key Storage

Idempotency requests are stored with:

- **Key**: `idempotency:{idempotency-key}`
- **Value**: JSON with request hash, status, and response
- **TTL**: Configurable (default 24 hours)

### Implementation Details

```java
class IdempotencyRequest {
    String idempotencyKey;
    String requestHash;        // SHA-256 of request body
    IdempotencyStatus status;  // PENDING, SUCCESS, FAILED
    String response;           // Cached response body
    LocalDateTime expiresAt;
}

enum IdempotencyStatus {
    PENDING,    // Operation in progress
    SUCCESS,    // Operation completed successfully
    FAILED      // Operation failed
}
```

### Error Scenarios

| Scenario | Behavior |
|----------|----------|
| First request | Execute operation, cache result |
| Duplicate with same payload | Return cached result |
| Duplicate with different payload | Return 409 Conflict |
| Request in progress | Return 429 Too Many Requests |
| Cached result expired | Re-execute operation |

## Security

### Authentication & Authorization

The API requires OAuth2 authentication for all endpoints:

```
Authorization: Bearer {access_token}
```

Headers validated:

- `Authorization` - OAuth2 Bearer token
- `x-fapi-interaction-id` - Request correlation ID (UUID)
- `x-fapi-auth-date` - User's last authentication date (RFC7231)
- `x-fapi-customer-ip-address` - Client IP address
- `x-customer-user-agent` - Client user agent

### Token Validation

The application validates tokens against configured OAuth2 provider:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OAUTH2_ISSUER_URI}
          jwk-set-uri: ${OAUTH2_JWK_SET_URI}
```

### Development Mode (Without OAuth2)

For local development without OAuth2 setup, disable security:

```yaml
# Not recommended for production
app:
  security:
    enabled: false
```

### Best Practices

- ✅ Never hardcode credentials
- ✅ Use environment variables for secrets
- ✅ Enable HTTPS in production
- ✅ Implement rate limiting
- ✅ Log security events
- ✅ Validate all inputs
- ✅ Use strong CORS policies

## Health Checks & Monitoring

### Health Endpoints

```
GET /actuator/health
```

Health indicators:

- MongoDB connectivity
- Redis connectivity
- Kafka broker connectivity
- Application startup status

Example response:

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MongoDB"
      }
    },
    "kafkaHealthIndicator": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

### Metrics

```
GET /actuator/metrics
```

Available metrics:

- Request count and latency
- Database connection pool
- Kafka producer/consumer lag
- JVM metrics (memory, GC, threads)

### Logging

Logs are written to:

- **Console**: Structured logs with timestamps
- **File**: `logs/application.log` with rotation
- **Level**: INFO (default), DEBUG for development

Configuration:

```yaml
logging:
  level:
    root: INFO
    com.itau.insurance.patrimonial: DEBUG
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 10
```

## Docker Deployment

### Build Docker Image

```bash
docker build -t patrimonial-api:latest .
```

### Run with Docker Compose

```bash
docker-compose -f compose.yaml up -d
```

This starts:

- MongoDB (port 27017)
- Redis (port 6379)
- Kafka (port 9092)
- Application (port 8080)

### Verify Deployment

```bash
# Check service status
docker-compose -f compose.yaml ps

# View application logs
docker-compose -f compose.yaml logs -f patrimonial-api

# Check health
curl http://localhost:8080/actuator/health
```

### Environment Variables

Pass environment variables via `.env` file or command line:

```bash
docker run \
  -e SPRING_DATA_MONGODB_URI=mongodb://root:rootpassword@mongo:27017/patrimonial \
  -e SPRING_DATA_REDIS_HOST=redis \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -p 8080:8080 \
  patrimonial-api:latest
```

### Docker Compose Services

#### MongoDB

- **Container**: mongodb:7.0
- **Port**: 27017
- **Replica Set**: rs0 (required for transactions)
- **Volumes**: mongodb_data, mongodb_config
- **Init Script**: Automatic replica set initialization

#### Redis

- **Container**: redis:7-alpine
- **Port**: 6379
- **Password**: redispassword (configured)
- **Volumes**: redis_data

#### Kafka

- **Container**: confluentinc/cp-kafka:7.5.0
- **Port**: 9092 (external), 29092 (internal)
- **Zookeeper**: Automatically started
- **Topics**: Auto-created as needed

#### Patrimonial API

- **Container**: Built from Dockerfile
- **Port**: 8080
- **Health Check**: /actuator/health (30s interval)
- **Depends On**: MongoDB, Redis, Kafka
- **Volumes**: ./logs:/app/logs

### Production Deployment

For production, consider:

1. **Use managed services**: AWS MongoDB Atlas, AWS ElastiCache (Redis), AWS MSK (Kafka)
2. **Enable security**:
   - Use secrets manager for credentials
   - Enable TLS/SSL for all connections
   - Implement network policies
3. **Scale horizontally**: Multiple instances behind load balancer
4. **Monitor & alert**: CloudWatch, DataDog, or similar
5. **Backup strategy**: Regular MongoDB backups
6. **High availability**: Multi-region replication

## Troubleshooting

### MongoDB Connection Issues

```
Error: MongoSocketOpenException: Exception opening socket
```

**Solution**:
1. Verify MongoDB is running: `docker ps | grep mongodb`
2. Check credentials in SPRING_DATA_MONGODB_URI
3. Verify network connectivity: `docker network ls`

### Redis Connection Issues

```
Error: Unable to connect to Redis at localhost:6379
```

**Solution**:
1. Verify Redis is running: `docker ps | grep redis`
2. Check Redis password configuration
3. Test connection: `redis-cli -h localhost -p 6379 ping`

### Kafka Connection Issues

```
Error: KafkaException: Broker transport failure
```

**Solution**:
1. Verify Kafka is running: `docker ps | grep kafka`
2. Check bootstrap servers configuration
3. Test Kafka connectivity: `kafka-broker-api-versions --bootstrap-server localhost:9092`

### Out of Memory

```
java.lang.OutOfMemoryError: Java heap space
```

**Solution**:
1. Increase JVM heap: `java -Xmx2g -Xms1g -jar app.jar`
2. Check for memory leaks in logs
3. Monitor with `docker stats`

### Test Failures

```
Tests fail with "Connection refused" errors
```

**Solution**:
1. Ensure Testcontainers is properly configured
2. Docker must be running for integration tests
3. Check Docker permissions: `docker ps`

## Performance Optimization

### Database Optimization

1. **Connection Pooling**: Configured with max 20 connections
2. **Index Usage**: Ensure queries use defined indexes
3. **Batch Operations**: Use batch writes for high throughput

### Caching Strategy

1. **Redis TTL**: Set appropriate expiration times
2. **Cache Keys**: Use consistent, hierarchical naming
3. **Cache Invalidation**: Implement event-driven invalidation

### Kafka Optimization

1. **Batch Size**: Adjust producer batch size based on latency requirements
2. **Compression**: Enabled for network bandwidth savings
3. **Partitions**: Increase for higher throughput

### Application Tuning

1. **Thread Pools**: Configure for optimal concurrency
2. **Connection Timeouts**: Set reasonable limits
3. **Request Limits**: Implement rate limiting

## Known Limitations

1. **Replica Set Required**: MongoDB requires replica set for transactions (development setup handles this)
2. **Single Broker Kafka**: Docker Compose uses single Kafka broker (scale for production)
3. **In-Memory Redis**: All idempotency data in memory (use managed Redis for persistence)
4. **OAuth2 Mocking**: Local development may bypass OAuth2 validation
5. **No Distributed Tracing**: Add Jaeger or Zipkin for distributed tracing

## Contributing

### Code Standards

- Follow Google Java Style Guide
- Use 4-space indentation
- Add JavaDoc for public APIs
- Write tests for all functionality

### Testing Requirements

- Minimum 80% code coverage
- All tests must pass: `mvn verify`
- Integration tests must use Testcontainers

### Commit Messages

Format: `[COMPONENT] Brief description`

Examples:
- `[API] Add policy update endpoint`
- `[PERSISTENCE] Fix MongoDB index creation`
- `[TEST] Add idempotency integration tests`

## License

This project is licensed under the MIT License. See LICENSE file for details.

## Support

For issues and questions:

1. Check this README
2. Review test examples for usage patterns
3. Check application logs: `docker logs patrimonial-api`
4. Open an issue with detailed description

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Spring Kafka](https://spring.io/projects/spring-kafka)
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.0)
- [MongoDB Transactions](https://docs.mongodb.com/manual/transactions/)
- [Kafka Best Practices](https://kafka.apache.org/documentation/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://herbertograca.com/2017/09/28/hexagonal-architecture/)

---

**Version**: 1.0.0  
**Last Updated**: 2024-10-10  
**Java Version**: 21  
**Spring Boot Version**: 3.3.5
