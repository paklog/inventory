# ADR-001: Hexagonal Architecture (Ports and Adapters)

**Status**: Accepted
**Date**: 2025-10-05
**Deciders**: Architecture Team
**Technical Story**: Platform Architecture Initiative

## Context

The Inventory Management Service needs to be:
- **Testable**: Unit tests without infrastructure dependencies
- **Maintainable**: Clear separation of business logic from technical concerns
- **Evolvable**: Ability to change infrastructure without affecting core logic
- **Independent**: Domain logic doesn't depend on frameworks or databases

We need an architecture that promotes:
- Domain-Driven Design (DDD)
- Separation of Concerns
- Dependency Inversion
- Testability

## Decision

We will use **Hexagonal Architecture** (also known as Ports and Adapters) for the Inventory Management Service.

### Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   REST API   │  │   MongoDB    │  │    Kafka     │      │
│  │  (Adapter)   │  │   (Adapter)  │  │  (Adapter)   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         ↓                  ↓                  ↓              │
│  ┌──────────────────────────────────────────────────┐      │
│  │         Application Layer (Use Cases)             │      │
│  │    InventoryCommandService                        │      │
│  │    InventoryQueryService                          │      │
│  └──────────────┬──────────────────┬─────────────────┘      │
│                 │       (Ports)     │                        │
│                 ↓                   ↓                        │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Domain Layer (Core)                    │    │
│  │   ProductStock (Aggregate)                          │    │
│  │   StockLevel (Value Object)                         │    │
│  │   InventoryLedgerEntry (Entity)                     │    │
│  │   Repository Interfaces (Ports)                     │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.paklog.inventory
├── domain/                    # Core business logic
│   ├── model/                # Aggregates, Entities, Value Objects
│   │   ├── ProductStock.java
│   │   ├── StockLevel.java
│   │   └── Location.java
│   ├── repository/           # Repository interfaces (ports)
│   │   └── ProductStockRepository.java
│   ├── event/                # Domain events
│   └── exception/            # Domain exceptions
│
├── application/              # Use cases / Application services
│   ├── service/
│   │   ├── InventoryCommandService.java
│   │   └── InventoryQueryService.java
│   └── dto/                  # Data Transfer Objects
│
└── infrastructure/           # Technical implementation (adapters)
    ├── web/                  # REST controllers
    │   └── InventoryController.java
    ├── persistence/
    │   └── mongodb/          # MongoDB adapter
    │       ├── ProductStockRepositoryImpl.java
    │       └── ProductStockDocument.java
    └── messaging/
        └── kafka/            # Kafka adapter
            └── EventPublisher.java
```

### Key Principles

1. **Dependency Direction**: Always points inward (Infrastructure → Application → Domain)
2. **Domain Isolation**: Domain layer has NO dependencies on frameworks or infrastructure
3. **Interface Segregation**: Repositories defined as interfaces in domain layer
4. **Adapter Pattern**: Infrastructure implements domain interfaces

### Example: Repository Port

**Domain Layer (Port)**:
```java
package com.paklog.inventory.domain.repository;

public interface ProductStockRepository {
    Optional<ProductStock> findBySku(String sku);
    ProductStock save(ProductStock productStock);
}
```

**Infrastructure Layer (Adapter)**:
```java
package com.paklog.inventory.infrastructure.persistence.mongodb;

@Component
public class ProductStockRepositoryImpl implements ProductStockRepository {
    private final ProductStockSpringRepository springRepository;

    @Override
    public Optional<ProductStock> findBySku(String sku) {
        return springRepository.findBySku(sku)
            .map(ProductStockDocument::toDomain);
    }
}
```

## Consequences

### Positive

✅ **Testability**: Domain logic can be tested without infrastructure
- Unit tests use in-memory repositories
- No database required for domain tests
- Fast test execution

✅ **Flexibility**: Easy to swap infrastructure
- Change from MongoDB to PostgreSQL without touching domain
- Switch from REST to GraphQL without changing business logic
- Replace Kafka with RabbitMQ if needed

✅ **Maintainability**: Clear boundaries and responsibilities
- Domain logic is isolated and focused
- Changes in UI don't affect domain
- Database schema changes are localized

✅ **Domain Focus**: Business logic is first-class citizen
- Domain model is rich and expressive
- Business rules are explicit
- Ubiquitous language in code

### Negative

⚠️ **Complexity**: More layers and abstractions
- Steeper learning curve for new developers
- More boilerplate code (mappers, DTOs)
- Requires discipline to maintain boundaries

⚠️ **Mapping Overhead**: Data transformations between layers
- Domain → DTO mapping
- Domain → Document mapping
- Performance cost of transformations

⚠️ **Initial Development**: Slower initial development
- More files and classes to create
- More planning required upfront
- Higher initial cognitive load

## Alternatives Considered

### 1. Layered Architecture (Traditional N-Tier)
**Rejected because**:
- Dependencies flow downward (harder to test)
- Domain layer often depends on persistence layer
- Harder to swap infrastructure

### 2. Clean Architecture (Uncle Bob)
**Similar approach, not chosen because**:
- Hexagonal is simpler and more pragmatic
- Less rigid in layer definitions
- Better fit for DDD

### 3. Transaction Script Pattern
**Rejected because**:
- Anemic domain model
- Business logic scattered in services
- Not suitable for complex domain

## References

- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design by Vaughn Vernon](https://www.amazon.com/Implementing-Domain-Driven-Design-Vaughn-Vernon/dp/0321834577)

## Related ADRs

- ADR-002: Event Sourcing Strategy
- ADR-003: CloudEvents Adoption
- ADR-004: MongoDB Selection

---

**Last Updated**: 2025-10-05
**Review Date**: 2026-01-05
