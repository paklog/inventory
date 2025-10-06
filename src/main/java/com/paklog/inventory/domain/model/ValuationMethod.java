package com.paklog.inventory.domain.model;

/**
 * Inventory valuation methods for cost accounting.
 *
 * Industry standard: GAAP, IFRS accounting principles
 */
public enum ValuationMethod {

    /**
     * First In, First Out
     * Assumes oldest inventory is consumed first
     * Most common in industries with perishable goods
     */
    FIFO("First In, First Out"),

    /**
     * Last In, First Out
     * Assumes newest inventory is consumed first
     * Less common due to tax implications and IFRS prohibition
     */
    LIFO("Last In, First Out"),

    /**
     * Weighted Average Cost
     * Calculates average cost based on all purchases
     * Smooths out price fluctuations
     */
    WEIGHTED_AVERAGE("Weighted Average Cost"),

    /**
     * Standard Cost
     * Uses predetermined standard cost
     * Variances tracked separately
     */
    STANDARD_COST("Standard Cost");

    private final String description;

    ValuationMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if method is allowed under IFRS
     */
    public boolean isIFRSCompliant() {
        return this != LIFO; // LIFO not allowed under IFRS
    }

    /**
     * Check if method requires cost layer tracking
     */
    public boolean requiresCostLayers() {
        return this == FIFO || this == LIFO;
    }
}
