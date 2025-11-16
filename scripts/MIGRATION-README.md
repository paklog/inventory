# Database Migration: stockByStatus as Source of Truth

## Overview

This migration changes how inventory quantities are stored in MongoDB:

**Before:**
```json
{
  "_id": "Bottle-W-021",
  "quantityOnHand": 938,        // Separate field (DEPRECATED)
  "quantityAllocated": 0,
  "stockByStatus": {}           // Empty - causes ATP = 0 bug!
}
```

**After:**
```json
{
  "_id": "Bottle-W-021",
  "quantityAllocated": 0,
  "stockByStatus": {
    "AVAILABLE": 938            // SOURCE OF TRUTH
  }
}
```

## Why This Migration?

1. **Bug Fix**: Empty `stockByStatus` caused `availableToPromise` to be 0
2. **Single Source of Truth**: Eliminates data inconsistency
3. **Better Status Tracking**: Physical stock status (AVAILABLE, DAMAGED, QUARANTINE) is now explicit

## How to Run

### Prerequisites
- MongoDB running
- mongosh or mongo shell available
- Backup your database first!

### Backup (IMPORTANT!)
```bash
mongodump --db inventorydb --collection product_stocks --out backup/
```

### Execute Migration
```bash
# Using mongosh
mongosh inventorydb < scripts/migrate-stock-by-status.js

# Or with connection string
mongosh mongodb://localhost:27017/inventorydb < scripts/migrate-stock-by-status.js

# Or with authentication
mongosh mongodb://user:password@localhost:27017/inventorydb < scripts/migrate-stock-by-status.js
```

### Expected Output
```
=== Stock By Status Migration ===
Starting migration at: 2025-11-16T...

Step 1: Analyzing documents...
Documents needing migration: 20
Documents already migrated: 0

Step 2: Migrating documents with empty stockByStatus...
  Migrated: BOOK-PB-001 (QOH: 1163 -> AVAILABLE: 1163)
  Migrated: PHONE-S-002 (QOH: 1838 -> AVAILABLE: 1838)
  ...

Step 3: Removing deprecated quantityOnHand field...
  Cleaned up 0 documents

Step 4: Validating migration...

=== Migration Summary ===
Total documents: 20
Migrated: 20
Skipped: 0
Errors: 0

Validation:
  Documents with valid stockByStatus: 20
  Documents still needing migration: 0
  Documents with deprecated quantityOnHand: 0

✅ Migration completed successfully!
```

## Rollback (If Needed)

```bash
mongorestore --db inventorydb --collection product_stocks backup/inventorydb/product_stocks.bson --drop
```

## Verification Queries

After migration, verify with:

```javascript
// Check a specific document
db.product_stocks.findOne({ _id: "Bottle-W-021" });

// Verify no deprecated fields remain
db.product_stocks.countDocuments({ quantityOnHand: { $exists: true } });
// Should return 0

// Verify all documents have valid stockByStatus
db.product_stocks.countDocuments({
  stockByStatus: { $ne: {}, $exists: true }
});
// Should match total document count

// Calculate ATP for a document
var doc = db.product_stocks.findOne({ _id: "Bottle-W-021" });
var qoh = Object.values(doc.stockByStatus).reduce((a, b) => a + b, 0);
var available = doc.stockByStatus["AVAILABLE"] || 0;
var allocated = doc.quantityAllocated;
var atp = Math.min(qoh - allocated, available);
print("ATP: " + atp);
```

## Application Code Changes

The application code has been updated to:

1. **ProductStockDocument.java**
   - Removed `quantityOnHand` field
   - `stockByStatus` is now source of truth
   - `getQuantityOnHand()` derives from `stockByStatus.values().sum()`

2. **ProductStock.java**
   - Added `loadComplete()` factory method for full reconstruction
   - Added validation: `quantityAllocated <= stockByStatus[AVAILABLE]`

## Testing

Run the new unit tests:
```bash
mvn test -Dtest=ProductStockDocumentTest
```

## Monitoring

After migration, monitor:
- API responses for correct ATP values
- No `StockLevelInvariantViolationException` errors
- Performance metrics for any degradation
