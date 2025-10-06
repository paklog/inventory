package com.paklog.inventory.domain.model;

/**
 * Represents the quality and availability status of inventory.
 * Critical for inventory segregation and proper ATP calculations.
 *
 * Industry standard: SAP IM Quality Inspection Status, Oracle Subinventory Status
 */
public enum StockStatus {

    /**
     * Unrestricted stock available for allocation and picking
     */
    AVAILABLE("Available for sale"),

    /**
     * Stock under quality inspection, cannot be allocated
     */
    QUARANTINE("Quality inspection pending"),

    /**
     * Damaged or defective stock, not available for sale
     */
    DAMAGED("Damaged or defective"),

    /**
     * Stock on hold for specific business reasons
     */
    ON_HOLD("On hold - blocked"),

    /**
     * Expired lot/batch, cannot be sold
     */
    EXPIRED("Lot expired"),

    /**
     * Customer return, pending inspection and disposition
     */
    RETURNED("Customer return - pending inspection"),

    /**
     * Reserved for specific purpose (e.g., VIP customer, recall)
     */
    RESERVED("Hard reserved"),

    /**
     * Allocated to order but not yet picked
     */
    ALLOCATED("Soft allocated to order"),

    /**
     * In transit between locations
     */
    IN_TRANSIT("In transit");

    private final String description;

    StockStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determine if stock in this status can be allocated to orders
     */
    public boolean isAvailableToPromise() {
        return this == AVAILABLE;
    }

    /**
     * Determine if stock in this status counts toward on-hand inventory
     */
    public boolean countsAsOnHand() {
        return this != IN_TRANSIT;
    }

    /**
     * Determine if stock in this status is sellable
     */
    public boolean isSellable() {
        return this == AVAILABLE || this == ALLOCATED;
    }
}
