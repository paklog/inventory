package com.paklog.inventory.domain.model;

/**
 * Types of warehouse zones based on function and operational characteristics.
 */
public enum ZoneType {
    /**
     * Primary picking zone for fast-moving items (A items)
     */
    FAST_PICK,

    /**
     * Standard picking zone for regular velocity items
     */
    PICK,

    /**
     * Bulk storage zone for pallet-level storage
     */
    BULK,

    /**
     * Reserve storage zone for overflow inventory
     */
    RESERVE,

    /**
     * Receiving zone for inbound shipments
     */
    RECEIVING,

    /**
     * Staging zone for outbound shipments
     */
    STAGING,

    /**
     * Quarantine zone for quality holds
     */
    QUARANTINE,

    /**
     * Hazardous materials zone with special handling
     */
    HAZMAT,

    /**
     * High-value items requiring security
     */
    HIGH_VALUE,

    /**
     * Returns processing zone
     */
    RETURNS,

    /**
     * Damaged goods zone
     */
    DAMAGE,

    /**
     * Refrigerated zone for temperature-sensitive items
     */
    REFRIGERATED,

    /**
     * Frozen zone for frozen products
     */
    FROZEN,

    /**
     * Cross-dock zone for direct shipping
     */
    CROSS_DOCK,

    /**
     * Value-added services zone (kitting, labeling, etc.)
     */
    VAS,

    /**
     * General purpose zone
     */
    GENERAL
}
