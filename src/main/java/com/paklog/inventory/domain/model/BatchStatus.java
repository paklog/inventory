package com.paklog.inventory.domain.model;

/**
 * Status of a lot/batch of inventory.
 */
public enum BatchStatus {
    /**
     * Available for allocation and picking
     */
    AVAILABLE,

    /**
     * On quality hold - cannot be picked
     */
    QUALITY_HOLD,

    /**
     * On customer hold - reserved for specific customer
     */
    CUSTOMER_HOLD,

    /**
     * Damaged - cannot be picked, awaiting disposition
     */
    DAMAGED,

    /**
     * Quarantined - under inspection or recall
     */
    QUARANTINE,

    /**
     * Expired - past expiry date, cannot be picked
     */
    EXPIRED,

    /**
     * Blocked - administrative or system hold
     */
    BLOCKED
}
