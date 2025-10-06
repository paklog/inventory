# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records for the Inventory Management Service.

## What is an ADR?

An Architecture Decision Record (ADR) documents an important architectural decision made for this project, along with its context and consequences.

## ADR Index

| ADR | Title | Status | Date | Summary |
|-----|-------|--------|------|---------|
| [ADR-001](./ADR-001-hexagonal-architecture.md) | Hexagonal Architecture | Accepted | 2025-10-05 | Use Hexagonal Architecture (Ports and Adapters) for clear separation between domain logic and infrastructure |
| [ADR-002](./ADR-002-event-sourcing-strategy.md) | Event Sourcing Strategy | Accepted | 2025-10-05 | Use Transactional Outbox Pattern for reliable event publishing to Kafka |
| [ADR-003](./ADR-003-cloudevents-adoption.md) | CloudEvents Adoption | Accepted | 2025-10-05 | Adopt CloudEvents v1.0 as standard format for all domain events |
| [ADR-004](./ADR-004-mongodb-selection.md) | MongoDB Selection | Accepted | 2025-10-05 | Use MongoDB 7.0 as primary data store for flexible schema and scalability |
| [ADR-005](./ADR-005-kafka-event-streaming.md) | Kafka for Event Streaming | Accepted | 2025-10-05 | Use Apache Kafka as event streaming platform for event-driven architecture |

## ADR Status Workflow

```
Proposed → Accepted → Deprecated → Superseded
     ↓
  Rejected
```

- **Proposed**: Under consideration
- **Accepted**: Currently in use
- **Deprecated**: No longer recommended, but still in use
- **Superseded**: Replaced by a newer ADR
- **Rejected**: Considered but not adopted

## How to Create a New ADR

1. Copy the template: `cp ADR-TEMPLATE.md ADR-XXX-title.md`
2. Fill in the sections
3. Submit for review
4. Update this index

## ADR Template

```markdown
# ADR-XXX: Title

**Status**: Proposed | Accepted | Deprecated | Superseded | Rejected
**Date**: YYYY-MM-DD
**Deciders**: List of people involved
**Technical Story**: Issue/Story ID

## Context
What is the issue we're seeing that is motivating this decision or change?

## Decision
What is the change that we're proposing and/or doing?

## Consequences
What becomes easier or more difficult because of this change?

### Positive
- ...

### Negative
- ...

## Alternatives Considered
What other options did we look at?

## References
- Link 1
- Link 2

## Related ADRs
- ADR-XXX
```

## Key Architectural Decisions

### Core Architecture
- **ADR-001**: Hexagonal Architecture ensures domain logic is isolated from infrastructure concerns
- **ADR-004**: MongoDB provides flexible schema for varying product data

### Event-Driven Architecture
- **ADR-002**: Outbox Pattern guarantees event delivery even during Kafka outages
- **ADR-003**: CloudEvents standardization enables interoperability with external systems
- **ADR-005**: Kafka provides durable, scalable event streaming backbone

## Decision Drivers

The key drivers for these architectural decisions were:

1. **Scalability**: Handle millions of SKUs and tens of thousands of transactions per second
2. **Reliability**: Zero data loss, eventual consistency acceptable
3. **Maintainability**: Clear boundaries, testable code
4. **Flexibility**: Support evolving business requirements
5. **Interoperability**: Standard formats for external integrations
6. **Developer Experience**: Modern tooling, good documentation

## Review Schedule

ADRs are reviewed:
- **Quarterly**: Check if decisions still valid
- **Annually**: Formal review and update
- **On Trigger**: When specified metric thresholds are reached

Example triggers:
- ADR-004: Review when SKU count > 500K or TPS > 25K
- ADR-005: Review when event volume > 100K/sec

## Further Reading

- [Documenting Architecture Decisions by Michael Nygard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
- [ADR GitHub Organization](https://adr.github.io/)
- [Architecture Decision Records in Action](https://www.thoughtworks.com/radar/techniques/lightweight-architecture-decision-records)

---

**Last Updated**: 2025-10-05
