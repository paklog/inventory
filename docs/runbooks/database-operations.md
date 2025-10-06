# Database Operations Runbook

## Overview
This runbook provides step-by-step procedures for MongoDB database operations in the Inventory Service.

---

## Table of Contents
1. [Backup and Restore](#backup-and-restore)
2. [Index Management](#index-management)
3. [Connection Pool Tuning](#connection-pool-tuning)
4. [Performance Troubleshooting](#performance-troubleshooting)
5. [Data Migration](#data-migration)

---

## Backup and Restore

### Automated Backups

**Schedule:** Daily at 2:00 AM UTC
**Retention:** 30 days
**Location:** `/var/backups/inventory-mongodb/`

#### Enable Automated Backups
```properties
# In application.properties
mongodb.backup.enabled=true
mongodb.backup.directory=/var/backups/inventory-mongodb
mongodb.backup.retention-days=30
mongodb.backup.schedule=0 0 2 * * *
```

#### Manual Backup
```bash
# Using mongodump
mongodump --uri="mongodb://localhost:27017/inventorydb" \
  --out=/var/backups/inventory-mongodb/manual_$(date +%Y%m%d_%H%M%S) \
  --gzip

# Verify backup
ls -lh /var/backups/inventory-mongodb/
```

#### Restore from Backup
```bash
# 1. Stop the application
kubectl scale deployment inventory-service --replicas=0

# 2. Restore the database
mongorestore --uri="mongodb://localhost:27017/inventorydb" \
  --gzip \
  --drop \
  /var/backups/inventory-mongodb/backup_20250105_020000

# 3. Verify restore
mongosh mongodb://localhost:27017/inventorydb --eval "db.product_stocks.countDocuments()"

# 4. Restart the application
kubectl scale deployment inventory-service --replicas=3
```

#### Backup Verification Checklist
- [ ] Backup file exists and is not empty
- [ ] Backup is compressed (.gz extension)
- [ ] Backup size is reasonable (compare with previous backups)
- [ ] Test restore on non-production environment monthly

---

## Index Management

### View Current Indexes
```javascript
// Connect to MongoDB
mongosh mongodb://localhost:27017/inventorydb

// View indexes for each collection
db.product_stocks.getIndexes()
db.inventory_ledger.getIndexes()
db.outbox_events.getIndexes()
db.serial_numbers.getIndexes()
db.stock_transfers.getIndexes()
db.containers.getIndexes()
db.inventory_snapshots.getIndexes()
```

### Index Performance Analysis
```javascript
// Check index usage statistics
db.product_stocks.aggregate([
  { $indexStats: {} }
])

// Explain query execution plan
db.product_stocks.find({ sku: "SKU-12345" }).explain("executionStats")
```

### Rebuild Indexes
```javascript
// If indexes are fragmented or corrupted
db.product_stocks.reIndex()
db.inventory_ledger.reIndex()
```

**Note:** The application automatically creates all required indexes on startup via `MongoIndexConfiguration`.

### Critical Indexes

#### product_stocks
- `sku_idx` - Unique index on SKU (primary lookup)
- `abc_class_idx` - ABC classification queries
- `low_stock_alert_idx` - Low stock monitoring
- `serial_tracked_idx` - Serial number filtering

#### inventory_ledger
- `sku_timestamp_idx` - Ledger queries by SKU and time
- `change_type_timestamp_idx` - Analytics by change type
- `operator_timestamp_idx` - Audit trail queries
- `ledger_ttl_idx` - Automatic cleanup (2 year retention)

#### outbox_events
- `unpublished_events_idx` - Event publishing queue
- `aggregate_events_idx` - Event sourcing queries
- `published_events_ttl_idx` - Cleanup published events (7 days)

---

## Connection Pool Tuning

### Current Settings
```properties
spring.data.mongodb.pool.min-size=10
spring.data.mongodb.pool.max-size=100
spring.data.mongodb.pool.max-wait-time-ms=5000
spring.data.mongodb.pool.max-connection-idle-time-ms=60000
spring.data.mongodb.pool.max-connection-life-time-ms=1800000
spring.data.mongodb.pool.maintenance-frequency-ms=10000
```

### Monitor Connection Pool
```bash
# View application logs for pool statistics
kubectl logs -f deployment/inventory-service | grep "ConnectionPool"

# Check MongoDB current connections
mongosh mongodb://localhost:27017/admin --eval "db.serverStatus().connections"
```

### Tune for High Load
```properties
# Increase pool size
spring.data.mongodb.pool.min-size=25
spring.data.mongodb.pool.max-size=200

# Reduce wait time for faster failure
spring.data.mongodb.pool.max-wait-time-ms=3000
```

### Tune for Low Memory
```properties
# Reduce pool size
spring.data.mongodb.pool.min-size=5
spring.data.mongodb.pool.max-size=50

# Aggressive connection cleanup
spring.data.mongodb.pool.max-connection-idle-time-ms=30000
```

---

## Performance Troubleshooting

### Slow Query Analysis

#### 1. Enable MongoDB Profiling
```javascript
// Enable profiling for slow queries (>100ms)
db.setProfilingLevel(1, { slowms: 100 })

// View slow queries
db.system.profile.find().sort({ ts: -1 }).limit(10).pretty()

// Disable profiling
db.setProfilingLevel(0)
```

#### 2. Check Current Operations
```javascript
// View currently running operations
db.currentOp()

// Kill long-running operation
db.killOp(<opid>)
```

#### 3. Analyze Query Plans
```javascript
// Explain specific slow query
db.product_stocks.find({
  quantityOnHand: { $lt: 100 }
}).explain("executionStats")

// Look for:
// - executionTimeMillis > 100
// - totalDocsExamined >> nReturned (table scan)
// - stage: "COLLSCAN" (missing index)
```

### Common Issues and Solutions

#### Issue: High CPU Usage
**Symptoms:**
- MongoDB CPU >80%
- Slow query responses
- Application timeouts

**Diagnosis:**
```javascript
// Check for missing indexes
db.currentOp({ "secs_running": { $gte: 5 } })

// Check for collection scans
db.system.profile.find({ "planSummary": /COLLSCAN/ })
```

**Solution:**
1. Add missing indexes
2. Optimize queries to use existing indexes
3. Consider sharding for large collections

#### Issue: High Memory Usage
**Symptoms:**
- MongoDB memory >90%
- Frequent page faults
- OOM kills

**Diagnosis:**
```javascript
// Check working set size
db.serverStatus().wiredTiger.cache

// Check index size
db.stats()
```

**Solution:**
1. Increase MongoDB RAM allocation
2. Reduce connection pool size
3. Archive old data (ledger cleanup)

#### Issue: Connection Pool Exhaustion
**Symptoms:**
- "Pool timeout" errors
- Requests waiting for connections
- High latency

**Diagnosis:**
```bash
# Check pool statistics in logs
kubectl logs deployment/inventory-service | grep "connection checkout failed"
```

**Solution:**
1. Increase max pool size
2. Reduce connection idle time
3. Check for connection leaks in code

---

## Data Migration

### Export Production Data
```bash
# Export specific collections
mongoexport --uri="mongodb://prod:27017/inventorydb" \
  --collection=product_stocks \
  --out=product_stocks.json

# Export all data
mongodump --uri="mongodb://prod:27017/inventorydb" \
  --out=/tmp/prod_export \
  --gzip
```

### Import to Staging
```bash
# Import specific collection
mongoimport --uri="mongodb://staging:27017/inventorydb" \
  --collection=product_stocks \
  --file=product_stocks.json

# Import full backup
mongorestore --uri="mongodb://staging:27017/inventorydb" \
  --gzip \
  /tmp/prod_export
```

### Data Anonymization (for non-prod environments)
```javascript
// Anonymize customer references
db.serial_numbers.updateMany(
  { customerId: { $exists: true } },
  { $set: { customerId: "ANONYMIZED" } }
)

// Anonymize operator IDs
db.inventory_ledger.updateMany(
  {},
  { $set: { operatorId: "ANONYMIZED" } }
)
```

---

## Monitoring and Alerts

### Key Metrics to Monitor
1. **Connection Pool:**
   - Current connections
   - Connection wait time
   - Pool exhaustion events

2. **Query Performance:**
   - Avg query time
   - Slow query count (>100ms)
   - Index hit ratio

3. **Storage:**
   - Database size
   - Index size
   - Disk usage

4. **Replication (if applicable):**
   - Replication lag
   - Oplog size
   - Secondary health

### Alert Thresholds
```yaml
alerts:
  - name: HighQueryTime
    condition: avg_query_time > 100ms
    severity: warning

  - name: PoolExhaustion
    condition: connection_wait_events > 10/min
    severity: critical

  - name: LowDiskSpace
    condition: disk_usage > 80%
    severity: warning

  - name: ReplicationLag
    condition: replication_lag > 10s
    severity: critical
```

---

## Emergency Procedures

### Database Corruption
1. Stop application immediately
2. Create emergency backup
3. Run repair: `mongod --repair`
4. Validate collections: `db.collection.validate()`
5. Restore from last known good backup if needed

### Accidental Data Deletion
1. DO NOT write to database
2. Identify backup containing deleted data
3. Restore to temporary database
4. Export deleted data
5. Import to production with careful validation

### Performance Degradation
1. Enable profiling to identify slow queries
2. Check index usage
3. Scale horizontally (add replicas)
4. Consider read preference (primary vs secondary)
5. Implement caching layer if needed

---

## Maintenance Windows

### Monthly Maintenance Checklist
- [ ] Review slow query logs
- [ ] Analyze index usage statistics
- [ ] Test backup restoration
- [ ] Review and optimize connection pool settings
- [ ] Clean up old TTL data
- [ ] Update MongoDB version (if needed)
- [ ] Review disk space trends

### Quarterly Tasks
- [ ] Full database backup test
- [ ] Disaster recovery drill
- [ ] Capacity planning review
- [ ] Security audit (user permissions)
- [ ] Index optimization review
