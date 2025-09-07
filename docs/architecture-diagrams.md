# Inventory Management Service - Architecture Diagrams

## 1. Domain Model - Detailed Class Diagram

```mermaid
classDiagram
    class ProductStock {
        <<Aggregate Root>>
        -String sku
        -int quantityOnHand
        -int quantityAllocated
        -LocalDateTime lastUpdated
        -List~DomainEvent~ uncommittedEvents
        +int getAvailableToPromise()
        +void allocate(int quantity)
        +void deallocate(int quantity) 
        +void adjustQuantityOnHand(int change, String reason)
        +void receiveStock(int quantity)
        +boolean canAllocate(int quantity)
        +List~DomainEvent~ getUncommittedEvents()
        +void markEventsAsCommitted()
        -void addEvent(DomainEvent event)
        -void validateInvariants()
    }
    
    class InventoryLedgerEntry {
        <<Entity>>
        -String id
        -String sku
        -LocalDateTime timestamp
        -int quantityChange
        -ChangeType changeType
        -String sourceReference
        -String reason
        -String operatorId
        +static InventoryLedgerEntry forAllocation(String sku, int quantity, String orderId)
        +static InventoryLedgerEntry forPick(String sku, int quantity, String orderId)
        +static InventoryLedgerEntry forAdjustment(String sku, int quantity, String reason, String operatorId)
        +static InventoryLedgerEntry forReceipt(String sku, int quantity, String receiptId)
    }
    
    class OutboxEvent {
        <<Entity>>
        -String id
        -String aggregateId
        -String eventType
        -String eventData
        -LocalDateTime createdAt
        -boolean processed
        -LocalDateTime processedAt
        +static OutboxEvent from(DomainEvent event)
        +void markAsProcessed()
    }
    
    class DomainEvent {
        <<Abstract>>
        -String eventId
        -String aggregateId
        -LocalDateTime occurredOn
        +String getEventType()
        +Map~String,Object~ getEventData()
    }
    
    class StockLevelChangedEvent {
        -String sku
        -StockLevel previousLevel
        -StockLevel newLevel
        -String changeReason
        +StockLevelChangedEvent(String sku, StockLevel previous, StockLevel current, String reason)
    }
    
    class StockLevel {
        <<Value Object>>
        -int quantityOnHand
        -int quantityAllocated
        +int getAvailableToPromise()
        +StockLevel withAllocation(int quantity)
        +StockLevel withDeallocation(int quantity)
        +StockLevel withQuantityChange(int change)
    }
    
    class ChangeType {
        <<Enumeration>>
        ALLOCATION
        DEALLOCATION
        PICK
        RECEIPT
        ADJUSTMENT_POSITIVE
        ADJUSTMENT_NEGATIVE
        CYCLE_COUNT
    }
    
    ProductStock --> StockLevel : contains
    ProductStock --> DomainEvent : publishes
    ProductStock --> InventoryLedgerEntry : creates audit trail
    DomainEvent <|-- StockLevelChangedEvent
    InventoryLedgerEntry --> ChangeType
    OutboxEvent --> DomainEvent : serializes
    StockLevelChangedEvent --> StockLevel : contains previous/new
```

## 2. Hexagonal Architecture - Detailed Layer Diagram

```mermaid
graph TB
    subgraph "External Actors"
        USER[Inventory Manager]
        WH[Warehouse Operations]
        SC[Sales Channel]
        ANALYST[Data Analyst]
    end
    
    subgraph "Infrastructure Layer (Adapters)"
        subgraph "Inbound Adapters"
            REST[REST Controllers]
            KAFKA_IN[Kafka Event Consumers]
        end
        
        subgraph "Outbound Adapters"
            MONGO[MongoDB Repositories]
            KAFKA_OUT[Kafka Event Publishers]
            OUTBOX_PUB[Outbox Event Publisher]
        end
    end
    
    subgraph "Application Layer (Use Cases)"
        subgraph "Command Side"
            CMD_SVC[Inventory Command Service]
            CMD_HANDLERS[Command Handlers]
        end
        
        subgraph "Query Side"
            QRY_SVC[Inventory Query Service]
            QRY_HANDLERS[Query Handlers]
        end
        
        subgraph "Event Handling"
            EVT_HANDLER[Event Handlers]
            EVT_PUBLISHER[Event Publisher Port]
        end
    end
    
    subgraph "Domain Layer (Core Business Logic)"
        subgraph "Aggregates"
            PS[ProductStock Aggregate]
        end
        
        subgraph "Domain Services"
            DOM_SVC[Inventory Domain Service]
        end
        
        subgraph "Repository Interfaces (Ports)"
            PS_REPO_PORT[ProductStock Repository Port]
            LEDGER_REPO_PORT[Ledger Repository Port]
            OUTBOX_REPO_PORT[Outbox Repository Port]
        end
    end
    
    subgraph "External Systems"
        DB[(MongoDB Database)]
        KAFKA_CLUSTER[Kafka Cluster]
    end
    
    %% Connections
    USER --> REST
    WH --> KAFKA_IN
    ANALYST --> REST
    
    REST --> CMD_SVC
    REST --> QRY_SVC
    KAFKA_IN --> EVT_HANDLER
    
    CMD_SVC --> PS
    QRY_SVC --> PS_REPO_PORT
    EVT_HANDLER --> CMD_SVC
    EVT_PUBLISHER --> KAFKA_OUT
    
    PS --> DOM_SVC
    PS --> PS_REPO_PORT
    CMD_SVC --> LEDGER_REPO_PORT
    CMD_SVC --> OUTBOX_REPO_PORT
    
    PS_REPO_PORT -.-> MONGO
    LEDGER_REPO_PORT -.-> MONGO
    OUTBOX_REPO_PORT -.-> MONGO
    OUTBOX_PUB --> KAFKA_OUT
    
    MONGO --> DB
    KAFKA_OUT --> KAFKA_CLUSTER
    KAFKA_CLUSTER --> SC
```

## 3. Event Choreography Flow

```mermaid
sequenceDiagram
    participant WMS as Warehouse Management
    participant KIN as Kafka (Inbound)
    participant EVT as Event Handler
    participant CMD as Command Service
    participant AGG as ProductStock Aggregate
    participant DOM as Domain Service
    participant REPO as Repository
    participant OUTBOX as Outbox Repository
    participant KOUT as Kafka (Outbound)
    participant SC as Sales Channel
    
    Note over WMS, SC: Inventory Allocation Scenario
    
    WMS->>+KIN: InventoryAllocationRequested Event
    KIN->>+EVT: Consume Event
    EVT->>+CMD: ProcessAllocationCommand(sku, quantity, orderId)
    CMD->>+REPO: findBySku(sku)
    REPO-->>-CMD: ProductStock
    CMD->>+AGG: allocate(quantity)
    
    AGG->>AGG: validateCanAllocate(quantity)
    AGG->>AGG: quantityAllocated += quantity
    AGG->>AGG: addEvent(StockLevelChangedEvent)
    AGG-->>-CMD: success
    
    CMD->>+DOM: createLedgerEntry(ALLOCATION)
    DOM-->>-CMD: InventoryLedgerEntry
    
    par Save Aggregate and Create Outbox Event
        CMD->>+REPO: save(productStock)
        REPO-->>-CMD: saved
    and
        CMD->>+OUTBOX: save(OutboxEvent.from(domainEvent))
        OUTBOX-->>-CMD: saved
    end
    
    Note over OUTBOX, KOUT: Async Outbox Processing
    
    loop Every 5 seconds
        KOUT->>+OUTBOX: findUnprocessedEvents()
        OUTBOX-->>-KOUT: List<OutboxEvent>
        KOUT->>+KOUT: publishToKafka(events)
        KOUT->>+OUTBOX: markAsProcessed(eventIds)
        OUTBOX-->>-KOUT: updated
    end
    
    KOUT->>SC: StockLevelChanged Event
```

## 4. Item Picking Event Flow

```mermaid
sequenceDiagram
    participant WMS as Warehouse Management
    participant KIN as Kafka (Inbound)
    participant EVT as Event Handler
    participant CMD as Command Service
    participant AGG as ProductStock Aggregate
    participant AUDIT as Audit Service
    participant REPO as Repository
    participant OUTBOX as Outbox Repository
    participant KOUT as Kafka (Outbound)
    
    Note over WMS, KOUT: Item Picked Scenario
    
    WMS->>+KIN: ItemPicked Event (sku, quantity, orderId)
    KIN->>+EVT: Consume Event
    EVT->>+CMD: ProcessItemPickedCommand(sku, quantity, orderId)
    
    CMD->>+REPO: findBySku(sku)
    REPO-->>-CMD: ProductStock
    
    par Update Stock Levels
        CMD->>+AGG: deallocate(quantity)
        AGG->>AGG: quantityAllocated -= quantity
        AGG-->>-CMD: success
    and
        CMD->>+AGG: adjustQuantityOnHand(-quantity, "PICKED")
        AGG->>AGG: quantityOnHand -= quantity
        AGG->>AGG: addEvent(StockLevelChangedEvent)
        AGG-->>-CMD: success
    end
    
    CMD->>+AUDIT: createLedgerEntry(PICK, sku, -quantity, orderId)
    AUDIT-->>-CMD: InventoryLedgerEntry
    
    par Transaction - Save All Changes
        CMD->>+REPO: save(productStock)
        REPO-->>-CMD: saved
    and
        CMD->>+REPO: save(ledgerEntry)
        REPO-->>-CMD: saved
    and
        CMD->>+OUTBOX: save(OutboxEvent.from(domainEvent))
        OUTBOX-->>-CMD: saved
    end
    
    CMD-->>-EVT: success
    EVT-->>-KIN: ack
    
    Note over OUTBOX, KOUT: Async Event Publishing
    KOUT->>OUTBOX: Poll for events
    KOUT->>KOUT: Publish StockLevelChanged
```

## 5. ProductStock Aggregate State Transitions

```mermaid
stateDiagram-v2
    [*] --> NotExists: SKU doesn't exist
    
    NotExists --> Available: receiveStock(quantity > 0)
    
    state Available {
        [*] --> InStock: quantity_on_hand > 0
        InStock --> PartiallyAllocated: allocate(quantity)
        PartiallyAllocated --> FullyAllocated: allocate(remaining_qty)
        PartiallyAllocated --> InStock: deallocate(quantity)
        FullyAllocated --> PartiallyAllocated: deallocate(quantity)
        
        InStock --> OutOfStock: adjustQuantityOnHand(-all)
        PartiallyAllocated --> OutOfStock: adjustQuantityOnHand(-all)
        FullyAllocated --> OutOfStock: adjustQuantityOnHand(-all)
        
        OutOfStock --> InStock: receiveStock(quantity)
        OutOfStock --> PartiallyAllocated: receiveStock(quantity) + has_allocations
    }
    
    Available --> ZeroStock: quantity_on_hand = 0 AND quantity_allocated = 0
    ZeroStock --> Available: receiveStock(quantity > 0)
    
    note right of Available
        Business Rules:
        - available_to_promise = quantity_on_hand - quantity_allocated
        - available_to_promise >= 0 (invariant)
        - quantity_allocated <= quantity_on_hand (invariant)
    end note
```

## 6. Data Access Pattern - Repository Layer

```mermaid
graph TB
    subgraph "Application Layer"
        CS[Command Service]
        QS[Query Service]
    end
    
    subgraph "Domain Layer (Ports)"
        PSR_PORT[ProductStockRepository Interface]
        LR_PORT[LedgerRepository Interface] 
        OR_PORT[OutboxRepository Interface]
    end
    
    subgraph "Infrastructure Layer (Adapters)"
        subgraph "MongoDB Adapters"
            PSR_IMPL[ProductStockRepositoryImpl]
            LR_IMPL[LedgerRepositoryImpl]
            OR_IMPL[OutboxRepositoryImpl]
        end
        
        subgraph "MongoDB Documents"
            PS_DOC[ProductStockDocument]
            L_DOC[LedgerEntryDocument]
            O_DOC[OutboxEventDocument]
        end
        
        subgraph "Spring Data"
            PS_SPRING[ProductStockSpringRepository]
            L_SPRING[LedgerSpringRepository]
            O_SPRING[OutboxSpringRepository]
        end
    end
    
    subgraph "Database"
        MONGO[(MongoDB)]
    end
    
    %% Application to Domain
    CS --> PSR_PORT
    CS --> LR_PORT
    CS --> OR_PORT
    QS --> PSR_PORT
    QS --> LR_PORT
    
    %% Port Implementations
    PSR_PORT -.-> PSR_IMPL
    LR_PORT -.-> LR_IMPL
    OR_PORT -.-> OR_IMPL
    
    %% Repository Implementations
    PSR_IMPL --> PS_DOC
    PSR_IMPL --> PS_SPRING
    LR_IMPL --> L_DOC
    LR_IMPL --> L_SPRING
    OR_IMPL --> O_DOC
    OR_IMPL --> O_SPRING
    
    %% Spring Data to MongoDB
    PS_SPRING --> MONGO
    L_SPRING --> MONGO
    O_SPRING --> MONGO
    
    note right of PSR_IMPL
        Repository Implementation:
        - Domain model ↔ Document mapping
        - Exception translation
        - Transaction management
    end note
```

## 7. Event Publishing Architecture - Transactional Outbox

```mermaid
graph TB
    subgraph "Transaction Boundary"
        subgraph "Business Operation"
            AGG[ProductStock Aggregate]
            DOMAIN_EVENT[Domain Event Creation]
        end
        
        subgraph "Persistence Layer"
            AGG_SAVE[Save Aggregate]
            OUTBOX_SAVE[Save Outbox Event]
        end
    end
    
    subgraph "Event Publishing (Separate Process)"
        SCHEDULER[Scheduled Task @5s intervals]
        OUTBOX_READER[Outbox Event Reader]
        EVENT_PUBLISHER[CloudEvent Publisher]
        KAFKA_PRODUCER[Kafka Producer]
        OUTBOX_UPDATER[Mark Events as Processed]
    end
    
    subgraph "External Systems"
        KAFKA_TOPIC[fulfillment.inventory.v1.events]
        CONSUMERS[Downstream Consumers]
    end
    
    subgraph "Database"
        MONGO[(MongoDB)]
        subgraph "Collections"
            PS_COLL[product_stocks]
            OUTBOX_COLL[outbox_events]
            LEDGER_COLL[inventory_ledger]
        end
    end
    
    %% Business flow
    AGG --> DOMAIN_EVENT
    DOMAIN_EVENT --> AGG_SAVE
    DOMAIN_EVENT --> OUTBOX_SAVE
    
    %% Persistence
    AGG_SAVE --> PS_COLL
    OUTBOX_SAVE --> OUTBOX_COLL
    
    %% Event publishing flow
    SCHEDULER --> OUTBOX_READER
    OUTBOX_READER --> OUTBOX_COLL
    OUTBOX_READER --> EVENT_PUBLISHER
    EVENT_PUBLISHER --> KAFKA_PRODUCER
    KAFKA_PRODUCER --> KAFKA_TOPIC
    KAFKA_TOPIC --> CONSUMERS
    EVENT_PUBLISHER --> OUTBOX_UPDATER
    OUTBOX_UPDATER --> OUTBOX_COLL
    
    note right of SCHEDULER
        Guarantees:
        - At-least-once delivery
        - Event ordering per aggregate
        - No event loss on system failure
    end note
```

## 8. API Contract Flow - REST Endpoints

```mermaid
sequenceDiagram
    participant CLIENT as Client
    participant CTRL as InventoryController
    participant CMD as Command Service
    participant QRY as Query Service
    participant AGG as ProductStock Aggregate
    participant REPO as Repository
    
    Note over CLIENT, REPO: Stock Level Query (GET /stock_levels/{sku})
    
    CLIENT->>+CTRL: GET /inventory/stock_levels/ABC123
    CTRL->>+QRY: getStockLevel("ABC123")
    QRY->>+REPO: findBySku("ABC123")
    REPO-->>-QRY: Optional<ProductStock>
    
    alt Stock Found
        QRY->>QRY: map to StockLevelResponse
        QRY-->>CTRL: StockLevelResponse
        CTRL-->>CLIENT: 200 OK + StockLevel
    else Stock Not Found
        QRY-->>CTRL: empty
        CTRL-->>CLIENT: 404 Not Found
    end
    
    Note over CLIENT, REPO: Stock Adjustment (POST /adjustments)
    
    CLIENT->>+CTRL: POST /inventory/adjustments
    Note right of CLIENT: {sku: "ABC123", quantity_change: 10, reason_code: "found"}
    
    CTRL->>CTRL: validate request
    CTRL->>+CMD: adjustStock(AdjustStockCommand)
    CMD->>+REPO: findBySku("ABC123")
    REPO-->>-CMD: ProductStock
    
    CMD->>+AGG: adjustQuantityOnHand(10, "found")
    AGG->>AGG: validate business rules
    AGG->>AGG: apply quantity change
    AGG->>AGG: create domain event
    AGG-->>-CMD: success
    
    par Save Changes
        CMD->>REPO: save(productStock)
    and
        CMD->>REPO: save(ledgerEntry) 
    and
        CMD->>REPO: save(outboxEvent)
    end
    
    CMD-->>-CTRL: success
    CTRL-->>-CLIENT: 202 Accepted
```

## 9. Error Handling and Resilience Patterns

```mermaid
graph TB
    subgraph "Error Handling Strategy"
        subgraph "Validation Errors"
            DOMAIN_VAL[Domain Validation]
            REQUEST_VAL[Request Validation]
        end
        
        subgraph "Infrastructure Errors"
            DB_ERROR[Database Connection]
            KAFKA_ERROR[Kafka Connection]
            TIMEOUT[Service Timeout]
        end
        
        subgraph "Business Rule Violations"
            INSUFFICIENT_STOCK[Insufficient Stock]
            NEGATIVE_STOCK[Negative Stock Invariant]
            INVALID_SKU[Invalid SKU Format]
        end
    end
    
    subgraph "Resilience Patterns"
        RETRY[Exponential Backoff Retry]
        CIRCUIT_BREAKER[Circuit Breaker]
        DEAD_LETTER[Dead Letter Queue]
        HEALTH_CHECK[Health Checks]
    end
    
    subgraph "Response Handling"
        ERROR_RESPONSE[Standardized Error Response]
        CORRELATION[Correlation ID Tracking]
        LOGGING[Structured Logging]
        METRICS[Error Metrics]
    end
    
    DOMAIN_VAL --> ERROR_RESPONSE
    REQUEST_VAL --> ERROR_RESPONSE
    DB_ERROR --> RETRY
    KAFKA_ERROR --> CIRCUIT_BREAKER
    TIMEOUT --> RETRY
    
    INSUFFICIENT_STOCK --> ERROR_RESPONSE
    NEGATIVE_STOCK --> ERROR_RESPONSE
    INVALID_SKU --> ERROR_RESPONSE
    
    RETRY --> DEAD_LETTER
    CIRCUIT_BREAKER --> HEALTH_CHECK
    ERROR_RESPONSE --> CORRELATION
    CORRELATION --> LOGGING
    LOGGING --> METRICS
```

## 10. Deployment Architecture

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[Application Load Balancer]
    end
    
    subgraph "Application Tier (Kubernetes)"
        subgraph "Inventory Service Pods"
            POD1[inventory-service-1]
            POD2[inventory-service-2] 
            POD3[inventory-service-3]
        end
        
        subgraph "Configuration"
            CONFIG[ConfigMaps]
            SECRETS[Secrets]
        end
    end
    
    subgraph "Data Tier"
        subgraph "MongoDB Cluster"
            MONGO_PRIMARY[(MongoDB Primary)]
            MONGO_SECONDARY1[(MongoDB Secondary)]
            MONGO_SECONDARY2[(MongoDB Secondary)]
        end
        
        subgraph "Kafka Cluster"
            KAFKA_BROKER1[Kafka Broker 1]
            KAFKA_BROKER2[Kafka Broker 2] 
            KAFKA_BROKER3[Kafka Broker 3]
            ZOOKEEPER[Zookeeper Ensemble]
        end
    end
    
    subgraph "Monitoring & Observability"
        PROMETHEUS[Prometheus]
        GRAFANA[Grafana]
        JAEGER[Jaeger Tracing]
        ELK[ELK Stack]
    end
    
    LB --> POD1
    LB --> POD2
    LB --> POD3
    
    POD1 --> CONFIG
    POD1 --> SECRETS
    POD2 --> CONFIG
    POD2 --> SECRETS
    POD3 --> CONFIG
    POD3 --> SECRETS
    
    POD1 --> MONGO_PRIMARY
    POD2 --> MONGO_PRIMARY
    POD3 --> MONGO_PRIMARY
    
    POD1 --> KAFKA_BROKER1
    POD2 --> KAFKA_BROKER2
    POD3 --> KAFKA_BROKER3
    
    KAFKA_BROKER1 --> ZOOKEEPER
    KAFKA_BROKER2 --> ZOOKEEPER
    KAFKA_BROKER3 --> ZOOKEEPER
    
    POD1 --> PROMETHEUS
    POD2 --> PROMETHEUS
    POD3 --> PROMETHEUS
    
    PROMETHEUS --> GRAFANA
    POD1 --> JAEGER
    POD1 --> ELK
```

These detailed diagrams provide comprehensive visual documentation for developers to understand:

1. **Domain Model Structure** - DDD aggregates, entities, and value objects
2. **Hexagonal Architecture** - Clear separation of concerns and dependency directions
3. **Event Choreography** - How events flow through the system
4. **State Management** - ProductStock aggregate lifecycle
5. **Data Access Patterns** - Repository implementation strategy
6. **Event Publishing** - Transactional outbox pattern implementation
7. **API Contracts** - REST endpoint interaction flows
8. **Error Handling** - Resilience and error management strategies
9. **Deployment Architecture** - Production infrastructure layout

Each diagram focuses on specific architectural concerns while maintaining consistency with the overall system design principles.