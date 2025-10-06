# ADR-002: Event Sourcing Strategy (Outbox Pattern)

**Status**: Accepted
**Date**: 2025-10-05
**Deciders**: Architecture Team, Backend Team
**Technical Story**: Event-Driven Architecture Initiative

## Context

The Inventory Management Service needs to:
- Publish domain events to Kafka for downstream consumers
- Ensure events are published reliably (no data loss)
- Maintain consistency between database state and published events
- Support event replay and audit trail
- Enable event-driven integrations with other services

**Challenges**:
1. **Dual Write Problem**: Writing to database AND publishing to Kafka can fail inconsistently
2. **Transaction Boundaries**: Kafka doesn't participate in database transactions
3. **Reliability**: Events must be published eventually, even if Kafka is temporarily unavailable
4. **Ordering**: Events must be published in the order they occurred

## Decision

We will use the **Outbox Pattern** with **Transactional Outbox** for reliable event publishing.

### Pattern Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Transaction                   │
│  ┌────────────────────────────────────────────────────┐    │
│  │  1. Update ProductStock aggregate                  │    │
│  │  2. Save ProductStock to product_stock collection  │    │
│  │  3. Save OutboxEvent to outbox_events collection   │ <──┼─ Single Transaction
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ↓
┌─────────────────────────────────────────────────────────────┐
│               Outbox Event Publisher (Scheduled)             │
│  ┌────────────────────────────────────────────────────┐    │
│  │  1. Poll unpublished events from outbox_events     │    │
│  │  2. Publish events to Kafka                        │    │
│  │  3. Mark events as published in database           │    │
│  │  4. Delete old published events (retention)        │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Implementation Details

#### 1. OutboxEvent Model

```java
@Document(collection = "outbox_events")
public class OutboxEvent {
    private String id;
    private String aggregateId;      // SKU
    private String eventType;        // CloudEvents type
    private String eventData;        // JSON payload
    private LocalDateTime createdAt;
    private boolean published;
    private LocalDateTime publishedAt;
    private int retryCount;
}
```

#### 2. Publishing Flow

**Step 1: Domain Operation**
```java
@Transactional
public ProductStock adjustStock(String sku, int quantityChange, String reasonCode) {
    // 1. Load aggregate
    ProductStock stock = repository.findBySku(sku);

    // 2. Execute domain logic
    stock.adjustQuantityOnHand(quantityChange, reasonCode);

    // 3. Save aggregate (generates domain events)
    repository.save(stock);

    // 4. Save domain events to outbox (in same transaction)
    List<OutboxEvent> outboxEvents = stock.getUncommittedEvents()
        .stream()
        .map(OutboxEvent::from)
        .collect(toList());
    outboxRepository.saveAll(outboxEvents);

    // 5. Mark events as committed
    stock.markEventsAsCommitted();

    return stock;
}
```

**Step 2: Background Publisher**
```java
@Scheduled(fixedDelay = 5000) // Every 5 seconds
public void publishEvents() {
    List<OutboxEvent> unpublishedEvents =
        outboxRepository.findUnpublished(BATCH_SIZE);

    for (OutboxEvent event : unpublishedEvents) {
        try {
            kafkaTemplate.send(TOPIC, event.toCloudEvent());
            event.markAsPublished();
            outboxRepository.save(event);
        } catch (Exception e) {
            event.incrementRetryCount();
            outboxRepository.save(event);
            log.error("Failed to publish event", e);
        }
    }
}
```

### Configuration

**Polling Interval**: 5 seconds
```properties
outbox.publisher.fixed-delay=5000
```

**Batch Size**: 100 events per poll
**Retry Strategy**: Exponential backoff with max 10 retries
**Retention**: Published events kept for 30 days

## Consequences

### Positive

✅ **Guaranteed Delivery**: Events eventually published (at-least-once delivery)
- Events stored in database atomically with business data
- Survives Kafka outages
- Automatic retries on failure

✅ **Consistency**: Database and events always consistent
- Single transaction guarantees atomicity
- No dual-write problem
- No lost events

✅ **Ordering**: Events published in order (per aggregate)
- CreatedAt timestamp preserves order
- Batch processing maintains sequence

✅ **Audit Trail**: Complete event history
- All events stored in database
- Can replay events if needed
- Debugging and troubleshooting easier

✅ **Decoupling**: Kafka availability doesn't block business operations
- Service continues working during Kafka outages
- Events queued in database
- Published when Kafka recovers

### Negative

⚠️ **Eventual Consistency**: Events not published immediately
- 5-second delay (average 2.5s)
- Downstream systems slightly behind
- Not suitable for real-time requirements

⚠️ **Duplicates Possible**: At-least-once delivery semantics
- Kafka publish failure after DB commit causes retries
- Consumers must be idempotent
- Requires duplicate detection downstream

⚠️ **Storage Overhead**: Events stored in database
- Additional storage for outbox table
- Need to clean up old events
- More database writes

⚠️ **Polling Overhead**: Background scheduler constantly polls
- Database queries every 5 seconds
- CPU and network overhead
- Consider Change Data Capture (CDC) for scale

## Alternatives Considered

### 1. Direct Kafka Publishing (Dual Write)
```java
@Transactional
public void adjustStock() {
    repository.save(stock);
    kafkaTemplate.send(event); // NOT in transaction!
}
```

**Rejected because**:
- ❌ Kafka publish can fail after DB commit
- ❌ Events can be lost
- ❌ Inconsistency between DB and Kafka

### 2. Two-Phase Commit (2PC)
**Rejected because**:
- ❌ Kafka doesn't support 2PC
- ❌ Performance overhead
- ❌ Complexity and failure modes

### 3. Change Data Capture (CDC) with Debezium
```
MongoDB → Debezium → Kafka
```

**Not chosen (yet) because**:
- ⚠️ More infrastructure complexity
- ⚠️ Requires connector deployment
- ⚠️ Harder to control event schema
- ✅ **Future consideration** for higher scale

### 4. Transaction Log Tailing
**Rejected because**:
- ❌ Tightly coupled to MongoDB internals
- ❌ Harder to transform events
- ❌ Less control over event format

## Migration Path to CDC

If outbox polling becomes a bottleneck (> 10K events/sec), we can migrate to CDC:

**Phase 1** (Current): Outbox Pattern
- Sufficient for 1-10K TPS
- Simple implementation
- Proven pattern

**Phase 2** (Future): Hybrid Approach
- Critical events: CDC (instant)
- Non-critical events: Outbox

**Phase 3** (Scale): Full CDC
- Debezium connector on outbox table
- Zero-lag event publishing
- Requires operational expertise

## Event Retention Policy

```java
@Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
public void cleanupOldEvents() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
    outboxRepository.deletePublishedEventsBefore(cutoff);
}
```

## Monitoring

**Metrics**:
- `outbox.unpublished.events.count`: Gauge of pending events
- `outbox.publish.success.count`: Counter of published events
- `outbox.publish.failure.count`: Counter of failed publishes
- `outbox.publish.lag.seconds`: Time between event creation and publish

**Alerts**:
- Unpublished events > 1000 for 5 minutes
- Publish lag > 60 seconds for 10 minutes
- Failure rate > 1% for 15 minutes

## References

- [Microservices Patterns: Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)
- [Implementing the Outbox Pattern](https://www.kamilgrzybek.com/design/the-outbox-pattern/)
- [Debezium CDC](https://debezium.io/)

## Related ADRs

- ADR-001: Hexagonal Architecture
- ADR-003: CloudEvents Adoption
- ADR-005: Kafka for Event Streaming

---

**Last Updated**: 2025-10-05
**Review Date**: 2026-01-05
**Next Review Trigger**: When event volume > 5K TPS
