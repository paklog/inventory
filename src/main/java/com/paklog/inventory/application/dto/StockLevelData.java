package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.StockLevel;

/**
 * Stock level data structure for CloudEvents.
 * Matches the AsyncAPI specification schema.
 *
 * With Jackson's SNAKE_CASE naming strategy, this will serialize to:
 * {
 *   "quantity_on_hand": 100,
 *   "quantity_allocated": 20,
 *   "available_to_promise": 80
 * }
 */
public class StockLevelData {
    private int quantityOnHand;
    private int quantityAllocated;
    private int availableToPromise;

    // Private constructor for factory method
    private StockLevelData(int quantityOnHand, int quantityAllocated, int availableToPromise) {
        this.quantityOnHand = quantityOnHand;
        this.quantityAllocated = quantityAllocated;
        this.availableToPromise = availableToPromise;
    }

    public static StockLevelData fromDomain(StockLevel stockLevel) {
        return new StockLevelData(
                stockLevel.getQuantityOnHand(),
                stockLevel.getQuantityAllocated(),
                stockLevel.getAvailableToPromise()
        );
    }

    // Getters
    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityAllocated() {
        return quantityAllocated;
    }

    public int getAvailableToPromise() {
        return availableToPromise;
    }
}
