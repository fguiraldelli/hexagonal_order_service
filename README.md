# 🏗️ Hexagonal Architecture with Spring Boot

> Complete implementation example of Hexagonal Architecture (Ports & Adapters) using Java 21 and Spring Framework for microservices.

## Quick Start

1. Extract this ZIP file
2. Run: `mvn clean install`
3. Run: `mvn spring-boot:run`
4. API available at: `http://localhost:8080/api/v1/orders`

## Create an Order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "client-123",
    "total": 150.50
  }'
```

## Confirm an Order

```bash
curl -X PUT http://localhost:8080/api/v1/orders/{orderId}/confirm \
  -H "Content-Type: application/json"
```

For complete documentation, see the full README in the root directory.
