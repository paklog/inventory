// MongoDB Migration Script: Migrate to stockByStatus as Source of Truth
//
// This migration:
// 1. Populates stockByStatus for documents with empty stockByStatus
// 2. Removes the deprecated quantityOnHand field
//
// Usage: mongosh inventorydb < scripts/migrate-stock-by-status.js
// Or: mongosh mongodb://localhost:27017/inventorydb < scripts/migrate-stock-by-status.js

print("=== Stock By Status Migration ===");
print("Starting migration at: " + new Date().toISOString());

// Configuration
const COLLECTION_NAME = "product_stocks";
const BATCH_SIZE = 100;

// Statistics
let totalDocuments = 0;
let migratedDocuments = 0;
let skippedDocuments = 0;
let errorDocuments = 0;

// Step 1: Count documents that need migration
print("\nStep 1: Analyzing documents...");

const needsMigration = db[COLLECTION_NAME].countDocuments({
    $or: [
        { stockByStatus: { $exists: false } },
        { stockByStatus: {} },
        { stockByStatus: { $eq: {} } }
    ]
});

const alreadyMigrated = db[COLLECTION_NAME].countDocuments({
    stockByStatus: { $ne: {}, $exists: true, $type: "object" }
});

print(`Documents needing migration: ${needsMigration}`);
print(`Documents already migrated: ${alreadyMigrated}`);

totalDocuments = needsMigration + alreadyMigrated;

// Step 2: Migrate documents with empty stockByStatus
print("\nStep 2: Migrating documents with empty stockByStatus...");

const cursor = db[COLLECTION_NAME].find({
    $or: [
        { stockByStatus: { $exists: false } },
        { stockByStatus: {} },
        { stockByStatus: { $eq: {} } }
    ]
}).batchSize(BATCH_SIZE);

let batch = [];
let batchCount = 0;

while (cursor.hasNext()) {
    const doc = cursor.next();

    try {
        // Create stockByStatus from quantityOnHand
        const quantityOnHand = doc.quantityOnHand || 0;
        const newStockByStatus = {};

        if (quantityOnHand > 0) {
            newStockByStatus["AVAILABLE"] = quantityOnHand;
        }

        // Update the document
        const result = db[COLLECTION_NAME].updateOne(
            { _id: doc._id },
            {
                $set: { stockByStatus: newStockByStatus },
                $unset: { quantityOnHand: "" }
            }
        );

        if (result.modifiedCount === 1) {
            migratedDocuments++;
            print(`  Migrated: ${doc._id} (QOH: ${quantityOnHand} -> AVAILABLE: ${quantityOnHand})`);
        } else {
            skippedDocuments++;
            print(`  Skipped: ${doc._id} (no changes)`);
        }

        batchCount++;
        if (batchCount % 10 === 0) {
            print(`  Progress: ${batchCount} documents processed...`);
        }

    } catch (error) {
        errorDocuments++;
        print(`  ERROR migrating ${doc._id}: ${error.message}`);
    }
}

// Step 3: Remove quantityOnHand field from already migrated documents
print("\nStep 3: Removing deprecated quantityOnHand field from migrated documents...");

const cleanupResult = db[COLLECTION_NAME].updateMany(
    {
        stockByStatus: { $ne: {}, $exists: true },
        quantityOnHand: { $exists: true }
    },
    { $unset: { quantityOnHand: "" } }
);

print(`  Cleaned up ${cleanupResult.modifiedCount} documents`);

// Step 4: Validate migration
print("\nStep 4: Validating migration...");

const stillNeedsMigration = db[COLLECTION_NAME].countDocuments({
    $or: [
        { stockByStatus: { $exists: false } },
        { stockByStatus: {} }
    ]
});

const stillHasDeprecatedField = db[COLLECTION_NAME].countDocuments({
    quantityOnHand: { $exists: true }
});

const hasValidStockByStatus = db[COLLECTION_NAME].countDocuments({
    stockByStatus: { $ne: {}, $exists: true, $type: "object" }
});

// Step 5: Summary
print("\n=== Migration Summary ===");
print(`Total documents: ${totalDocuments}`);
print(`Migrated: ${migratedDocuments}`);
print(`Skipped: ${skippedDocuments}`);
print(`Errors: ${errorDocuments}`);
print(`\nValidation:`);
print(`  Documents with valid stockByStatus: ${hasValidStockByStatus}`);
print(`  Documents still needing migration: ${stillNeedsMigration}`);
print(`  Documents with deprecated quantityOnHand: ${stillHasDeprecatedField}`);

if (stillNeedsMigration === 0 && stillHasDeprecatedField === 0) {
    print("\n✅ Migration completed successfully!");
} else {
    print("\n⚠️  Migration incomplete. Please review the results.");
}

print("\nCompleted at: " + new Date().toISOString());

// Sample verification query
print("\n=== Sample Verification (first 3 documents) ===");
db[COLLECTION_NAME].find({}).limit(3).forEach(doc => {
    print(`SKU: ${doc._id}`);
    print(`  stockByStatus: ${JSON.stringify(doc.stockByStatus)}`);
    print(`  quantityOnHand (deprecated): ${doc.quantityOnHand || "REMOVED"}`);
    print(`  quantityAllocated: ${doc.quantityAllocated}`);
    print("");
});
