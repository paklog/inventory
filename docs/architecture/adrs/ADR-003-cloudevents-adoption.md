# ADR-003: CloudEvents Adoption for Event Standardization

**Status**: Accepted
**Date**: 2025-10-05
**Deciders**: Architecture Team, Integration Team
**Technical Story**: Event-Driven Architecture Standardization

## Context

The Paklog platform consists of multiple microservices that communicate via events:
- Inventory Service
- Order Management Service
- Warehouse Operations Service
- Fulfillment Service
- Marketplace Integration Service

**Current Challenges**:
1. **No Standard Format**: Each service uses different event schemas
2. **Metadata Inconsistency**: Trace IDs, timestamps, source tracking varies
3. **Version Management**: No standard way to version event schemas
4. **Interoperability**: Hard to integrate with external systems
5. **Tooling**: Can't use standard event tooling (schema registries, validators)

**Requirements**:
- Standard event format across all services
- Support for event versioning
- Traceability and correlation
- Industry standard (not custom)
- JSON-based for readability

## Decision

We will adopt **CloudEvents v1.0** as the standard event format for all domain events published by the Inventory Service and recommend adoption across all Paklog services.

### CloudEvents Specification

CloudEvents defines a standard structure for event data:

```json
{
  "specversion": "1.0",
  "type": "com.paklog.inventory.fulfillment.v1.product-stock.level-changed",
  "source": "inventory-service",
  "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "time": "2025-10-05T20:00:00Z",
  "datacontenttype": "application/json",
  "subject": "SKU-12345",
  "traceparent": "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01",
  "data": {
    "sku": "SKU-12345",
    "previousQuantityOnHand": 500,
    "newQuantityOnHand": 600,
    "changeType": "ADJUSTMENT",
    "reasonCode": "stock_intake"
  }
}
```

### Event Type Naming Convention

Format: `com.paklog.<bounded-context>.<domain>.v<version>.<aggregate>.<event-name>`

**Examples**:
```
com.paklog.inventory.fulfillment.v1.product-stock.level-changed
com.paklog.inventory.fulfillment.v1.stock-status.changed
com.paklog.inventory.fulfillment.v1.inventory-hold.placed
com.paklog.inventory.fulfillment.v1.inventory-hold.released
com.paklog.inventory.fulfillment.v1.abc-classification.changed
com.paklog.inventory.fulfillment.v1.kit.assembled
com.paklog.inventory.fulfillment.v1.stock-transfer.completed
```

### Implementation

#### 1. CloudEventType Registry

```java
public enum CloudEventType {
    STOCK_LEVEL_CHANGED("com.paklog.inventory.fulfillment.v1.product-stock.level-changed"),
    STOCK_STATUS_CHANGED("com.paklog.inventory.fulfillment.v1.stock-status.changed"),
    INVENTORY_HOLD_PLACED("com.paklog.inventory.fulfillment.v1.inventory-hold.placed"),
    INVENTORY_HOLD_RELEASED("com.paklog.inventory.fulfillment.v1.inventory-hold.released"),
    // ... more types

    private final String eventType;

    public String getEventType() {
        return eventType;
    }
}
```

#### 2. CloudEvent Factory

```java
@Component
public class CloudEventFactory {

    public CloudEvent createStockLevelChangedEvent(ProductStock stock,
                                                   StockLevelChangedEvent domainEvent) {
        return CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType(CloudEventType.STOCK_LEVEL_CHANGED.getEventType())
            .withSource(URI.create("inventory-service"))
            .withTime(OffsetDateTime.now())
            .withDataContentType("application/json")
            .withSubject(stock.getSku())
            .withExtension("traceparent", MDC.get("traceparent"))
            .withData(objectMapper.writeValueAsBytes(domainEvent))
            .build();
    }
}
```

#### 3. JSON Schema Validation

Each event type has a JSON Schema:

```
src/main/resources/asyncapi/cloudevents/jsonschema/
├── stock-level-changed-schema.json
├── stock-status-changed-schema.json
├── inventory-hold-placed-schema.json
└── ...
```

**Example Schema**:
```json
{
  "$id": "com.paklog.inventory.fulfillment.v1.product-stock.level-changed",
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Stock Level Changed Event",
  "type": "object",
  "required": ["sku", "previousQuantityOnHand", "newQuantityOnHand", "changeType"],
  "properties": {
    "sku": {
      "type": "string",
      "description": "The SKU identifier"
    },
    "previousQuantityOnHand": {
      "type": "integer",
      "description": "Quantity before change"
    },
    "newQuantityOnHand": {
      "type": "integer",
      "description": "Quantity after change"
    },
    "changeType": {
      "type": "string",
      "enum": ["ADJUSTMENT", "RECEIPT", "ALLOCATION", "PICK"]
    },
    "reasonCode": {
      "type": "string"
    }
  }
}
```

#### 4. Schema Validator

```java
@Component
public class CloudEventSchemaValidator {

    private final Map<String, JsonSchema> schemas = new HashMap<>();

    @PostConstruct
    public void loadSchemas() {
        // Load all schemas from classpath
        schemas.put(
            CloudEventType.STOCK_LEVEL_CHANGED.getEventType(),
            loadSchema("cloudevents/jsonschema/stock-level-changed-schema.json")
        );
        // ... load other schemas
    }

    public void validate(CloudEvent cloudEvent) throws ValidationException {
        JsonSchema schema = schemas.get(cloudEvent.getType());
        if (schema == null) {
            throw new ValidationException("No schema found for type: " + cloudEvent.getType());
        }

        Set<ValidationMessage> errors = schema.validate(
            new String(cloudEvent.getData().toBytes())
        );

        if (!errors.isEmpty()) {
            throw new ValidationException("Event validation failed: " + errors);
        }
    }
}
```

## Consequences

### Positive

✅ **Standardization**: Industry-standard event format
- CNCF (Cloud Native Computing Foundation) specification
- Supported by major cloud providers (AWS, Azure, GCP)
- Growing ecosystem of tools

✅ **Interoperability**: Easy integration with external systems
- Standard format recognized by partners
- Compatible with event gateways (AWS EventBridge, Google Eventarc)
- Schema Registry support (Confluent, Apicurio)

✅ **Traceability**: Built-in support for distributed tracing
- `traceparent` extension for W3C Trace Context
- `source` identifies origin service
- `id` for event correlation

✅ **Versioning**: Clear version in event type
- `v1`, `v2` in type name
- Consumers can handle multiple versions
- Graceful evolution

✅ **Tooling**: Leverage standard tooling
- CloudEvents SDK for validation
- AsyncAPI for documentation
- Schema validation with JSON Schema

✅ **Documentation**: Self-describing events
- Metadata in standard fields
- Schema references
- Human-readable JSON

### Negative

⚠️ **Payload Overhead**: More metadata than custom format
- CloudEvents envelope adds ~200-300 bytes
- Acceptable trade-off for benefits
- Consider Avro/Protobuf for binary format if needed

⚠️ **Learning Curve**: Team needs to learn CloudEvents
- Training required
- Documentation needed
- Templates and examples help

⚠️ **Migration**: Existing events need conversion
- Backward compatibility layer required
- Gradual migration approach
- Dual-format support during transition

⚠️ **Schema Management**: Need schema registry
- Additional infrastructure
- Schema versioning process
- Governance overhead

## Event Lifecycle

### Development
1. Define JSON Schema for event data
2. Create CloudEvent type in registry
3. Create sample event
4. Validate against schema
5. Document in AsyncAPI

### Publishing
```java
// 1. Domain event occurs
stock.adjustQuantityOnHand(100, "stock_intake");

// 2. Create CloudEvent
CloudEvent cloudEvent = factory.createStockLevelChangedEvent(stock, domainEvent);

// 3. Validate against schema
validator.validate(cloudEvent);

// 4. Publish to outbox
outboxRepository.save(OutboxEvent.from(cloudEvent));

// 5. Outbox publisher sends to Kafka
kafkaTemplate.send("inventory.events", cloudEvent);
```

### Consuming
```java
@KafkaListener(topics = "inventory.events")
public void handleInventoryEvent(CloudEvent cloudEvent) {
    // 1. Validate CloudEvent
    if (!cloudEvent.getSpecVersion().equals("1.0")) {
        log.error("Unsupported CloudEvents version");
        return;
    }

    // 2. Route by type
    switch (cloudEvent.getType()) {
        case "com.paklog.inventory.fulfillment.v1.product-stock.level-changed":
            handleStockLevelChanged(cloudEvent);
            break;
        // ... other types
    }
}
```

## Versioning Strategy

### Backward-Compatible Changes (Same Version)
- ✅ Add optional fields
- ✅ Add new event types
- ✅ Add enum values (if consumers handle unknown)

### Breaking Changes (New Version)
- ❌ Remove fields
- ❌ Change field types
- ❌ Change field semantics
- ❌ Remove enum values

**Version Transition**:
```
v1: com.paklog.inventory.fulfillment.v1.product-stock.level-changed
v2: com.paklog.inventory.fulfillment.v2.product-stock.level-changed
```

**Support Policy**:
- Publish both v1 and v2 for 6 months
- Deprecate v1 with 3-month notice
- Remove v1 after deprecation period

## Alternatives Considered

### 1. Custom Event Format
```json
{
  "eventId": "123",
  "eventType": "StockLevelChanged",
  "timestamp": "2025-10-05T20:00:00Z",
  "payload": { ... }
}
```

**Rejected because**:
- ❌ Not an industry standard
- ❌ No tooling support
- ❌ Hard to integrate externally
- ❌ No tracing integration

### 2. Avro with Schema Registry
**Not chosen because**:
- ⚠️ Binary format (harder to debug)
- ⚠️ Additional infrastructure (Schema Registry)
- ⚠️ Less human-readable
- ✅ **Can be combined**: CloudEvents with Avro data

### 3. Protocol Buffers
**Not chosen because**:
- ⚠️ Requires code generation
- ⚠️ Binary format
- ⚠️ Steeper learning curve
- ✅ **Can be combined**: CloudEvents with Protobuf data

## Migration Plan

### Phase 1: New Events (Current)
- ✅ All new events use CloudEvents format
- ✅ JSON Schema validation
- ✅ 12 event types implemented

### Phase 2: Legacy Event Wrapper (Month 2-3)
- Wrap existing custom events in CloudEvents envelope
- Maintain backward compatibility
- Consumers can handle both formats

### Phase 3: Full Migration (Month 4-6)
- Convert all legacy events to CloudEvents
- Remove compatibility layer
- 100% CloudEvents

## References

- [CloudEvents Specification v1.0](https://cloudevents.io/)
- [CNCF CloudEvents Primer](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/primer.md)
- [CloudEvents SDK for Java](https://github.com/cloudevents/sdk-java)
- [JSON Schema](https://json-schema.org/)
- [AsyncAPI Specification](https://www.asyncapi.com/)

## Related ADRs

- ADR-002: Event Sourcing Strategy
- ADR-005: Kafka for Event Streaming

---

**Last Updated**: 2025-10-05
**Review Date**: 2026-01-05
