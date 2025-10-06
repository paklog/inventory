package com.paklog.inventory.domain.model;

/**
 * Status of a serialized inventory unit.
 */
public enum SerialStatus {

    /**
     * In warehouse inventory, available for allocation
     */
    IN_INVENTORY,

    /**
     * Allocated to an order but not yet shipped
     */
    ALLOCATED,

    /**
     * Shipped to customer
     */
    SHIPPED,

    /**
     * Returned from customer
     */
    RETURNED,

    /**
     * Under quality inspection
     */
    QUARANTINE,

    /**
     * Damaged or defective
     */
    DAMAGED,

    /**
     * Scrapped or disposed
     */
    SCRAPPED,

    /**
     * In transit between locations
     */
    IN_TRANSIT;

    /**
     * Check if this status allows allocation
     */
    public boolean canBeAllocated() {
        return this == IN_INVENTORY;
    }
}
