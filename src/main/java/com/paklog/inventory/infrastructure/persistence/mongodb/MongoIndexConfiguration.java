package com.paklog.inventory.infrastructure.persistence.mongodb;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Configures MongoDB indexes for optimal query performance.
 * Creates indexes on application startup to ensure all collections are properly indexed.
 */
@Component
public class MongoIndexConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfiguration.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfiguration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndexes() {
        log.info("Initializing MongoDB indexes...");

        createProductStockIndexes();
        createInventoryLedgerIndexes();
        createOutboxEventIndexes();
        createSerialNumberIndexes();
        createStockTransferIndexes();
        createContainerIndexes();
        createInventorySnapshotIndexes();

        log.info("MongoDB indexes initialized successfully");
    }

    private void createProductStockIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("product_stocks");

        // Note: SKU is the @Id field, so it's already indexed as _id (unique by default)
        // No need to create additional index on sku

        // ABC Classification queries
        indexOps.ensureIndex(new Index()
                .on("abcClassification.abcClass", Sort.Direction.ASC)
                .named("abc_class_idx"));

        // Stock status queries
        indexOps.ensureIndex(new Index()
                .on("stockByStatus", Sort.Direction.ASC)
                .named("stock_status_idx"));

        // Valuation method queries
        indexOps.ensureIndex(new Index()
                .on("valuation.valuationMethod", Sort.Direction.ASC)
                .named("valuation_method_idx"));

        // Serial tracked products
        indexOps.ensureIndex(new Index()
                .on("serialTracked", Sort.Direction.ASC)
                .named("serial_tracked_idx"));

        // Low stock alerts - compound index for efficient queries
        indexOps.ensureIndex(new Index()
                .on("quantityOnHand", Sort.Direction.ASC)
                .on("lastUpdated", Sort.Direction.DESC)
                .named("low_stock_alert_idx"));

        log.info("Created indexes for product_stocks collection");
    }

    private void createInventoryLedgerIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("inventory_ledger");

        // Most common query: ledger by SKU and timestamp (descending for recent first)
        indexOps.ensureIndex(new Index()
                .on("sku", Sort.Direction.ASC)
                .on("timestamp", Sort.Direction.DESC)
                .named("sku_timestamp_idx"));

        // Query by change type for analytics
        indexOps.ensureIndex(new Index()
                .on("changeType", Sort.Direction.ASC)
                .on("timestamp", Sort.Direction.DESC)
                .named("change_type_timestamp_idx"));

        // Query by operator for audit trails
        indexOps.ensureIndex(new Index()
                .on("operatorId", Sort.Direction.ASC)
                .on("timestamp", Sort.Direction.DESC)
                .named("operator_timestamp_idx"));

        // Time-based queries for reporting
        indexOps.ensureIndex(new Index()
                .on("timestamp", Sort.Direction.DESC)
                .named("timestamp_idx"));

        // TTL index for automatic cleanup of old ledger entries (optional - 2 years retention)
        indexOps.ensureIndex(new Index()
                .on("timestamp", Sort.Direction.ASC)
                .expire(730, TimeUnit.DAYS)
                .named("ledger_ttl_idx"));

        log.info("Created indexes for inventory_ledger collection");
    }

    private void createOutboxEventIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("outbox_events");

        // Most critical: unpublished events ordered by creation time
        indexOps.ensureIndex(new Index()
                .on("published", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.ASC)
                .named("unpublished_events_idx"));

        // Query by event type
        indexOps.ensureIndex(new Index()
                .on("eventType", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("event_type_idx"));

        // Query by aggregate ID for event sourcing
        indexOps.ensureIndex(new Index()
                .on("aggregateId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.ASC)
                .named("aggregate_events_idx"));

        // TTL index for published events (cleanup after 7 days)
        indexOps.ensureIndex(new Index()
                .on("createdAt", Sort.Direction.ASC)
                .expire(7, TimeUnit.DAYS)
                .named("published_events_ttl_idx"));

        log.info("Created indexes for outbox_events collection");
    }

    private void createSerialNumberIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("serial_numbers");

        // Primary lookup by serial number
        indexOps.ensureIndex(new Index()
                .on("serialNumber", Sort.Direction.ASC)
                .unique()
                .named("serial_number_unique_idx"));

        // Query by SKU and status
        indexOps.ensureIndex(new Index()
                .on("sku", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("sku_status_idx"));

        // Customer allocation queries
        indexOps.ensureIndex(new Index()
                .on("customerId", Sort.Direction.ASC)
                .sparse()
                .named("customer_idx"));

        // Location-based queries
        indexOps.ensureIndex(new Index()
                .on("locationId", Sort.Direction.ASC)
                .sparse()
                .named("location_idx"));

        log.info("Created indexes for serial_numbers collection");
    }

    private void createStockTransferIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("stock_transfers");

        // Query by status for pending transfers
        indexOps.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("initiatedAt", Sort.Direction.DESC)
                .named("status_initiated_idx"));

        // Query by SKU
        indexOps.ensureIndex(new Index()
                .on("sku", Sort.Direction.ASC)
                .on("initiatedAt", Sort.Direction.DESC)
                .named("sku_transfer_idx"));

        // Query by source/destination locations
        indexOps.ensureIndex(new Index()
                .on("fromLocationId", Sort.Direction.ASC)
                .named("from_location_idx"));

        indexOps.ensureIndex(new Index()
                .on("toLocationId", Sort.Direction.ASC)
                .named("to_location_idx"));

        log.info("Created indexes for stock_transfers collection");
    }

    private void createContainerIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("containers");

        // Container ID unique index
        indexOps.ensureIndex(new Index()
                .on("containerId", Sort.Direction.ASC)
                .unique()
                .named("container_id_unique_idx"));

        // Query by status
        indexOps.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .named("container_status_idx"));

        // Query by location
        indexOps.ensureIndex(new Index()
                .on("locationId", Sort.Direction.ASC)
                .named("container_location_idx"));

        // Query by type
        indexOps.ensureIndex(new Index()
                .on("containerType", Sort.Direction.ASC)
                .named("container_type_idx"));

        log.info("Created indexes for containers collection");
    }

    private void createInventorySnapshotIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("inventory_snapshots");

        // Query by SKU and snapshot date
        indexOps.ensureIndex(new Index()
                .on("sku", Sort.Direction.ASC)
                .on("snapshotDate", Sort.Direction.DESC)
                .named("sku_snapshot_date_idx"));

        // Query by snapshot type and date
        indexOps.ensureIndex(new Index()
                .on("snapshotType", Sort.Direction.ASC)
                .on("snapshotDate", Sort.Direction.DESC)
                .named("type_snapshot_date_idx"));

        // Query by reason
        indexOps.ensureIndex(new Index()
                .on("reason", Sort.Direction.ASC)
                .on("snapshotDate", Sort.Direction.DESC)
                .named("reason_snapshot_date_idx"));

        log.info("Created indexes for inventory_snapshots collection");
    }
}
