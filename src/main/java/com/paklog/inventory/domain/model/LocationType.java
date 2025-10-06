package com.paklog.inventory.domain.model;

/**
 * Types of physical warehouse locations based on storage characteristics.
 */
public enum LocationType {
    /**
     * Pallet rack location for pallet-level storage
     */
    PALLET_RACK,

    /**
     * Flow rack location for case picking
     */
    FLOW_RACK,

    /**
     * Shelving location for piece picking
     */
    SHELVING,

    /**
     * Bulk floor location for large items
     */
    BULK_FLOOR,

    /**
     * Bin location for small parts
     */
    BIN,

    /**
     * Mezzanine storage
     */
    MEZZANINE,

    /**
     * Drive-in rack for LIFO storage
     */
    DRIVE_IN_RACK,

    /**
     * Drive-through rack for FIFO storage
     */
    DRIVE_THROUGH_RACK,

    /**
     * Cantilever rack for long items
     */
    CANTILEVER_RACK,

    /**
     * Push-back rack
     */
    PUSH_BACK_RACK,

    /**
     * Carousel system
     */
    CAROUSEL,

    /**
     * Automated storage and retrieval system (AS/RS)
     */
    ASRS,

    /**
     * Staging area location
     */
    STAGING,

    /**
     * Receiving dock location
     */
    RECEIVING_DOCK,

    /**
     * Shipping dock location
     */
    SHIPPING_DOCK,

    /**
     * Quality control area
     */
    QC_AREA,

    /**
     * General purpose location
     */
    GENERAL
}
