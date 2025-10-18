# Inventory Management Service

Inventory management service providing single source of truth for product stock levels with event-driven architecture, DDD, and transactional outbox pattern.

## Overview

The Inventory Management Service serves as the single source of truth for product stock levels across the Paklog fulfillment platform. This bounded context manages quantity on hand, allocations, reservations, and calculates available-to-promise (ATP) inventory. It provides comprehensive inventory tracking with lot/batch management, serial number tracking, multi-location support, and audit trail capabilities.

## Domain-Driven Design

### Bounded Context
**Inventory Management** - Tracks and manages product stock levels, allocations, reservations, and inventory movements across multiple locations.

### Core Domain Model

#### Aggregates
- **ProductStock** - Root aggregate managing stock levels for a SKU
- **InventorySnapshot** - Point-in-time snapshot of inventory state
- **Container** - Physical container holding inventory items
- **StockTransfer** - Transfer of stock between locations
- **AssemblyOrder** - Kit assembly/disassembly operations
- **CycleCount** - Physical inventory count execution

#### Entities
- **InventoryLedgerEntry** - Immutable record of stock changes
- **OutboxEvent** - Event pending publication
- **SerialNumber** - Serialized inventory item tracking
- **StockLocation** - Stock stored at specific location
- **LotBatch** - Lot or batch of inventory
- **Kit** - Product bundle/assembly definition

#### Value Objects
- **StockLevel** - Quantity on hand, allocated, available to promise
- **Location** - Warehouse location identifier
- **ChangeType** - Type of inventory change (RECEIPT, ADJUSTMENT, etc.)
- **MultiUOMQuantity** - Quantity with unit of measure
- **PhysicalReservation** - Physical stock reservation
- **InventoryHold** - Inventory hold/quarantine status
- **ABCClassification** - ABC analysis classification

#### Domain Events
- **StockLevelChanged** - Stock level modified
- **StockAddedToLocation** - Stock added to location
- **StockRemovedFromLocation** - Stock removed from location
- **PhysicalStockReserved** - Stock physically reserved
- **PhysicalStockReservationReleased** - Reservation released
- **KitAssembledEvent** - Kit components assembled
- **KitDisassembledEvent** - Kit broken down to components
- **InventorySnapshotCreatedEvent** - Snapshot created
- **UOMConversionAddedEvent** - UOM conversion rule added

#### Domain Services
- **EventReplayService** - Rebuilds state from event stream

### Ubiquitous Language
- **Quantity on Hand (QoH)**: Physical inventory in warehouse
- **Allocated**: Inventory committed to orders but not yet picked
- **Available to Promise (ATP)**: Inventory available for new orders (QoH - Allocated)
- **Lot/Batch**: Group of inventory items with common attributes
- **Serial Number**: Unique identifier for individual inventory items
- **Cycle Count**: Periodic physical inventory verification
- **Kit**: Bundle of component products
- **Outbox Event**: Event awaiting asynchronous publication

## Architecture & Patterns

### Hexagonal Architecture (Ports and Adapters)

```
src/main/java/com/paklog/inventory/
├── domain/                           # Core business logic
│   ├── model/                       # Aggregates, entities, value objects
│   │   ├── ProductStock.java        # Main aggregate root
│   │   ├── StockLevel.java          # Core value object
│   │   ├── InventoryLedgerEntry.java # Audit entity
│   │   └── OutboxEvent.java         # Outbox entity
│   ├── repository/                  # Repository interfaces (ports)
│   ├── event/                       # Domain events
│   ├── service/                     # Domain services
│   └── exception/                   # Domain exceptions
├── application/                      # Use cases & orchestration
│   ├── service/                     # Application services
│   ├── command/                     # Commands
│   ├── query/                       # Queries
│   └── port/                        # Application ports
└── infrastructure/                   # External adapters
    ├── persistence/                 # MongoDB repositories
    ├── messaging/                   # Kafka consumers/publishers
    ├── web/                         # REST controllers
    ├── outbox/                      # Outbox scheduler
    └── config/                      # Configuration
```

### Design Patterns & Principles
- **Hexagonal Architecture** - Clean separation of domain and infrastructure
- **Domain-Driven Design** - Rich domain model with business invariants
- **CQRS** - Command/query separation
- **Event-Driven Architecture** - Integration via domain events
- **Transactional Outbox Pattern** - Guaranteed event delivery
- **Event Sourcing Lite** - Audit trail via immutable ledger entries
- **Repository Pattern** - Data access abstraction
- **Aggregate Pattern** - Consistency boundaries
- **SOLID Principles** - Maintainable and extensible code

## Technology Stack

### Core Framework
- **Java 21** - Programming language
- **Spring Boot 3.2.5** - Application framework
- **Maven** - Build and dependency management

### Data & Persistence
- **MongoDB** - Document database for aggregates
- **Spring Data MongoDB** - Data access layer
- **Optimistic Locking** - Concurrency control

### Messaging & Events
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration
- **CloudEvents 2.5.0** - Standardized event format

### API & Documentation
- **Spring Web MVC** - REST API framework
- **Bean Validation** - Input validation
- **OpenAPI/Swagger** - API documentation

### Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **OpenTelemetry 1.44.1** - Distributed tracing
- **Micrometer Tracing 1.2.5** - Tracing integration
- **Logstash Logback Encoder 7.2** - Structured logging
- **Loki** - Log aggregation

### Testing
- **JUnit 5** - Unit testing framework
- **Testcontainers 1.20.4** - Integration testing
- **Mockito 5.14.2** - Mocking framework
- **AssertJ** - Fluent assertions

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Local development environment

## Standards Applied

### Architectural Standards
- ✅ Hexagonal Architecture (Ports and Adapters)
- ✅ Domain-Driven Design tactical patterns
- ✅ CQRS for command/query separation
- ✅ Event-Driven Architecture
- ✅ Microservices architecture
- ✅ RESTful API design

### Code Quality Standards
- ✅ SOLID principles
- ✅ Clean Code practices
- ✅ Comprehensive unit and integration testing
- ✅ Domain-driven design patterns
- ✅ Immutable value objects
- ✅ Rich domain models with business logic

### Event & Integration Standards
- ✅ CloudEvents specification v1.0
- ✅ Transactional Outbox Pattern
- ✅ At-least-once delivery semantics
- ✅ Event versioning strategy
- ✅ Idempotent event consumers

### Data Standards
- ✅ Audit trail for all stock changes
- ✅ Optimistic locking for concurrency
- ✅ Event sourcing for audit log
- ✅ Single source of truth for inventory

### Observability Standards
- ✅ Structured logging (JSON)
- ✅ Distributed tracing (OpenTelemetry)
- ✅ Health check endpoints
- ✅ Prometheus metrics
- ✅ Correlation ID propagation

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/paklog/inventory.git
   cd inventory
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d mongodb kafka
   ```

3. **Build and run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify the service is running**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f inventory

# Stop all services
docker-compose down
```

## API Documentation

Once running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

### Key Endpoints

- `GET /inventory/stock_levels/{sku}` - Get current stock level for SKU
- `POST /inventory/adjustments` - Create manual stock adjustment
- `POST /inventory/allocations` - Allocate inventory to order
- `POST /inventory/reservations` - Reserve physical inventory
- `GET /inventory/reports/health` - Inventory health metrics
- `GET /inventory/ledger/{sku}` - Get audit trail for SKU
- `POST /inventory/transfers` - Transfer stock between locations
- `POST /inventory/cycle-counts` - Create cycle count

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run tests with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Configuration

Key configuration properties:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/inventory
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: inventory-service

inventory:
  outbox:
    scheduler:
      enabled: true
      fixed-delay: 5000
  ledger:
    retention-days: 365
```

## Event Integration

### Consumed Events
- `com.paklog.warehouse.inventory.allocation.requested` - From Warehouse Operations
- `com.paklog.warehouse.item.picked` - From Warehouse Operations
- `com.paklog.order.cancelled` - From Order Management

### Published Events
- `com.paklog.inventory.stock.level.changed.v1`
- `com.paklog.inventory.stock.added.to.location.v1`
- `com.paklog.inventory.stock.removed.from.location.v1`
- `com.paklog.inventory.reservation.created.v1`
- `com.paklog.inventory.kit.assembled.v1`

### Event Format
All events follow the CloudEvents specification v1.0 and are published via the transactional outbox pattern.

## Monitoring

- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus
- **Info**: http://localhost:8080/actuator/info

## Contributing

1. Follow hexagonal architecture principles
2. Implement domain logic in domain layer
3. Maintain aggregate consistency boundaries
4. Use transactional outbox for event publishing
5. Write comprehensive tests including domain model tests
6. Document domain concepts using ubiquitous language
7. Follow existing code style and conventions

## License

Copyright © 2024 Paklog. All rights reserved.
