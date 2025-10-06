package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.StockStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Optimized MongoDB queries and aggregations for inventory operations.
 * Uses MongoDB aggregation framework for complex queries with better performance.
 */
@Component
public class OptimizedInventoryQueries {

    private final MongoTemplate mongoTemplate;

    public OptimizedInventoryQueries(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Find products with low stock using optimized query.
     * Uses compound index on quantityOnHand and lastUpdated.
     */
    public List<ProductStockDocument> findLowStockProducts(int threshold) {
        Query query = new Query()
                .addCriteria(Criteria.where("quantityOnHand").lt(threshold))
                .limit(100); // Limit results for performance

        return mongoTemplate.find(query, ProductStockDocument.class, "product_stocks");
    }

    /**
     * Aggregate stock by ABC classification.
     * Uses aggregation framework for efficient grouping.
     */
    public Map<String, Object> aggregateStockByABCClass() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("abcClassification").exists(true)),
                Aggregation.group("abcClassification.abcClass")
                        .count().as("productCount")
                        .sum("quantityOnHand").as("totalQuantity")
                        .avg("quantityOnHand").as("avgQuantity")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "product_stocks", Map.class);

        return Map.of("results", results.getMappedResults());
    }

    /**
     * Find inventory ledger entries by SKU within date range.
     * Uses compound index on sku and timestamp.
     */
    public List<InventoryLedgerEntryDocument> findLedgerEntriesBySku(
            String sku, LocalDateTime startDate, LocalDateTime endDate) {

        Query query = new Query()
                .addCriteria(Criteria.where("sku").is(sku)
                        .and("timestamp").gte(startDate).lte(endDate))
                .limit(1000); // Limit for performance

        return mongoTemplate.find(query, InventoryLedgerEntryDocument.class, "inventory_ledger");
    }

    /**
     * Aggregate stock movements by change type.
     * Useful for analytics and reporting.
     */
    public Map<String, Object> aggregateStockMovementsByType(LocalDateTime startDate, LocalDateTime endDate) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("timestamp").gte(startDate).lte(endDate)),
                Aggregation.group("changeType")
                        .count().as("transactionCount")
                        .sum("quantityChange").as("totalQuantityChange")
                        .avg("quantityChange").as("avgQuantityChange")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "inventory_ledger", Map.class);

        return Map.of("results", results.getMappedResults());
    }

    /**
     * Find unpublished outbox events efficiently.
     * Uses compound index on published and createdAt.
     */
    public List<Map> findUnpublishedEvents(int limit) {
        Query query = new Query()
                .addCriteria(Criteria.where("published").is(false))
                .limit(limit);

        return mongoTemplate.find(query, Map.class, "outbox_events");
    }

    /**
     * Aggregate stock by status.
     * Provides overview of available, reserved, quarantined stock.
     */
    public Map<String, Object> aggregateStockByStatus() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.project()
                        .and("sku").as("sku")
                        .and("stockByStatus").as("stockByStatus"),
                Aggregation.unwind("stockByStatus"),
                Aggregation.group("stockByStatus")
                        .count().as("productCount")
                        .sum("stockByStatus.quantity").as("totalQuantity")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "product_stocks", Map.class);

        return Map.of("results", results.getMappedResults());
    }

    /**
     * Find products by ABC class with pagination.
     * Uses index on abcClassification.abcClass.
     */
    public List<ProductStockDocument> findProductsByABCClass(String abcClass, int page, int size) {
        Query query = new Query()
                .addCriteria(Criteria.where("abcClassification.abcClass").is(abcClass))
                .skip((long) page * size)
                .limit(size);

        return mongoTemplate.find(query, ProductStockDocument.class, "product_stocks");
    }

    /**
     * Find serial numbers by SKU and status.
     * Uses compound index on sku and status.
     */
    public List<SerialNumberDocument> findSerialNumbersBySkuAndStatus(String sku, String status) {
        Query query = new Query()
                .addCriteria(Criteria.where("sku").is(sku)
                        .and("status").is(status))
                .limit(100);

        return mongoTemplate.find(query, SerialNumberDocument.class, "serial_numbers");
    }

    /**
     * Aggregate stock transfers by status.
     * Useful for monitoring pending transfers.
     */
    public Map<String, Object> aggregateTransfersByStatus() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("status")
                        .count().as("transferCount")
                        .sum("quantity").as("totalQuantity")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "stock_transfers", Map.class);

        return Map.of("results", results.getMappedResults());
    }

    /**
     * Find containers by location and status.
     * Uses compound index for efficient queries.
     */
    public List<ContainerDocument> findContainersByLocationAndStatus(String locationId, String status) {
        Query query = new Query()
                .addCriteria(Criteria.where("locationId").is(locationId)
                        .and("status").is(status));

        return mongoTemplate.find(query, ContainerDocument.class, "containers");
    }
}
