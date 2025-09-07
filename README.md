# Inventory Management Service

This is an Inventory Management Service built with Spring Boot, leveraging Domain-Driven Design (DDD) and Hexagonal Architecture principles. It provides a single source of truth for product stock levels, integrates with external systems via event-driven mechanisms (Kafka and CloudEvents), and exposes REST APIs for inventory management and reporting.

## 🚀 Features

- **ProductStock Aggregate**: Manages `quantity_on_hand`, `quantity_allocated`, and calculates `available_to_promise`.
- **Event-Driven Integration**: Consumes `InventoryAllocationRequested` and `ItemPicked` events from Warehouse Operations, and publishes `StockLevelChanged` events to Sales Channels.
- **Transactional Outbox Pattern**: Guarantees atomicity between database transactions and event publishing.
- **REST API**:
    - `GET /inventory/stock_levels/{sku}`: Retrieve current stock levels for a SKU.
    - `POST /inventory/adjustments`: Perform manual stock adjustments.
    - `GET /inventory/reports/health`: Retrieve inventory health metrics (e.g., turnover, dead stock).
- **Audit Trail**: Maintains an immutable ledger of all stock changes.
- **Persistence**: Uses MongoDB for storing `ProductStock` aggregates, `InventoryLedgerEntry` records, and `OutboxEvent`s.
- **Build Tool**: Maven.
- **Containerization**: Docker.

## 🏗️ Architecture

The service follows a Hexagonal Architecture, separating the core domain logic from external concerns (databases, messaging, APIs).

- **Domain Layer**: Contains the core business logic, aggregates (`ProductStock`), entities (`InventoryLedgerEntry`, `OutboxEvent`), value objects (`StockLevel`, `ChangeType`), and repository interfaces (ports).
- **Application Layer**: Orchestrates domain operations, handles use cases (commands and queries), and defines application-specific DTOs and event publisher ports.
- **Infrastructure Layer**: Implements the ports defined in the domain and application layers. This includes REST controllers, Kafka consumers/publishers, and MongoDB repository implementations (adapters).

Detailed architectural diagrams can be found in [`docs/architecture-diagrams.md`](docs/architecture-diagrams.md).

## 🛠️ Technology Stack

- **Spring Boot**: Latest stable release (3.2.x)
- **Spring Data MongoDB**: For persistence.
- **Spring Kafka**: For event streaming.
- **CloudEvents Java SDK**: For standardized event format.
- **Maven**: Build automation tool.
- **Docker / Docker Compose**: For local development and containerization.
- **JUnit 5 & Testcontainers**: For comprehensive testing.

## ⚙️ Setup and Local Development

### Prerequisites

- Java 17 JDK
- Maven 3.x
- Docker and Docker Compose

### 1. Clone the Repository

```bash
git clone https://github.com/paklog/inventory-service.git
cd inventory-service
```

### 2. Build the Application

```bash
./mvnw clean install
```

### 3. Run with Docker Compose (MongoDB and Kafka)

The `docker-compose.yaml` file sets up MongoDB, Zookeeper, Kafka, and the Inventory Service itself.

```bash
docker-compose up --build
```

This command will:
- Build the `inventory-service` Docker image.
- Start MongoDB, Zookeeper, and Kafka containers.
- Start the `inventory-service` container, which will connect to MongoDB and Kafka.

Wait for all services to become healthy. You can check their status with `docker-compose ps`.

### 4. Access the Application

The application will be running on `http://localhost:8080`.

**API Endpoints:**

- **Get Stock Level:**
  `GET http://localhost:8080/inventory/stock_levels/{sku}`
  Example: `GET http://localhost:8080/inventory/stock_levels/TEST-SKU-001`

- **Create Stock Adjustment:**
  `POST http://localhost:8080/inventory/adjustments`
  Body (application/json):
  ```json
  {
    "sku": "TEST-SKU-001",
    "quantityChange": 10,
    "reasonCode": "CYCLE_COUNT",
    "comment": "Found 10 extra units during cycle count"
  }
  ```

- **Get Inventory Health Metrics:**
  `GET http://localhost:8080/inventory/reports/health`
  Example: `GET http://localhost:8080/inventory/reports/health?category=Electronics&startDate=2023-01-01`

### 5. Interacting with Kafka (Optional)

You can use Kafka command-line tools (from within the Kafka container) or a GUI client to interact with the topics:

- **Produce an `InventoryAllocationRequested` event:**
  (Example using `kafkacat` or similar tool, or a custom producer)
  Topic: `fulfillment.warehouse.v1.events`
  CloudEvent JSON payload:
  ```json
  {
    "specversion": "1.0",
    "type": "com.example.fulfillment.warehouse.inventory.allocation.requested",
    "source": "/warehouse-service",
    "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    "time": "2023-10-27T10:00:00Z",
    "datacontenttype": "application/json",
    "subject": "TEST-SKU-001",
    "data": {
      "sku": "TEST-SKU-001",
      "quantity": 5,
      "order_id": "ORDER-XYZ-001"
    }
  }
  ```

- **Consume `StockLevelChanged` events:**
  Topic: `fulfillment.inventory.v1.events`

## 🧪 Running Tests

```bash
./mvnw test
```

Integration tests use Testcontainers to spin up isolated MongoDB and Kafka instances, ensuring reliable and reproducible test environments.

## 🤝 Contributing

Contributions are welcome! Please follow the architectural guidelines (DDD, Hexagonal Architecture) and Spring best practices.

## 📄 License

[Specify your license here, e.g., MIT, Apache 2.0]