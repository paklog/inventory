package com.paklog.inventory.domain.model;

/**
 * Types of cycle count programs.
 */
public enum CountType {
    /**
     * Regular scheduled cycle count based on ABC classification
     */
    SCHEDULED_ABC,

    /**
     * Random location count for validation
     */
    RANDOM,

    /**
     * Directed count for specific SKU or location
     */
    DIRECTED,

    /**
     * Exception-based count triggered by discrepancy
     */
    EXCEPTION,

    /**
     * Blind count where counter doesn't see system quantity
     */
    BLIND,

    /**
     * Full physical inventory count
     */
    PHYSICAL_INVENTORY,

    /**
     * Quality audit count
     */
    QUALITY_AUDIT,

    /**
     * Perpetual inventory count (continuous)
     */
    PERPETUAL
}
