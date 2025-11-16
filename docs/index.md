---
layout: default
title: Home
---

# Inventory Management Service Documentation

Inventory management service providing single source of truth for product stock levels with event-driven architecture, DDD, and transactional outbox pattern.

## Overview

The Inventory Management Service serves as the single source of truth for product stock levels across the Paklog fulfillment platform. This bounded context manages quantity on hand, allocations, reservations, and calculates available-to-promise (ATP) inventory.

## Quick Links

### Getting Started
- [Developer Onboarding Guide](developer-onboarding-guide.md) - Start here for new developers
- [API Documentation](api-documentation.md) - REST API reference
- [API Versioning Strategy](api-versioning-strategy.md) - Version management

### Architecture & Design
- [Domain-Driven Design](domain-driven-design.md) - DDD implementation details
- [Architecture Diagrams](architecture-diagrams.md) - System architecture
- [C4 Diagrams](architecture/c4-diagrams.md) - Detailed architectural views
- [Bounded Context Analysis](inventory-bounded-context-analysis.md) - Context mapping

### Architecture Decision Records
- [ADR-001: Hexagonal Architecture](architecture/adrs/ADR-001-hexagonal-architecture.md)
- [ADR-002: Event Sourcing Strategy](architecture/adrs/ADR-002-event-sourcing-strategy.md)
- [ADR-003: CloudEvents Adoption](architecture/adrs/ADR-003-cloudevents-adoption.md)
- [ADR-004: MongoDB Selection](architecture/adrs/ADR-004-mongodb-selection.md)
- [ADR-005: Kafka Event Streaming](architecture/adrs/ADR-005-kafka-event-streaming.md)

### Implementation Guides
- [Stock Status Implementation](implementation-guide-stock-status.md) - Stock status tracking
- [Phase 2 Testing](phase-2-testing-implementation.md) - Testing strategy

### Operations
- [Database Operations Runbook](runbooks/database-operations.md)
- [Incident Response Runbook](runbooks/incident-response.md)

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.2.5** - Application framework
- **MongoDB** - Document database
- **Apache Kafka** - Event streaming
- **CloudEvents 2.5.0** - Event standard
- **OpenTelemetry** - Distributed tracing

## Key Features

- Single source of truth for inventory
- Available-to-Promise (ATP) calculation
- Lot/batch management
- Serial number tracking
- Multi-location support
- Audit trail via ledger
- Event-driven integration
- Transactional outbox pattern

## Domain Model

### Aggregates
- **ProductStock** - Stock levels for a SKU
- **InventorySnapshot** - Point-in-time inventory state
- **Container** - Physical inventory containers
- **StockTransfer** - Inventory movements
- **AssemblyOrder** - Kit assembly operations
- **CycleCount** - Physical inventory counts

### Key Concepts
- **Quantity on Hand (QoH)**: Physical inventory
- **Allocated**: Committed to orders
- **Available to Promise (ATP)**: QoH - Allocated
- **Transactional Outbox**: Guaranteed event delivery

## Architecture Patterns

- **Hexagonal Architecture** - Clean separation of concerns
- **Domain-Driven Design** - Rich domain models
- **Event-Driven Architecture** - Async integration
- **CQRS** - Command/Query separation
- **Event Sourcing Lite** - Audit via immutable ledger

## Getting Started

1. Review the [Developer Onboarding Guide](developer-onboarding-guide.md)
2. Understand the [Domain-Driven Design](domain-driven-design.md) approach
3. Explore the [API Documentation](api-documentation.md)
4. Read relevant [Architecture Decision Records](architecture/adrs/)
5. Check the [Operations Runbooks](runbooks/) for production support

## Contributing

For contribution guidelines, please refer to the main [README](../README.md) in the project root.

## Support

- **GitHub Issues**: [Report bugs or request features](https://github.com/paklog/inventory/issues)
- **Documentation**: Browse the guides in the navigation menu
- **Runbooks**: See [Operations](runbooks/) for troubleshooting
