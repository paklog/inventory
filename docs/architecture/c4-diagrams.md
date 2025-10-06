# C4 Model Diagrams - Inventory Management Service

## Overview

The C4 model provides a way to describe and communicate software architecture at different levels of abstraction.

**Levels**:
1. **System Context**: How the system fits into the world
2. **Container**: High-level technology choices and responsibilities
3. **Component**: Components within a container
4. **Code**: How components are implemented (UML class diagrams)

---

## Level 1: System Context Diagram

Shows how the Inventory Service fits into the Paklog ecosystem.

```mermaid
graph TB
    subgraph "External Systems"
        Marketplace[Marketplace APIs<br/>Amazon, eBay, Shopify]
        WMS[Warehouse Management<br/>System]
        ERP[ERP System<br/>SAP, Oracle]
    end

    subgraph "Users"
        WarehouseStaff[Warehouse Staff]
        InventoryManager[Inventory Manager]
        SystemIntegrator[System Integrator]
    end

    subgraph "Paklog Platform"
        InventoryService[Inventory Management<br/>Service<br/><br/>Tracks stock levels,<br/>allocations, and<br/>inventory movements]
        OrderService[Order Management<br/>Service]
        FulfillmentService[Fulfillment<br/>Service]
        WarehouseOps[Warehouse Operations<br/>Service]
    end

    %% User interactions
    WarehouseStaff -->|View stock levels| InventoryService
    InventoryManager -->|Adjust stock| InventoryService
    SystemIntegrator -->|API integration| InventoryService

    %% Service interactions
    OrderService -->|Check availability,<br/>allocate stock| InventoryService
    FulfillmentService -->|Update stock<br/>after pick/ship| InventoryService
    WarehouseOps -->|Receive stock,<br/>move inventory| InventoryService

    %% External integrations
    InventoryService -->|Publish inventory<br/>events| Marketplace
    WMS -->|Sync physical<br/>inventory| InventoryService
    ERP -->|Valuation and<br/>cost updates| InventoryService

    style InventoryService fill:#2563eb,stroke:#1e40af,stroke-width:4px,color:#fff
```

**Key Relationships**:
- **Users**: Warehouse staff view stock, inventory managers adjust stock
- **Internal Services**: Order service allocates stock, fulfillment updates stock
- **External Systems**: WMS syncs physical inventory, ERP provides cost data

---

## Level 2: Container Diagram

Shows the containers (applications/services) within the Inventory Service.

```mermaid
graph TB
    subgraph "External"
        OrderService[Order Service]
        FulfillmentService[Fulfillment Service]
        ExternalSystems[External Systems]
    end

    subgraph "Inventory Management Service"
        subgraph "Application Layer"
            WebAPI[REST API<br/>Spring Boot<br/>Port 8085]
        end

        subgraph "Infrastructure"
            MongoDB[(MongoDB<br/>Inventory Data)]
            Kafka[Kafka<br/>Event Bus]
            Redis[(Redis<br/>Cache)]
        end

        subgraph "Background Jobs"
            OutboxPublisher[Outbox Event<br/>Publisher<br/>Scheduled @5s]
            MetricsCollector[Metrics<br/>Collector<br/>Async Processing]
        end

        subgraph "Observability"
            Prometheus[Prometheus<br/>Metrics]
            Jaeger[Jaeger<br/>Distributed Tracing]
        end
    end

    %% External to API
    OrderService -->|HTTP/REST| WebAPI
    FulfillmentService -->|HTTP/REST| WebAPI
    ExternalSystems -->|HTTP/REST| WebAPI

    %% API to Infrastructure
    WebAPI -->|Read/Write| MongoDB
    WebAPI -->|Query cache| Redis
    WebAPI -->|Write to outbox| MongoDB

    %% Background jobs
    OutboxPublisher -->|Poll outbox| MongoDB
    OutboxPublisher -->|Publish events| Kafka
    MetricsCollector -->|Record metrics| Prometheus

    %% Observability
    WebAPI -->|Send traces| Jaeger
    WebAPI -->|Export metrics| Prometheus

    %% Event consumers
    Kafka -->|Consume events| OrderService
    Kafka -->|Consume events| FulfillmentService

    style WebAPI fill:#2563eb,stroke:#1e40af,stroke-width:3px,color:#fff
    style MongoDB fill:#059669,stroke:#047857,stroke-width:2px,color:#fff
    style Kafka fill:#dc2626,stroke:#b91c1c,stroke-width:2px,color:#fff
    style Redis fill:#7c3aed,stroke:#6d28d9,stroke-width:2px,color:#fff
```

**Key Containers**:
- **REST API**: Spring Boot application handling HTTP requests
- **MongoDB**: Primary data store for inventory data
- **Kafka**: Event streaming platform for pub/sub
- **Redis**: Distributed cache for performance
- **Background Jobs**: Outbox publisher, metrics collection
- **Observability**: Prometheus metrics, Jaeger tracing

---

## Level 3: Component Diagram

Shows the components within the REST API container.

```mermaid
graph TB
    subgraph "Infrastructure Layer"
        subgraph "Web"
            Controller[Inventory Controller<br/>@RestController]
            ExceptionHandler[Global Exception<br/>Handler]
        end

        subgraph "Persistence"
            ProductStockRepoImpl[ProductStock<br/>Repository Impl]
            LedgerRepoImpl[Ledger<br/>Repository Impl]
            OutboxRepoImpl[Outbox<br/>Repository Impl]
        end

        subgraph "Messaging"
            EventPublisher[Kafka Event<br/>Publisher]
        end

        subgraph "Cache"
            CacheConfig[Cache<br/>Configuration<br/>L1: Caffeine<br/>L2: Redis]
        end
    end

    subgraph "Application Layer"
        CommandService[Inventory Command<br/>Service]
        QueryService[Inventory Query<br/>Service]
        BulkService[Bulk Allocation<br/>Service]
    end

    subgraph "Domain Layer"
        ProductStock[ProductStock<br/>Aggregate]
        StockLevel[StockLevel<br/>Value Object]
        LedgerEntry[Ledger Entry<br/>Entity]
        RepoInterface[Repository<br/>Interfaces<br/>Ports]
        DomainEvents[Domain Events]
    end

    subgraph "External"
        MongoDB[(MongoDB)]
        Kafka[Kafka]
        Redis[(Redis)]
    end

    %% HTTP Requests
    Controller --> CommandService
    Controller --> QueryService
    Controller --> BulkService

    %% Application to Domain
    CommandService --> ProductStock
    CommandService --> RepoInterface
    QueryService --> RepoInterface
    BulkService --> ProductStock

    %% Domain to Infrastructure
    RepoInterface -.implements.- ProductStockRepoImpl
    RepoInterface -.implements.- LedgerRepoImpl
    RepoInterface -.implements.- OutboxRepoImpl

    %% Infrastructure to External
    ProductStockRepoImpl --> MongoDB
    LedgerRepoImpl --> MongoDB
    OutboxRepoImpl --> MongoDB
    EventPublisher --> Kafka
    CacheConfig --> Redis

    %% Events
    ProductStock --> DomainEvents
    DomainEvents --> OutboxRepoImpl

    %% Caching
    QueryService --> CacheConfig

    style ProductStock fill:#2563eb,stroke:#1e40af,stroke-width:3px,color:#fff
    style CommandService fill:#059669,stroke:#047857,stroke-width:2px,color:#fff
    style QueryService fill:#059669,stroke:#047857,stroke-width:2px,color:#fff
```

**Key Components**:

**Infrastructure Layer**:
- **Controllers**: HTTP endpoint handling
- **Repository Implementations**: MongoDB adapters
- **Event Publisher**: Kafka producer
- **Cache Configuration**: Multi-tier caching

**Application Layer**:
- **Command Service**: Write operations (adjustments, allocations)
- **Query Service**: Read operations (stock levels, health metrics)
- **Bulk Service**: High-performance bulk operations

**Domain Layer**:
- **Product Stock**: Core aggregate
- **Stock Level**: Value object
- **Repository Interfaces**: Ports for persistence
- **Domain Events**: Business events

---

## Level 4: Code Diagram - ProductStock Aggregate

UML class diagram showing the ProductStock aggregate structure.

```mermaid
classDiagram
    class ProductStock {
        -String sku
        -StockLevel stockLevel
        -Location location
        -List~LotBatch~ lotBatches
        -List~SerialNumber~ serialNumbers
        -List~InventoryHold~ holds
        -ABCClassification abcClassification
        -InventoryValuation valuation
        -List~DomainEvent~ uncommittedEvents

        +adjustQuantityOnHand(int quantity, String reasonCode)
        +allocate(int quantity)
        +deallocate(int quantity)
        +receiveStock(int quantity)
        +placeHold(InventoryHold hold)
        +releaseHold(String holdId)
        +getAvailableToPromise() int
        +getUncommittedEvents() List~DomainEvent~
        +markEventsAsCommitted()
    }

    class StockLevel {
        <<Value Object>>
        -int quantityOnHand
        -int quantityAllocated

        +getAvailable() int
        +adjustOnHand(int quantity) StockLevel
        +allocate(int quantity) StockLevel
        +deallocate(int quantity) StockLevel
    }

    class Location {
        <<Value Object>>
        -String warehouseId
        -String zone
        -String aisle
        -String bin

        +getLocationCode() String
    }

    class LotBatch {
        <<Entity>>
        -String lotNumber
        -int quantity
        -LocalDate expirationDate
        -LocalDate receivedDate
        -BatchStatus status

        +isExpired() boolean
        +isActive() boolean
    }

    class SerialNumber {
        <<Entity>>
        -String serialNumber
        -SerialStatus status
        -String assignedOrderId

        +assign(String orderId)
        +release()
    }

    class InventoryHold {
        <<Value Object>>
        -String holdId
        -HoldType holdType
        -int quantity
        -LocalDateTime placedAt
        -LocalDateTime expiresAt
        -String reason

        +isExpired() boolean
        +isActive() boolean
    }

    class ABCClassification {
        <<Value Object>>
        -ABCClass abcClass
        -LocalDateTime lastCalculatedAt
        -double annualDollarUsage
    }

    class InventoryValuation {
        <<Value Object>>
        -ValuationMethod method
        -BigDecimal unitCost
        -BigDecimal totalValue

        +calculateValue(int quantity) BigDecimal
    }

    class DomainEvent {
        <<Interface>>
        +getEventType() String
        +getAggregateId() String
        +getOccurredAt() LocalDateTime
    }

    class StockLevelChangedEvent {
        -String sku
        -int previousQuantityOnHand
        -int newQuantityOnHand
        -ChangeType changeType
        -String reasonCode
    }

    ProductStock "1" *-- "1" StockLevel
    ProductStock "1" *-- "1" Location
    ProductStock "1" *-- "0..*" LotBatch
    ProductStock "1" *-- "0..*" SerialNumber
    ProductStock "1" *-- "0..*" InventoryHold
    ProductStock "1" *-- "0..1" ABCClassification
    ProductStock "1" *-- "0..1" InventoryValuation
    ProductStock "1" --> "0..*" DomainEvent
    DomainEvent <|-- StockLevelChangedEvent
```

**Key Design Patterns**:
- **Aggregate Pattern**: ProductStock is the aggregate root
- **Value Objects**: Immutable objects like StockLevel, Location
- **Entity Pattern**: LotBatch and SerialNumber have identity
- **Domain Events**: ProductStock generates events on state changes

---

## Deployment Diagram

Shows how the system is deployed in production.

```mermaid
graph TB
    subgraph "AWS Cloud"
        subgraph "Load Balancing"
            ALB[Application Load<br/>Balancer]
        end

        subgraph "ECS Cluster"
            Service1[Inventory Service<br/>Instance 1]
            Service2[Inventory Service<br/>Instance 2]
            Service3[Inventory Service<br/>Instance 3]
        end

        subgraph "Data Layer"
            MongoDB[(MongoDB Atlas<br/>Replica Set<br/>Primary + 2 Secondaries)]
            MSK[AWS MSK<br/>Kafka Cluster<br/>3 Brokers]
            ElastiCache[(ElastiCache<br/>Redis Cluster)]
        end

        subgraph "Observability"
            CloudWatch[CloudWatch<br/>Logs & Metrics]
            XRay[AWS X-Ray<br/>Distributed Tracing]
        end
    end

    subgraph "External"
        Users[Users/Services]
    end

    Users -->|HTTPS| ALB
    ALB --> Service1
    ALB --> Service2
    ALB --> Service3

    Service1 --> MongoDB
    Service2 --> MongoDB
    Service3 --> MongoDB

    Service1 --> MSK
    Service2 --> MSK
    Service3 --> MSK

    Service1 --> ElastiCache
    Service2 --> ElastiCache
    Service3 --> ElastiCache

    Service1 --> CloudWatch
    Service2 --> CloudWatch
    Service3 --> CloudWatch

    Service1 --> XRay
    Service2 --> XRay
    Service3 --> XRay

    style Service1 fill:#2563eb,stroke:#1e40af,stroke-width:2px,color:#fff
    style Service2 fill:#2563eb,stroke:#1e40af,stroke-width:2px,color:#fff
    style Service3 fill:#2563eb,stroke:#1e40af,stroke-width:2px,color:#fff
    style MongoDB fill:#059669,stroke:#047857,stroke-width:2px,color:#fff
    style MSK fill:#dc2626,stroke:#b91c1c,stroke-width:2px,color:#fff
    style ElastiCache fill:#7c3aed,stroke:#6d28d9,stroke-width:2px,color:#fff
```

**Deployment Characteristics**:
- **High Availability**: 3 service instances across availability zones
- **Managed Services**: MongoDB Atlas, AWS MSK, ElastiCache
- **Auto-scaling**: ECS service scales based on CPU/memory
- **Load Distribution**: ALB distributes traffic across instances

---

## Data Flow Diagram

Shows how data flows through the system for a stock adjustment.

```mermaid
sequenceDiagram
    participant User
    participant Controller
    participant CommandService
    participant ProductStock
    participant Repository
    participant MongoDB
    participant OutboxRepo
    participant Cache
    participant OutboxPublisher
    participant Kafka

    User->>Controller: PATCH /stock_levels/{sku}
    Controller->>CommandService: adjustStock(sku, quantity, reason)

    activate CommandService
    CommandService->>Repository: findBySku(sku)
    Repository->>MongoDB: db.product_stock.findOne({sku})
    MongoDB-->>Repository: ProductStockDocument
    Repository-->>CommandService: ProductStock

    CommandService->>ProductStock: adjustQuantityOnHand(quantity, reason)
    activate ProductStock
    ProductStock->>ProductStock: Generate StockLevelChangedEvent
    ProductStock-->>CommandService: ProductStock (modified)
    deactivate ProductStock

    CommandService->>Repository: save(productStock)
    Repository->>MongoDB: db.product_stock.save()

    CommandService->>ProductStock: getUncommittedEvents()
    ProductStock-->>CommandService: List<DomainEvent>

    CommandService->>OutboxRepo: saveAll(outboxEvents)
    OutboxRepo->>MongoDB: db.outbox_events.insertMany()

    CommandService->>ProductStock: markEventsAsCommitted()
    CommandService->>Cache: evictStockLevel(sku)
    Cache-->>CommandService: Evicted

    CommandService-->>Controller: ProductStock
    deactivate CommandService

    Controller-->>User: 200 OK StockLevelResponse

    Note over OutboxPublisher: Background Process (Every 5s)
    OutboxPublisher->>OutboxRepo: findUnpublished()
    OutboxRepo->>MongoDB: db.outbox_events.find({published: false})
    MongoDB-->>OutboxRepo: List<OutboxEvent>
    OutboxRepo-->>OutboxPublisher: List<OutboxEvent>

    OutboxPublisher->>Kafka: publish(CloudEvent)
    Kafka-->>OutboxPublisher: Ack

    OutboxPublisher->>OutboxRepo: markAsPublished()
    OutboxRepo->>MongoDB: db.outbox_events.update({published: true})
```

---

## References

- [C4 Model](https://c4model.com/)
- [Structurizr](https://structurizr.com/)
- [Mermaid Diagrams](https://mermaid.js.org/)

---

**Last Updated**: 2025-10-05
