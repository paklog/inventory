
# Application Services & Infrastructure

This diagram shows the application services and their relationship with the domain layer (repositories).

- **Application Services (`<<ApplicationService>>`)**: These services orchestrate the business logic. They use repositories to retrieve and store aggregates and call the domain objects to perform operations. `InventoryCommandService` and `InventoryQueryService` are examples.
- **Repositories (`<<Repository>>`)**: These are interfaces defined in the domain layer but implemented in the infrastructure layer. They provide an abstraction for persisting and retrieving aggregates.

```mermaid
classDiagram
    direction RL

    class InventoryCommandService {
        <<ApplicationService>>
        +adjustStock(sku, quantity, reason)
        +allocateStock(sku, quantity, orderId)
        +processItemPicked(sku, quantity, orderId)
        +receiveStock(sku, quantity, receiptId)
    }

    class PhysicalStockCommandService {
        <<ApplicationService>>
        +moveStock(sku, quantity, from, to)
        +pickStock(sku, quantity, location)
    }

    class InventoryQueryService {
        <<ApplicationService>>
        +getStockLevel(sku)
        +getInventoryHealth()
    }

    class ProductStockRepository {
        <<Repository>>
        +findBySku(sku) ProductStock
        +save(productStock) ProductStock
    }

    class StockLocationRepository {
        <<Repository>>
        +findBySkuAndLocation(sku, location) StockLocation
        +save(stockLocation) StockLocation
    }
    
    class InventoryLedgerRepository {
        <<Repository>>
        +save(ledgerEntry) InventoryLedgerEntry
    }

    InventoryCommandService ..> ProductStockRepository : uses
    InventoryCommandService ..> InventoryLedgerRepository : uses
    PhysicalStockCommandService ..> StockLocationRepository : uses
    InventoryQueryService ..> ProductStockRepository : uses
```
