package com.paklog.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for inventory health metrics analysis.
 *
 * Provides key performance indicators for inventory management including:
 * - Inventory turnover rate: Measures how quickly inventory is sold/used
 * - Dead stock identification: SKUs with no movement over a period
 * - Out-of-stock analysis: SKUs currently unavailable for fulfillment
 *
 * These metrics help identify optimization opportunities, reduce carrying costs,
 * and improve inventory efficiency. Corresponds to user story INV-11.
 */
public class InventoryHealthMetricsResponse {

    @JsonProperty("inventory_turnover")
    private double inventoryTurnover;

    @JsonProperty("dead_stock_skus")
    private List<String> deadStockSkus;

    @JsonProperty("total_skus")
    private long totalSkus;

    @JsonProperty("out_of_stock_skus")
    private long outOfStockSkus;

    // Private constructor for factory method
    private InventoryHealthMetricsResponse(double inventoryTurnover, List<String> deadStockSkus, long totalSkus, long outOfStockSkus) {
        this.inventoryTurnover = inventoryTurnover;
        this.deadStockSkus = deadStockSkus;
        this.totalSkus = totalSkus;
        this.outOfStockSkus = outOfStockSkus;
    }

    /**
     * Factory method to create an InventoryHealthMetricsResponse.
     *
     * @param inventoryTurnover the inventory turnover rate
     * @param deadStockSkus list of SKUs identified as dead stock
     * @param totalSkus total number of SKUs in the system
     * @param outOfStockSkus number of SKUs currently out of stock
     * @return a new InventoryHealthMetricsResponse instance
     */
    public static InventoryHealthMetricsResponse of(double inventoryTurnover, List<String> deadStockSkus, long totalSkus, long outOfStockSkus) {
        return new InventoryHealthMetricsResponse(inventoryTurnover, deadStockSkus, totalSkus, outOfStockSkus);
    }

    // Getters
    public double getInventoryTurnover() {
        return inventoryTurnover;
    }

    public List<String> getDeadStockSkus() {
        return deadStockSkus;
    }

    public long getTotalSkus() {
        return totalSkus;
    }

    public long getOutOfStockSkus() {
        return outOfStockSkus;
    }
}