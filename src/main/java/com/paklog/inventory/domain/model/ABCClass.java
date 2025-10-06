package com.paklog.inventory.domain.model;

/**
 * ABC classification categories for inventory optimization.
 */
public enum ABCClass {

    /**
     * A-items: High value/velocity items
     * Typically 20% of SKUs representing 80% of value
     * Require tight control, frequent counts, high service levels
     */
    A,

    /**
     * B-items: Medium value/velocity items
     * Typically 30% of SKUs representing 15% of value
     * Moderate control, quarterly counts, standard service levels
     */
    B,

    /**
     * C-items: Low value/velocity items
     * Typically 50% of SKUs representing 5% of value
     * Basic control, semi-annual counts, lower service levels acceptable
     */
    C
}
