package com.paklog.inventory.application.dto;

import java.util.List;

public class InventoryHealthMetricsResponse {
    private double inventoryTurnover;
    private List<String> deadStockSkus;
    private long totalSkus;
    private long outOfStockSkus;

    // Private constructor for factory method
    private InventoryHealthMetricsResponse(double inventoryTurnover, List<String> deadStockSkus, long totalSkus, long outOfStockSkus) {
        this.inventoryTurnover = inventoryTurnover;
        this.deadStockSkus = deadStockSkus;
        this.totalSkus = totalSkus;
        this.outOfStockSkus = outOfStockSkus;
    }

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