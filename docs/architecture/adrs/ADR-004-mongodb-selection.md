# ADR-004: MongoDB Selection for Inventory Data Storage

**Status**: Accepted
**Date**: 2025-10-05
**Deciders**: Architecture Team, Database Team
**Technical Story**: Database Technology Selection

## Context

The Inventory Management Service requires a database that supports:
- **Flexible Schema**: Product attributes vary by category
- **High Write Throughput**: Stock adjustments from multiple sources
- **Fast Reads**: Stock level queries need sub-50ms response times
- **Horizontal Scalability**: Growth from thousands to millions of SKUs
- **Document Storage**: Rich aggregate structures (ProductStock with nested data)
- **Query Flexibility**: Complex queries for reporting and analytics
- **Operational Simplicity**: Managed service availability

**Data Characteristics**:
- **Volume**: 100K+ SKUs initially, scaling to 10M+
- **Write Pattern**: 5K-10K stock updates/sec at peak
- **Read Pattern**: 50K stock queries/sec
- **Data Structure**: Complex nested documents (lots, serial numbers, holds)
- **Consistency**: Eventual consistency acceptable for most queries

## Decision

We will use **MongoDB 7.0** as the primary data store for the Inventory Management Service.

### MongoDB Configuration

```yaml
# docker-compose.yaml
mongodb:
  image: mongo:7.0
  environment:
    MONGO_INITDB_DATABASE: inventorydb
  ports:
    - "27017:27017"
```

```properties
# application.properties
spring.data.mongodb.uri=mongodb://localhost:27017/inventorydb
spring.data.mongodb.pool.min-size=10
spring.data.mongodb.pool.max-size=100
spring.data.mongodb.pool.max-wait-time-ms=5000
```

### Collections Design

#### 1. product_stock Collection
**Purpose**: Core inventory data
**Schema**: Flexible document for ProductStock aggregate

```javascript
{
  _id: ObjectId("..."),
  sku: "SKU-12345",
  stockLevel: {
    quantityOnHand: 500,
    quantityAllocated: 150,
    quantityAvailable: 350
  },
  location: {
    warehouseId: "WH-001",
    zone: "A",
    aisle: "12",
    bin: "B5"
  },
  lotBatches: [
    {
      lotNumber: "LOT-2025-001",
      quantity: 200,
      expirationDate: ISODate("2026-01-01"),
      receivedDate: ISODate("2025-10-01"),
      status: "ACTIVE"
    }
  ],
  serialNumbers: [
    {
      serialNumber: "SN-12345-001",
      status: "AVAILABLE",
      assignedOrderId: null
    }
  ],
  holds: [
    {
      holdId: "HOLD-001",
      holdType: "QUALITY",
      quantity: 50,
      placedAt: ISODate("2025-10-05"),
      expiresAt: ISODate("2025-10-10"),
      reason: "Quality inspection pending"
    }
  ],
  abcClassification: {
    abcClass: "A",
    lastCalculatedAt: ISODate("2025-10-01"),
    annualDollarUsage: 150000.0
  },
  valuation: {
    method: "WEIGHTED_AVERAGE",
    unitCost: 25.50,
    totalValue: 12750.0
  },
  metadata: {
    createdAt: ISODate("2025-01-01"),
    updatedAt: ISODate("2025-10-05"),
    version: 5
  }
}
```

**Indexes**:
```javascript
db.product_stock.createIndex({ sku: 1 }, { unique: true })
db.product_stock.createIndex({ "stockLevel.quantityOnHand": 1 })
db.product_stock.createIndex({ "location.warehouseId": 1, "location.zone": 1 })
db.product_stock.createIndex({ "abcClassification.abcClass": 1 })
db.product_stock.createIndex({ "metadata.updatedAt": -1 })
```

#### 2. inventory_ledger Collection
**Purpose**: Audit trail and history
**Schema**: Immutable ledger entries

```javascript
{
  _id: ObjectId("..."),
  sku: "SKU-12345",
  timestamp: ISODate("2025-10-05T20:00:00Z"),
  changeType: "ADJUSTMENT",
  quantityChange: 100,
  reasonCode: "stock_intake",
  operatorId: "admin",
  orderId: null,
  correlationId: "a1b2c3d4-e5f6-g7h8",
  metadata: {
    source: "REST_API",
    ipAddress: "192.168.1.100"
  }
}
```

**Indexes**:
```javascript
db.inventory_ledger.createIndex({ sku: 1, timestamp: -1 })
db.inventory_ledger.createIndex({ changeType: 1, timestamp: -1 })
db.inventory_ledger.createIndex({ correlationId: 1 })
db.inventory_ledger.createIndex({ timestamp: -1 })
```

#### 3. outbox_events Collection
**Purpose**: Transactional outbox pattern
**Schema**: Unpublished domain events

```javascript
{
  _id: ObjectId("..."),
  aggregateId: "SKU-12345",
  eventType: "com.paklog.inventory.fulfillment.v1.product-stock.level-changed",
  eventData: "{...}", // CloudEvent JSON
  createdAt: ISODate("2025-10-05T20:00:00Z"),
  published: false,
  publishedAt: null,
  retryCount: 0
}
```

**Indexes**:
```javascript
db.outbox_events.createIndex({ published: 1, createdAt: 1 })
db.outbox_events.createIndex({ aggregateId: 1, createdAt: -1 })
```

## Consequences

### Positive

✅ **Flexible Schema**: Perfect for varying product data
- Different SKUs have different attributes
- Easy to add new fields without migration
- Nested documents match domain model

✅ **Performance**: Excellent read/write performance
- Sub-10ms reads with indexes
- 10K+ writes/sec on commodity hardware
- Horizontal scaling with sharding

✅ **Developer Productivity**: Easy to work with
- JSON-native (matches REST API)
- Spring Data MongoDB support
- Testcontainers for integration tests
- MongoDB Compass for debugging

✅ **Operational Maturity**: Production-ready
- MongoDB Atlas (managed service)
- Built-in replication
- Point-in-time recovery
- Automated backups

✅ **Query Flexibility**: Rich query capabilities
- Aggregation framework
- Text search
- Geospatial queries
- Array operations

✅ **Ecosystem**: Large community and tooling
- Extensive documentation
- Monitoring tools (MongoDB Ops Manager)
- Schema design tools
- Migration tools

### Negative

⚠️ **Consistency Trade-offs**: Eventual consistency by default
- Not ACID across collections
- Read-your-own-writes requires care
- **Mitigation**: Single-document ACID transactions work well

⚠️ **Join Performance**: No native joins
- Denormalization required
- Data duplication
- **Mitigation**: Embed related data in documents

⚠️ **Schema Validation**: Optional, not enforced
- Can lead to data quality issues
- **Mitigation**: Use JSON Schema validation
  ```javascript
  db.createCollection("product_stock", {
    validator: { $jsonSchema: { ... } }
  })
  ```

⚠️ **Storage Overhead**: JSON format uses more space
- Larger storage footprint than relational
- Field names repeated in each document
- **Mitigation**: Compression, short field names

⚠️ **Index Memory**: Indexes must fit in RAM
- Large datasets require significant memory
- **Mitigation**: Selective indexing, compound indexes

## Alternatives Considered

### 1. PostgreSQL
**Pros**:
- ✅ ACID transactions
- ✅ Strong consistency
- ✅ JSONB support for flexible schema

**Cons**:
- ❌ Harder to scale horizontally
- ❌ More complex sharding
- ❌ Less flexible schema changes

**Decision**: Rejected because flexibility and scalability outweigh ACID requirements for inventory

### 2. Amazon DynamoDB
**Pros**:
- ✅ Fully managed
- ✅ Unlimited scale
- ✅ Low latency

**Cons**:
- ❌ Vendor lock-in
- ❌ Limited query flexibility
- ❌ Expensive for high write volume
- ❌ Complex data modeling

**Decision**: Rejected due to vendor lock-in and query limitations

### 3. Cassandra
**Pros**:
- ✅ Excellent write scalability
- ✅ Linear scalability
- ✅ Multi-datacenter replication

**Cons**:
- ❌ Complex data modeling
- ❌ Limited query flexibility
- ❌ Steep learning curve
- ❌ Operational complexity

**Decision**: Rejected as over-engineered for current needs

### 4. Redis (Primary Store)
**Pros**:
- ✅ Extremely fast
- ✅ In-memory performance

**Cons**:
- ❌ Limited persistence options
- ❌ Memory constraints
- ❌ No complex queries
- ❌ Not suitable for primary storage

**Decision**: Rejected for primary storage; used for caching instead

## Data Modeling Strategy

### Embedding vs. Referencing

**Embed When**:
- Data accessed together (ProductStock + LotBatches)
- One-to-few relationship
- Data doesn't change frequently
- Bounded context ownership

**Reference When**:
- Many-to-many relationships
- Data accessed independently
- Large datasets
- Frequent updates

**Our Approach**:
```
ProductStock (Embedded):
├── StockLevel ✅ Embedded
├── Location ✅ Embedded
├── LotBatches[] ✅ Embedded
├── SerialNumbers[] ✅ Embedded
├── Holds[] ✅ Embedded
├── ABCClassification ✅ Embedded
└── Valuation ✅ Embedded

InventoryLedger: Separate Collection ✅ (audit log)
OutboxEvents: Separate Collection ✅ (transactional outbox)
```

## Scaling Strategy

### Phase 1 (Current): Single Replica Set
- **Capacity**: 100K SKUs, 10K TPS
- **Setup**: 1 Primary + 2 Secondaries
- **Read Preference**: Primary for writes, Secondaries for analytics

### Phase 2 (6-12 months): Sharding by SKU Prefix
- **Trigger**: > 1M SKUs or > 50K TPS
- **Shard Key**: `{ sku: "hashed" }`
- **Shards**: 3-5 initially

### Phase 3 (12+ months): Multi-Region
- **Setup**: Replica sets in multiple regions
- **Writes**: Single region (primary)
- **Reads**: Local region (secondary)

## Operational Considerations

### Backups
```bash
# Automated daily backups
mongodump --uri="mongodb://localhost:27017/inventorydb" \
  --out=/backups/$(date +%Y%m%d)

# Retention: 30 days
find /backups -type d -mtime +30 -exec rm -rf {} \;
```

### Monitoring
```yaml
# Prometheus metrics
mongodb.connections.current
mongodb.operations.total
mongodb.network.bytesIn
mongodb.network.bytesOut
mongodb.locks.waiting
```

### Performance Tuning
```javascript
// Analyze slow queries
db.setProfilingLevel(1, { slowms: 100 })
db.system.profile.find().sort({ ts: -1 }).limit(5)

// Explain query plans
db.product_stock.find({ sku: "SKU-12345" }).explain("executionStats")
```

## References

- [MongoDB Documentation](https://www.mongodb.com/docs/)
- [Data Modeling Guide](https://www.mongodb.com/docs/manual/core/data-modeling-introduction/)
- [Performance Best Practices](https://www.mongodb.com/docs/manual/administration/analyzing-mongodb-performance/)
- [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb)

## Related ADRs

- ADR-001: Hexagonal Architecture
- ADR-002: Event Sourcing Strategy

---

**Last Updated**: 2025-10-05
**Review Date**: 2026-01-05
**Next Review Trigger**: When SKU count > 500K or TPS > 25K
