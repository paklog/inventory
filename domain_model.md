
# Domain Model: Aggregates, Entities, and Value Objects

This diagram illustrates the core domain model of the inventory system, identifying the aggregates, entities, and value objects according to Domain-Driven Design principles.

- **Aggregates (`<<Aggregate>>`)**: `ProductStock` and `StockLocation` are the two main aggregates. They encapsulate business logic and ensure the consistency of the objects within their boundaries.
- **Entities (`<<Entity>>`)**: `PhysicalReservation` is an entity within the `StockLocation` aggregate. It has its own identity but its lifecycle is managed by `StockLocation`.
- **Value Objects (`<<ValueObject>>`)**: `StockLevel` and `Location` are value objects. They are immutable and their equality is based on their attributes, not an ID.

```mermaid
classDiagram
    direction LR

    class ProductStock {
        <<Aggregate>>
        -sku: String
        -lastUpdated: LocalDateTime
        +allocate(quantity)
        +deallocate(quantity)
        +adjustQuantityOnHand(change, reason)
        +receiveStock(quantity)
    }

    class StockLevel {
        <<ValueObject>>
        -quantityOnHand: int
        -quantityAllocated: int
        +getAvailableToPromise() int
    }

    class StockLocation {
        <<Aggregate>>
        -sku: String
        -quantity: int
        +addStock(quantity)
        +removeStock(quantity)
        +addPhysicalReservation(reservationId, quantity)
        +removePhysicalReservation(reservationId)
        +getAvailableToPick() int
    }

    class Location {
        <<ValueObject>>
        -aisle: String
        -shelf: String
        -bin: String
    }

    class PhysicalReservation {
        <<Entity>>
        -reservationId: String
        -quantity: int
    }

    class InventoryLedgerEntry {
        <<Aggregate>>
        -sku: String
        -quantityChange: int
        -reasonCode: String
        -timestamp: LocalDateTime
    }

    ProductStock "1" *-- "1" StockLevel : contains
    StockLocation "1" *-- "1" Location : located at
    StockLocation "1" *-- "0..*" PhysicalReservation : has
```
