# ADR-005: Kafka for Event Streaming

**Status**: Accepted
**Date**: 2025-10-05
**Deciders**: Architecture Team, Platform Team
**Technical Story**: Event-Driven Architecture Platform Selection

## Context

The Paklog platform requires an event streaming platform to enable:
- **Asynchronous Communication**: Between microservices
- **Event-Driven Architecture**: Domain events for inter-service coordination
- **Event Sourcing**: Event log as source of truth
- **Real-Time Processing**: Stream processing for analytics and monitoring
- **Integration**: With external systems (partners, marketplaces)
- **Scalability**: Handle millions of events per day

**Requirements**:
- **Durability**: Events must not be lost
- **Ordering**: Events for same aggregate must be ordered
- **Scalability**: Linear scaling with load
- **Performance**: Sub-100ms latency for event delivery
- **Replay**: Ability to replay events from history
- **Monitoring**: Comprehensive observability

## Decision

We will use **Apache Kafka** as the event streaming platform for the Paklog ecosystem, with Inventory Service as an early adopter.

### Kafka Architecture

```
┌────────────────────────────────────────────────────────────┐
│                    Kafka Cluster                            │
│  ┌──────────────────────────────────────────────────┐      │
│  │  Topic: inventory.events                          │      │
│  │  ┌─────────┬─────────┬─────────┬─────────┐      │      │
│  │  │Partition│Partition│Partition│Partition│      │      │
│  │  │    0    │    1    │    2    │    3    │      │      │
│  │  └─────────┴─────────┴─────────┴─────────┘      │      │
│  └──────────────────────────────────────────────────┘      │
└────────────────────────────────────────────────────────────┘
        ↑                                      ↓
    Producers                              Consumers
┌──────────────┐                      ┌──────────────┐
│   Inventory  │                      │    Order     │
│   Service    │                      │  Management  │
└──────────────┘                      └──────────────┘
                                      ┌──────────────┐
                                      │  Warehouse   │
                                      │  Operations  │
                                      └──────────────┘
                                      ┌──────────────┐
                                      │ Fulfillment  │
                                      │   Service    │
                                      └──────────────┘
```

### Configuration

#### Docker Compose (Development)
```yaml
kafka:
  image: confluentinc/cp-kafka:7.6.1
  ports:
    - "9092:9092"
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

#### Spring Kafka Configuration
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=inventory-service-group
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=io.cloudevents.kafka.CloudEventSerializer
spring.kafka.consumer.value-deserializer=io.cloudevents.kafka.CloudEventDeserializer
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.producer.properties.enable.idempotence=true
```

### Topic Design

#### Topic Naming Convention
Format: `<domain>.<entity>.<event-type>`

**Topics**:
```
inventory.events                  # All inventory domain events
inventory.stock.commands         # Commands (if using CQRS)
inventory.stock.snapshots        # Snapshots for event sourcing
```

#### Partitioning Strategy

**Key**: SKU (ensures ordering per SKU)
```java
ProducerRecord<String, CloudEvent> record = new ProducerRecord<>(
    "inventory.events",
    productStock.getSku(),  // Key = SKU
    cloudEvent
);
```

**Partitions**: 12 partitions initially
- Parallelism: Up to 12 consumers
- Throughput: ~10K events/sec per partition
- Total capacity: ~120K events/sec

### Producer Implementation

```java
@Component
public class KafkaEventPublisher {

    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;
    private final CorrelationIdInterceptor correlationIdInterceptor;

    @Value("${kafka.topic.inventory-events}")
    private String topic;

    public CompletableFuture<SendResult<String, CloudEvent>> publish(
            String sku,
            CloudEvent cloudEvent) {

        ProducerRecord<String, CloudEvent> record =
            new ProducerRecord<>(topic, sku, cloudEvent);

        // Add correlation ID header
        record.headers().add(
            "X-Correlation-ID",
            correlationIdInterceptor.getCorrelationId().getBytes()
        );

        return kafkaTemplate.send(record)
            .thenApply(result -> {
                log.info("Published event: type={}, partition={}, offset={}",
                    cloudEvent.getType(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
                return result;
            })
            .exceptionally(ex -> {
                log.error("Failed to publish event: type={}",
                    cloudEvent.getType(), ex);
                throw new EventPublishException(ex);
            });
    }
}
```

### Consumer Implementation

```java
@Component
public class OrderServiceEventConsumer {

    @KafkaListener(
        topics = "inventory.events",
        groupId = "order-service-group",
        containerFactory = "cloudEventListenerContainerFactory"
    )
    public void handleInventoryEvent(
            @Payload CloudEvent cloudEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received event: type={}, partition={}, offset={}",
            cloudEvent.getType(), partition, offset);

        try {
            switch (cloudEvent.getType()) {
                case "com.paklog.inventory.fulfillment.v1.product-stock.level-changed":
                    handleStockLevelChanged(cloudEvent);
                    break;
                case "com.paklog.inventory.fulfillment.v1.stock-status.changed":
                    handleStockStatusChanged(cloudEvent);
                    break;
                default:
                    log.warn("Unknown event type: {}", cloudEvent.getType());
            }
        } catch (Exception e) {
            log.error("Error processing event", e);
            // Will retry based on Kafka consumer configuration
            throw e;
        }
    }

    private void handleStockLevelChanged(CloudEvent cloudEvent) {
        StockLevelChangedEvent event = deserialize(cloudEvent);
        // Update order availability based on new stock level
        orderAvailabilityService.updateAvailability(event.getSku());
    }
}
```

## Consequences

### Positive

✅ **High Throughput**: Millions of events per second
- Linear scalability with partitions
- Batching and compression
- Zero-copy optimization

✅ **Durability**: Events persist to disk
- Configurable replication factor
- Log retention policies
- No data loss with proper configuration

✅ **Ordering Guarantees**: Per-partition ordering
- Events for same SKU always ordered
- Partition assignment by key (SKU)

✅ **Replay Capability**: Consumer can rewind
- Seek to specific offset
- Replay from timestamp
- Useful for debugging and recovery

✅ **Decoupling**: Publishers and consumers independent
- Add/remove consumers without affecting producers
- Consumers can process at their own pace
- Back pressure handling

✅ **Ecosystem**: Rich tooling and connectors
- Kafka Connect for integrations
- Kafka Streams for processing
- Schema Registry support
- Monitoring tools (Burrow, Kafdrop)

✅ **Industry Standard**: Widely adopted
- Large community
- Production-proven
- Cloud provider support (MSK, Confluent Cloud)

### Negative

⚠️ **Operational Complexity**: Requires expertise
- Zookeeper dependency (until KRaft)
- Cluster management
- Rebalancing coordination
- **Mitigation**: Use managed Kafka (Confluent Cloud, AWS MSK)

⚠️ **Eventual Consistency**: Events delivered asynchronously
- Consumers eventually see events
- Temporal coupling in message ordering
- **Mitigation**: Design for eventual consistency

⚠️ **Consumer Lag**: Monitoring required
- Consumers can fall behind
- Need alerting on high lag
- **Mitigation**: Consumer lag monitoring and auto-scaling

⚠️ **Storage Costs**: Events retained on disk
- Default retention: 7 days
- Long retention = more storage
- **Mitigation**: Tiered storage, log compaction

⚠️ **Message Size Limits**: Default 1MB max
- Large payloads need special handling
- **Mitigation**: Reference large data, compress, or use external storage

## Kafka Configuration

### Producer Configuration

```yaml
# Reliability (Idempotence + Acks)
enable.idempotence: true
acks: all                      # Wait for all replicas
retries: 3
max.in.flight.requests.per.connection: 1

# Performance
batch.size: 16384
linger.ms: 10                  # Wait 10ms to batch
compression.type: snappy       # Compress messages
buffer.memory: 33554432        # 32MB buffer

# Timeout
request.timeout.ms: 30000
delivery.timeout.ms: 120000
```

### Consumer Configuration

```yaml
# Reliability
enable.auto.commit: false      # Manual commit
auto.offset.reset: earliest    # Start from beginning if no offset
isolation.level: read_committed # Only read committed messages

# Performance
fetch.min.bytes: 1024
fetch.max.wait.ms: 500
max.poll.records: 500
max.poll.interval.ms: 300000   # 5 minutes

# Session
session.timeout.ms: 30000
heartbeat.interval.ms: 10000
```

### Topic Configuration

```yaml
# Retention
retention.ms: 604800000        # 7 days
retention.bytes: -1            # No size limit

# Replication
replication.factor: 3          # 3 replicas
min.insync.replicas: 2         # At least 2 replicas

# Partitions
num.partitions: 12

# Cleanup
cleanup.policy: delete         # Delete old messages
```

## Monitoring

### Key Metrics

**Producer**:
- `kafka.producer.record-send-rate`: Messages/sec
- `kafka.producer.request-latency-avg`: Average latency
- `kafka.producer.record-error-rate`: Error rate

**Consumer**:
- `kafka.consumer.records-lag`: Consumer lag
- `kafka.consumer.records-consumed-rate`: Consumption rate
- `kafka.consumer.fetch-latency-avg`: Fetch latency

**Broker**:
- `kafka.server.BrokerTopicMetrics.MessagesInPerSec`: Incoming messages
- `kafka.network.RequestMetrics.TotalTimeMs`: Request time
- `kafka.log.Log.Size`: Log size

### Alerts

```yaml
- alert: HighConsumerLag
  expr: kafka_consumer_lag > 10000
  for: 5m
  annotations:
    summary: "Consumer lag high for {{ $labels.topic }}"

- alert: LowProducerThroughput
  expr: rate(kafka_producer_record_send_total[5m]) < 100
  for: 10m
  annotations:
    summary: "Producer throughput low"
```

## Alternatives Considered

### 1. RabbitMQ
**Pros**:
- ✅ Easier to operate
- ✅ Rich routing
- ✅ Lower latency for small messages

**Cons**:
- ❌ Lower throughput than Kafka
- ❌ No native replay
- ❌ Not designed for event streaming

**Decision**: Rejected due to lower throughput and no replay

### 2. AWS SNS/SQS
**Pros**:
- ✅ Fully managed
- ✅ No operational overhead
- ✅ Simple pub/sub

**Cons**:
- ❌ Vendor lock-in
- ❌ No ordering guarantees (SQS)
- ❌ No replay capability
- ❌ Message retention only 14 days

**Decision**: Rejected due to vendor lock-in and limited features

### 3. Apache Pulsar
**Pros**:
- ✅ Similar to Kafka
- ✅ Multi-tenancy
- ✅ Tiered storage

**Cons**:
- ❌ Less mature ecosystem
- ❌ Smaller community
- ❌ Fewer managed offerings

**Decision**: Rejected due to less mature ecosystem

### 4. Google Pub/Sub
**Pros**:
- ✅ Fully managed
- ✅ Global distribution
- ✅ No capacity planning

**Cons**:
- ❌ Vendor lock-in
- ❌ No ordering within partition
- ❌ More expensive at scale

**Decision**: Rejected due to vendor lock-in

## Migration Path

### Phase 1 (Current): Single Cluster
- Development: Local Kafka in Docker
- Staging: Self-managed Kafka cluster
- Production: AWS MSK (Managed Streaming for Kafka)

### Phase 2 (6 months): Multi-Region
- Active-active replication with MirrorMaker 2
- Regional clusters for low latency
- Cross-region disaster recovery

### Phase 3 (12 months): Confluent Cloud
- Migrate to fully managed Confluent Cloud
- Reduce operational burden
- Access to Confluent features (ksqlDB, Schema Registry)

## References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Kafka: The Definitive Guide](https://www.confluent.io/resources/kafka-the-definitive-guide/)
- [Spring Kafka](https://spring.io/projects/spring-kafka)
- [CloudEvents Kafka Protocol Binding](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md)

## Related ADRs

- ADR-002: Event Sourcing Strategy (Outbox Pattern)
- ADR-003: CloudEvents Adoption

---

**Last Updated**: 2025-10-05
**Review Date**: 2026-01-05
**Next Review Trigger**: When event volume > 100K/sec
