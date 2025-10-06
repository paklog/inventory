package com.paklog.inventory.domain.model;

/**
 * Status lifecycle for stock transfers between locations.
 */
public enum TransferStatus {

    /**
     * Transfer created but stock not yet removed from source
     */
    INITIATED,

    /**
     * Stock removed from source location, in transit to destination
     */
    IN_TRANSIT,

    /**
     * Stock received at destination location
     */
    COMPLETED,

    /**
     * Transfer cancelled before completion
     */
    CANCELLED;

    /**
     * Check if transfer is in a final state (no further transitions)
     */
    public boolean isFinalStatus() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * Check if inventory is currently in-transit
     */
    public boolean isInTransit() {
        return this == IN_TRANSIT;
    }
}
