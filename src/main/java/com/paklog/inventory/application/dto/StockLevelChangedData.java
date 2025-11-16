package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.StockLevel;

/**
 * Data payload for StockLevelChanged CloudEvent.
 * Matches the AsyncAPI specification schema with nested stock level structures.
 *
 * With Jackson's SNAKE_CASE naming strategy, this will serialize to:
 * {
 *   "sku": "SKU-12345",
 *   "previous_stock_level": {
 *     "quantity_on_hand": 100,
 *     "quantity_allocated": 20,
 *     "available_to_promise": 80
 *   },
 *   "new_stock_level": {
 *     "quantity_on_hand": 150,
 *     "quantity_allocated": 30,
 *     "available_to_promise": 120
 *   },
 *   "change_reason": "PURCHASE_RECEIPT"
 * }
 */
public class StockLevelChangedData {
    private String sku;
    private StockLevelData previousStockLevel;
    private StockLevelData newStockLevel;
    private String changeReason;

    // Private constructor for factory method
    private StockLevelChangedData(String sku, StockLevelData previousStockLevel,
                                   StockLevelData newStockLevel, String changeReason) {
        this.sku = sku;
        this.previousStockLevel = previousStockLevel;
        this.newStockLevel = newStockLevel;
        this.changeReason = changeReason;
    }

    public static StockLevelChangedData of(String sku, StockLevel previousStockLevel,
                                            StockLevel newStockLevel, String changeReason) {
        return new StockLevelChangedData(
                sku,
                StockLevelData.fromDomain(previousStockLevel),
                StockLevelData.fromDomain(newStockLevel),
                changeReason
        );
    }

    // Getters
    public String getSku() {
        return sku;
    }

    public StockLevelData getPreviousStockLevel() {
        return previousStockLevel;
    }

    public StockLevelData getNewStockLevel() {
        return newStockLevel;
    }

    public String getChangeReason() {
        return changeReason;
    }
}
