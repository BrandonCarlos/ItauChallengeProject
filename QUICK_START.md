# Quick Start Guide - Patrimonial API

## 30-Second Setup

```bash
cd c:\Users\User\Downloads\wayon\wayon
docker-compose -f compose.yaml up -d
```

Wait 30-40 seconds for services to start, then:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## Access Points

| Service | URL | Purpose |
|---------|-----|---------|
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API Documentation |
| **OpenAPI JSON** | http://localhost:8080/v3/api-docs | Machine-readable spec |
| **Health Check** | http://localhost:8080/actuator/health | Service status |
| **MongoDB** | mongodb://root:rootpassword@localhost:27017 | Database |
| **Redis** | redis://localhost:6379 | Cache |
| **Kafka** | localhost:9092 | Message broker |

## Test the API

### 1. List Policies (Open Insurance API)

```bash
curl -X GET http://localhost:8080/insurance-patrimonial \
  -H "Authorization: Bearer dummy-token" \
  -H "x-fapi-interaction-id: 550e8400-e29b-41d4-a716-446655440000"
```

### 2. Create a Policy (CRUD API with Idempotency)

```bash
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: create-policy-001" \
  -d '{
    "policyNumber": "POL2024001",
    "productName": "Comprehensive Residential",
    "brand": "InsurCo",
    "companyName": "InsurCo Ltd",
    "cnpjNumber": "12345678901234",
    "issueDate": "2024-01-01",
    "startDate": "2024-01-01",
    "endDate": "2025-01-01"
  }'
```

Expected response: `201 Created`

### 3. Get Policy

```bash
# Replace with actual policy ID from create response
curl -X GET http://localhost:8080/api/v1/policies/{policyId}
```

### 4. List All Policies

```bash
curl -X GET "http://localhost:8080/api/v1/policies?page=0&size=10"
```

### 5. Update Policy

```bash
curl -X PUT http://localhost:8080/api/v1/policies/{policyId} \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: update-policy-001" \
  -d '{
    "productName": "Updated Product Name"
  }'
```

### 6. Delete Policy

```bash
curl -X DELETE http://localhost:8080/api/v1/policies/{policyId} \
  -H "Idempotency-Key: delete-policy-001"
```

Expected response: `204 No Content`

## Development Workflow

### 1. Build from Source

```bash
cd c:\Users\User\Downloads\wayon\wayon
mvn clean package -DskipTests
```

### 2. Run Application (Without Docker)

Requires local MongoDB, Redis, Kafka:

```bash
# Terminal 1: Start infrastructure
docker-compose -f compose.yaml up -d

# Terminal 2: Run application
java -jar target/patrimonial-api-1.0.0.jar
```

Or with Maven:

```bash
mvn spring-boot:run
```

### 3. Run with Custom Configuration

```bash
java -jar target/patrimonial-api-1.0.0.jar \
  --spring.data.mongodb.uri=mongodb://localhost:27017/patrimonial \
  --spring.redis.host=localhost \
  --kafka.bootstrap-servers=localhost:9092
```

## Verify Installation

### Check Docker Containers

```bash
docker-compose -f compose.yaml ps
```

Expected output:
```
NAME                    STATUS
patrimonial-mongodb     Up (healthy)
patrimonial-redis       Up (healthy)
patrimonial-kafka       Up (healthy)
patrimonial-zookeeper   Up (healthy)
patrimonial-api         Up (healthy)
```

### Check Service Health

```bash
# Application health
curl -s http://localhost:8080/actuator/health | jq .

# MongoDB connection
docker exec patrimonial-mongodb mongosh -u root -p rootpassword --authenticationDatabase admin --eval "db.adminCommand('ping')"

# Redis connection
docker exec patrimonial-redis redis-cli -a redispassword ping

# Kafka broker
docker exec patrimonial-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

## Common Commands

### View Logs

```bash
# All services
docker-compose -f compose.yaml logs -f

# Specific service
docker-compose -f compose.yaml logs -f patrimonial-api

# Last 100 lines
docker-compose -f compose.yaml logs --tail=100 patrimonial-api
```

### Stop All Services

```bash
docker-compose -f compose.yaml down
```

### Stop and Remove Volumes (Clean Slate)

```bash
docker-compose -f compose.yaml down -v
```

### Restart Services

```bash
docker-compose -f compose.yaml restart
```

### View MongoDB Data

```bash
docker exec -it patrimonial-mongodb mongosh -u root -p rootpassword --authenticationDatabase admin patrimonial

# In mongosh:
> use patrimonial
> db.policies.find()
> db.outbox_events.find()
```

### Monitor Kafka Topics

```bash
# List topics
docker exec patrimonial-kafka kafka-topics --list --bootstrap-server localhost:9092

# Monitor topic
docker exec patrimonial-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic patrimonial.policy.created \
  --from-beginning \
  --value-deserializer org.apache.kafka.common.serialization.StringDeserializer
```

## Troubleshooting

### Issue: Connection refused to MongoDB

**Solution**:
```bash
# Wait for MongoDB to be fully ready
docker-compose -f compose.yaml logs patrimonial-mongodb | grep "initialized"

# Or manually initialize
docker exec patrimonial-mongodb mongosh -u root -p rootpassword --authenticationDatabase admin --eval "rs.status()"
```

### Issue: Kafka not available

**Solution**:
```bash
# Check if Zookeeper is ready first
docker-compose -f compose.yaml logs patrimonial-zookeeper | grep "Listening on"

# Then check Kafka
docker-compose -f compose.yaml logs patrimonial-kafka | grep "broker started"
```

### Issue: Redis connection error

**Solution**:
```bash
# Test Redis directly
docker exec patrimonial-redis redis-cli -a redispassword INFO server

# Check Redis memory usage
docker exec patrimonial-redis redis-cli -a redispassword INFO memory
```

### Issue: Application won't start

**Solution**:
```bash
# Check application logs in detail
docker-compose -f compose.yaml logs patrimonial-api

# Rebuild container
docker-compose -f compose.yaml build --no-cache patrimonial-api

# Restart
docker-compose -f compose.yaml up -d patrimonial-api
```

## Configuration Reference

### Environment Variables

Edit `.env` file or pass via docker-compose:

```env
# MongoDB
SPRING_DATA_MONGODB_URI=mongodb://root:rootpassword@mongodb:27017/patrimonial?authSource=admin&replicaSet=rs0

# Redis
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=redispassword

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Application
SERVER_PORT=8080
SPRING_PROFILE=dev
```

### Application Properties

Edit `src/main/resources/application.yaml`:

```yaml
# Idempotency
app:
  idempotency:
    enabled: true
    ttl-seconds: 86400

# Events
  events:
    kafka:
      enabled: true
      topic-prefix: patrimonial

# Outbox
  outbox:
    enabled: true
    poll-interval-ms: 5000
    batch-size: 100
```

## Performance Tuning

### MongoDB

```bash
# Check indexes
docker exec patrimonial-mongodb mongosh -u root -p rootpassword --authenticationDatabase admin patrimonial
> db.policies.getIndexes()

# Add index if needed
> db.policies.createIndex({ "policyNumber": 1 }, { unique: true })
```

### Redis

```bash
# Monitor Redis commands in real-time
docker exec patrimonial-redis redis-cli -a redispassword --stat

# Check memory fragmentation
docker exec patrimonial-redis redis-cli -a redispassword INFO memory
```

### Kafka

```bash
# Monitor consumer lag
docker exec patrimonial-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group patrimonial-service \
  --describe
```

## API Rate Limiting

The API supports idempotency key-based rate limiting:

```bash
# First request - succeeds
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Idempotency-Key: key-1" \
  -d '{...}'
# Response: 201 Created

# Duplicate concurrent request - returns 429
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Idempotency-Key: key-1" \
  -d '{...}'
# Response: 429 Too Many Requests

# Retry after initial completes - succeeds with cached result
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Idempotency-Key: key-1" \
  -d '{...}'
# Response: 201 Created (cached from first request)
```

## Production Checklist

Before deploying to production:

- [ ] Update OAuth2 configuration with real auth server
- [ ] Configure MongoDB connection pooling for production scale
- [ ] Setup Redis replication and persistence
- [ ] Configure Kafka replication factor (>1)
- [ ] Enable HTTPS/TLS for all connections
- [ ] Configure proper logging levels (INFO instead of DEBUG)
- [ ] Setup monitoring and alerting
- [ ] Implement backup and disaster recovery procedures
- [ ] Run security scanning (OWASP dependency check)
- [ ] Load test with production-like data volumes
- [ ] Setup CI/CD pipeline for automated deployments

## Support & Documentation

- **Full README**: See `README.md`
- **Implementation Report**: See `IMPLEMENTATION_REPORT.md`
- **API Documentation**: Access `/swagger-ui.html` after starting services
- **Architecture Details**: See `README.md` - Architecture section

---

**For more information, see the complete README.md file.**
