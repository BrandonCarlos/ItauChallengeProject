# Quick Start Guide - Patrimonial API

## 1) Start everything with Docker

```bash
git clone https://github.com/BrandonCarlos/ItauChallengeProject.git
cd ItauChallengeProject
docker-compose -f compose.yaml up -d --build
```

Wait ~30-60 seconds, then verify:

```bash
docker-compose -f compose.yaml ps
curl http://localhost:8080/actuator/health
```

## 2) Main URLs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

## 3) Quick API smoke test

### Open Insurance endpoint

```bash
curl -X GET "http://localhost:8080/insurance-patrimonial?page=1&page-size=25" \
  -H "Authorization: Bearer dummy-token" \
  -H "x-fapi-interaction-id: 550e8400-e29b-41d4-a716-446655440000"
```

### Create policy (idempotent write)

```bash
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: create-policy-001" \
  -d '{
    "policyNumber": "POL-9001",
    "customerId": "CUST-001",
    "coverage": "HOME",
    "premium": 1500,
    "startDate": "2026-10-12",
    "endDate": "2027-10-12",
    "propertyDescription": "Apartment in Sao Paulo",
    "propertyAddress": "Av. Paulista, 1000"
  }'
```

### Read / update / delete

```bash
# Replace {policyId}
curl -X GET "http://localhost:8080/api/v1/policies/{policyId}"

curl -X PUT "http://localhost:8080/api/v1/policies/{policyId}" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: update-policy-001" \
  -d '{
    "policyNumber": "POL-9001",
    "customerId": "CUST-001",
    "coverage": "HOME",
    "premium": 1700,
    "startDate": "2026-10-12",
    "endDate": "2027-10-12",
    "propertyDescription": "Apartment in Sao Paulo",
    "propertyAddress": "Av. Paulista, 1000"
  }'

curl -X DELETE "http://localhost:8080/api/v1/policies/{policyId}" \
  -H "Idempotency-Key: delete-policy-001"
```

## 4) Useful runtime commands

### Logs

```bash
docker-compose -f compose.yaml logs -f
docker-compose -f compose.yaml logs -f patrimonial-api
docker-compose -f compose.yaml logs -f kafka
docker-compose -f compose.yaml logs -f redis
docker-compose -f compose.yaml logs -f mongodb
```

### Kafka

```bash
docker exec patrimonial-kafka kafka-topics --list --bootstrap-server localhost:9092
docker exec patrimonial-kafka kafka-topics --describe --bootstrap-server localhost:9092 --topic policy-events
docker exec patrimonial-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group patrimonial-service

docker exec -it patrimonial-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic policy-events \
  --property print.key=true \
  --property print.headers=true
```

### Redis

```bash
docker exec -it patrimonial-redis redis-cli -a redispassword ping
docker exec -it patrimonial-redis redis-cli -a redispassword KEYS "idempotency:*"
docker exec -it patrimonial-redis redis-cli -a redispassword MONITOR
```

### MongoDB

```bash
docker exec -it patrimonial-mongodb mongosh
```

Inside mongosh:

```javascript
use patrimonial
show collections
db.policies.find().limit(5).pretty()
db.outbox_events.find().sort({created_at: -1}).limit(10).pretty()
db.policies.getIndexes()
```

## 5) Stop / reset

```bash
docker-compose -f compose.yaml down
docker-compose -f compose.yaml down -v
```

## 6) Local run without dockerized app container

Keep infra with docker:

```bash
docker-compose -f compose.yaml up -d mongodb mongodb-init redis zookeeper kafka
```

Run app:

```bash
mvn spring-boot:run
```

or:

```bash
java -jar target/patrimonial-api-1.0.0.jar \
  --spring.data.mongodb.uri=mongodb://localhost:27017/patrimonial?replicaSet=rs0 \
  --spring.data.redis.host=localhost \
  --spring.data.redis.password=redispassword \
  --spring.kafka.bootstrap-servers=localhost:9092
```

