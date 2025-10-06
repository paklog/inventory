# Developer Onboarding Guide

Welcome to the Inventory Management Service! This guide will help you get up and running quickly.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Overview](#project-overview)
3. [Getting Started](#getting-started)
4. [Development Workflow](#development-workflow)
5. [Architecture Overview](#architecture-overview)
6. [Common Tasks](#common-tasks)
7. [Troubleshooting](#troubleshooting)
8. [Learning Resources](#learning-resources)

---

## Prerequisites

### Required Software

| Tool | Version | Purpose | Installation |
|------|---------|---------|--------------|
| **Java** | 21+ | Runtime | [Download JDK](https://adoptium.net/) |
| **Maven** | 3.9+ | Build tool | [Install Maven](https://maven.apache.org/install.html) |
| **Docker** | 24+ | Containers | [Install Docker](https://docs.docker.com/get-docker/) |
| **Git** | 2.40+ | Version control | [Install Git](https://git-scm.com/downloads) |

### Recommended Software

| Tool | Purpose |
|------|---------|
| **IntelliJ IDEA** | Java IDE (Community or Ultimate) |
| **Postman** | API testing |
| **MongoDB Compass** | MongoDB GUI |
| **Kafdrop** | Kafka UI |

### Accounts & Access

- GitHub account with access to `paklog/inventory` repository
- Slack workspace: `#paklog-platform-team`
- AWS access (for staging/production deployment)
- Confluence access (for additional documentation)

---

## Project Overview

### What is the Inventory Management Service?

The Inventory Management Service is a microservice that:
- Tracks product stock levels across warehouses
- Manages stock allocations for orders
- Provides inventory health metrics
- Publishes inventory events to other services

### Technology Stack

```
┌─────────────────────────────────────────────────┐
│ Application Layer                               │
│ • Java 21                                       │
│ • Spring Boot 3.2.5                             │
│ • Spring Data MongoDB                           │
│ • Spring Kafka                                  │
└─────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────┐
│ Infrastructure                                  │
│ • MongoDB 7.0 (Data store)                      │
│ • Kafka (Event bus)                             │
│ • Redis (Cache - L2)                            │
│ • Caffeine (Cache - L1)                         │
└─────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────┐
│ Observability                                   │
│ • OpenTelemetry (Distributed tracing)           │
│ • Prometheus (Metrics)                          │
│ • Structured logging (JSON)                     │
└─────────────────────────────────────────────────┘
```

### Architecture

We use **Hexagonal Architecture** (Ports and Adapters):

```
src/main/java/com/paklog/inventory/
├── domain/          # Business logic (NO framework dependencies)
│   ├── model/       # Aggregates, Entities, Value Objects
│   ├── repository/  # Repository interfaces (ports)
│   └── event/       # Domain events
├── application/     # Use cases / Application services
│   ├── service/     # Command and Query services
│   └── dto/         # Data Transfer Objects
└── infrastructure/  # Technical implementation (adapters)
    ├── web/         # REST controllers
    ├── persistence/ # MongoDB repositories
    └── messaging/   # Kafka publishers/consumers
```

**Key Principle**: Dependencies flow inward (Infrastructure → Application → Domain)

---

## Getting Started

### 1. Clone the Repository

```bash
git clone git@github.com:paklog/inventory.git
cd inventory
```

### 2. Start Dependencies

Start MongoDB and Kafka using Docker Compose:

```bash
docker-compose up -d
```

Verify containers are running:

```bash
docker-compose ps
```

Expected output:
```
NAME                 IMAGE                    STATUS
inventory-mongodb    mongo:7.0                Up
inventory-kafka      confluentinc/cp-kafka    Up
inventory-zookeeper  confluentinc/cp-zookeeper Up
inventory-redis      redis:7.2                Up
```

### 3. Build the Project

```bash
./mvnw clean install
```

This will:
- Compile the code
- Run unit tests
- Run integration tests (using Testcontainers)
- Generate code coverage report

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on **http://localhost:8085**

### 5. Verify It's Working

**Check health endpoint**:
```bash
curl http://localhost:8085/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "mongo": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

**Create test stock**:
```bash
curl -X PATCH http://localhost:8085/inventory/stock_levels/TEST-SKU-001 \
  -H 'Content-Type: application/json' \
  -d '{
    "quantity_change": 100,
    "reason_code": "stock_intake",
    "comment": "Test data"
  }'
```

**Query stock**:
```bash
curl http://localhost:8085/inventory/stock_levels/TEST-SKU-001
```

Expected response:
```json
{
  "sku": "TEST-SKU-001",
  "quantity_on_hand": 100,
  "quantity_allocated": 0,
  "available_to_promise": 100
}
```

### 6. Explore the API

Open **Swagger UI** in your browser:
```
http://localhost:8085/swagger-ui.html
```

---

## Development Workflow

### Day-to-Day Development

#### 1. Pull Latest Changes
```bash
git checkout main
git pull origin main
```

#### 2. Create Feature Branch
```bash
git checkout -b feature/your-feature-name
```

#### 3. Make Changes

Edit code in your IDE (IntelliJ IDEA recommended).

**Hot Reload**: Use Spring Boot DevTools for automatic restart on code changes.

#### 4. Run Tests

**Unit tests only**:
```bash
./mvnw test
```

**Integration tests**:
```bash
./mvnw verify
```

**Single test class**:
```bash
./mvnw test -Dtest=ProductStockTest
```

**Single test method**:
```bash
./mvnw test -Dtest=ProductStockTest#shouldAdjustQuantityOnHand
```

#### 5. Check Code Coverage

```bash
./mvnw clean test jacoco:report
```

Open `target/site/jacoco/index.html` in a browser.

**Target**: > 80% code coverage

#### 6. Commit and Push

```bash
git add .
git commit -m "feat: add bulk allocation endpoint"
git push origin feature/your-feature-name
```

#### 7. Create Pull Request

Go to GitHub and create a PR from your branch to `main`.

**PR Checklist**:
- [ ] All tests passing
- [ ] Code coverage > 80%
- [ ] API documentation updated (if API changed)
- [ ] No SonarQube critical issues
- [ ] At least one approval from team member

---

## Architecture Overview

### Domain Model

#### ProductStock Aggregate

The central aggregate representing inventory for a SKU:

```java
public class ProductStock {
    private String sku;                          // Unique identifier
    private StockLevel stockLevel;               // Quantity on hand + allocated
    private Location location;                   // Warehouse location
    private List<LotBatch> lotBatches;          // Lot tracking
    private List<SerialNumber> serialNumbers;    // Serial number tracking
    private List<InventoryHold> holds;          // Quality holds, etc.
    private ABCClassification abcClassification; // ABC analysis
    private InventoryValuation valuation;        // Cost and value

    // Business methods
    public void adjustQuantityOnHand(int quantity, String reasonCode) { ... }
    public void allocate(int quantity) { ... }
    public void deallocate(int quantity) { ... }
    public int getAvailableToPromise() { ... }
}
```

#### Key Invariants

1. **Available = OnHand - Allocated**
   - Always maintained by aggregate methods
   - Enforced in domain logic

2. **Allocations Cannot Exceed Available**
   ```java
   if (quantity > getAvailableToPromise()) {
       throw new InsufficientStockException();
   }
   ```

3. **Events Always Published on State Change**
   - `StockLevelChangedEvent` when quantity changes
   - `StockStatusChangedEvent` when status changes

### Event Flow

#### Outbox Pattern for Reliable Event Publishing

```
1. Business Operation (in transaction)
   ├── Update ProductStock
   ├── Save to MongoDB
   └── Save OutboxEvent to MongoDB

2. Background Publisher (every 5 seconds)
   ├── Poll unpublished events from outbox
   ├── Publish to Kafka
   └── Mark as published
```

**Why?** Guarantees events are published even if Kafka is down.

#### CloudEvents Format

All events follow CloudEvents v1.0 specification:

```json
{
  "specversion": "1.0",
  "type": "com.paklog.inventory.fulfillment.v1.product-stock.level-changed",
  "source": "inventory-service",
  "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "time": "2025-10-05T20:00:00Z",
  "subject": "SKU-12345",
  "data": {
    "sku": "SKU-12345",
    "previousQuantityOnHand": 500,
    "newQuantityOnHand": 600,
    "changeType": "ADJUSTMENT"
  }
}
```

### Caching Strategy

**L1 Cache** (Caffeine - In-Memory):
- Sub-millisecond response
- 10,000 entry max
- 5 min TTL

**L2 Cache** (Redis - Distributed):
- Cross-instance sharing
- 5-30 min TTL (varies by data type)
- Automatic eviction on updates

---

## Common Tasks

### Add a New Endpoint

1. **Create DTO** (if needed):
   ```java
   // src/main/java/com/paklog/inventory/application/dto/
   public record NewFeatureRequest(
       String param1,
       int param2
   ) {}
   ```

2. **Add method to service**:
   ```java
   // InventoryCommandService or InventoryQueryService
   public Result processNewFeature(NewFeatureRequest request) {
       // Implementation
   }
   ```

3. **Add controller endpoint**:
   ```java
   @PostMapping("/new-feature")
   public ResponseEntity<Result> newFeature(@Valid @RequestBody NewFeatureRequest request) {
       Result result = commandService.processNewFeature(request);
       return ResponseEntity.ok(result);
   }
   ```

4. **Update OpenAPI spec**:
   Edit `openapi.yaml` to document the new endpoint.

5. **Write tests**:
   - Unit test for service logic
   - Integration test for controller

### Add a New Domain Event

1. **Create event class**:
   ```java
   // src/main/java/com/paklog/inventory/domain/event/
   public record NewFeatureEvent(
       String sku,
       String eventData,
       LocalDateTime occurredAt
   ) implements DomainEvent {
       @Override
       public String getEventType() {
           return "com.paklog.inventory.fulfillment.v1.new-feature.occurred";
       }
   }
   ```

2. **Add to CloudEventType enum**:
   ```java
   NEW_FEATURE_OCCURRED("com.paklog.inventory.fulfillment.v1.new-feature.occurred")
   ```

3. **Create JSON Schema**:
   Create `src/main/resources/asyncapi/cloudevents/jsonschema/new-feature-occurred-schema.json`

4. **Generate event in aggregate**:
   ```java
   public void performNewFeature() {
       // ... business logic ...
       this.registerEvent(new NewFeatureEvent(...));
   }
   ```

5. **Create sample CloudEvent**:
   Create `src/main/resources/asyncapi/cloudevents/samples/new-feature-occurred-sample.json`

### Debug Issues

#### Application Won't Start

**Check dependencies**:
```bash
docker-compose ps
```

All containers should be `Up`. If not:
```bash
docker-compose down
docker-compose up -d
```

**Check logs**:
```bash
# Application logs
tail -f logs/inventory-service.log

# MongoDB logs
docker-compose logs mongodb

# Kafka logs
docker-compose logs kafka
```

#### Tests Failing

**Clean build**:
```bash
./mvnw clean install
```

**Run specific failing test**:
```bash
./mvnw test -Dtest=FailingTestClass#failingMethod
```

**Check Testcontainers**:
Ensure Docker is running and has enough resources (4GB+ RAM recommended).

#### Kafka Events Not Publishing

**Check outbox table**:
```javascript
// Connect to MongoDB
docker exec -it inventory-mongodb mongosh inventorydb

// Check unpublished events
db.outbox_events.find({ published: false }).count()
```

**Check outbox publisher logs**:
```bash
grep "OutboxEventPublisher" logs/inventory-service.log
```

---

## Learning Resources

### Internal Documentation

- [Architecture Decision Records](./architecture/adrs/README.md)
- [C4 Diagrams](./architecture/c4-diagrams.md)
- [API Documentation](./api-documentation.md)
- [Testing Guide](./testing-guide.md)
- [API Versioning Strategy](./api-versioning-strategy.md)

### External Resources

#### Domain-Driven Design
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design by Vaughn Vernon](https://vaughnvernon.com/iddd/)

#### Hexagonal Architecture
- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

#### Spring Boot
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb)
- [Spring Kafka](https://spring.io/projects/spring-kafka)

#### Event-Driven Architecture
- [CloudEvents Specification](https://cloudevents.io/)
- [Microservices Patterns](https://microservices.io/patterns/)
- [Event Sourcing by Martin Fowler](https://martinfowler.com/eaaDev/EventSourcing.html)

### Video Tutorials

- [Spring Boot Microservices Tutorial](https://youtube.com/...)
- [Domain-Driven Design Crash Course](https://youtube.com/...)
- [Kafka Tutorial for Beginners](https://youtube.com/...)

---

## Team Contacts

| Role | Name | Slack | Email |
|------|------|-------|-------|
| **Tech Lead** | Jane Doe | @janedoe | jane.doe@paklog.com |
| **Backend Lead** | John Smith | @johnsmith | john.smith@paklog.com |
| **DevOps Lead** | Alice Johnson | @alicejohnson | alice.johnson@paklog.com |

### Slack Channels

- `#paklog-platform-team`: General team channel
- `#paklog-platform-dev`: Development discussions
- `#paklog-platform-ops`: Operations and incidents
- `#paklog-platform-questions`: Ask anything!

---

## Next Steps

After completing this guide, you should:

1. ✅ Have a working local development environment
2. ✅ Understand the architecture and domain model
3. ✅ Know how to run tests and check coverage
4. ✅ Be able to add new endpoints and events

**Your First Task**: Pick up a "good first issue" from the GitHub issues board!

**Questions?** Don't hesitate to ask in `#paklog-platform-questions`!

---

**Last Updated**: 2025-10-05
**Maintained By**: Platform Team
