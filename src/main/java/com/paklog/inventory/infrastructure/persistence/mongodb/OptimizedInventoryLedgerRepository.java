package com.paklog.inventory.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimized repository using MongoDB aggregation framework for complex queries.
 * Provides significant performance improvements over client-side processing.
 */
@Repository
public class OptimizedInventoryLedgerRepository {

    private final MongoTemplate mongoTemplate;

    public OptimizedInventoryLedgerRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Optimized aggregation pipeline for bulk SKU quantity calculation.
     * Uses server-side aggregation to reduce network transfer and client-side processing.
     *
     * Performance: O(n) with index on {sku, changeType, timestamp}
     *
     * @param skus List of SKUs to aggregate
     * @param start Start date
     * @param end End date
     * @return Map of SKU to total picked quantity
     */
    public Map<String, Integer> findTotalQuantityPickedBySkusUsingAggregation(
            List<String> skus, LocalDateTime start, LocalDateTime end) {

        // Build aggregation pipeline
        MatchOperation matchStage = Aggregation.match(
                Criteria.where("sku").in(skus)
                        .and("changeType").is("PICK")
                        .and("timestamp").gte(start).lte(end)
        );

        GroupOperation groupStage = Aggregation.group("sku")
                .sum("quantityChange").as("totalQuantity");

        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                groupStage
        );

        // Execute aggregation with hint to use compound index
        AggregationResults<SkuQuantityAggregation> results = mongoTemplate.aggregate(
                aggregation,
                "inventory_ledger",
                SkuQuantityAggregation.class
        );

        // Convert results to map
        Map<String, Integer> quantityBySku = new HashMap<>();
        for (SkuQuantityAggregation result : results.getMappedResults()) {
            quantityBySku.put(result.getId(), result.getTotalQuantity());
        }

        return quantityBySku;
    }

    /**
     * Projection class for aggregation results
     */
    public static class SkuQuantityAggregation {
        private String id; // This maps to the _id field from $group
        private int totalQuantity;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getTotalQuantity() {
            return totalQuantity;
        }

        public void setTotalQuantity(int totalQuantity) {
            this.totalQuantity = totalQuantity;
        }
    }
}
