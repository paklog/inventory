package com.paklog.inventory.domain.model;

/**
 * Standardized reason codes for inventory stock adjustments.
 * Based on industry standards from SAP, Oracle, Shopify, and Microsoft Dynamics.
 */
public enum ReasonCode {

    // ========== Inbound Operations ==========

    /** Stock received from supplier/vendor */
    PURCHASE_RECEIPT("Inbound", "Stock received from supplier purchase order"),

    /** Customer return to stock */
    RETURN_TO_STOCK("Inbound", "Customer return received and restocked"),

    /** Transfer received from another location/warehouse */
    TRANSFER_IN("Inbound", "Stock transferred in from another location"),

    /** Manufactured/assembled items completed */
    PRODUCTION_COMPLETE("Inbound", "Production or assembly completed"),

    /** Stock found during physical count */
    FOUND_STOCK("Inbound", "Stock found that was not previously recorded"),

    // ========== Outbound Operations ==========

    /** Regular sale fulfillment */
    SALE("Outbound", "Stock sold to customer"),

    /** Damaged goods write-off */
    DAMAGE("Outbound", "Stock damaged and written off"),

    /** Lost or stolen inventory */
    THEFT_LOSS("Outbound", "Stock lost due to theft or shrinkage"),

    /** Transfer sent to another location/warehouse */
    TRANSFER_OUT("Outbound", "Stock transferred out to another location"),

    /** Obsolete or expired items disposal */
    DISPOSAL("Outbound", "Stock disposed due to obsolescence or expiry"),

    /** Stock consumed in production */
    PRODUCTION_CONSUMPTION("Outbound", "Stock consumed in production process"),

    /** Sample or promotional items */
    SAMPLE("Outbound", "Stock used for samples or promotional purposes"),

    // ========== Adjustment Operations ==========

    /** Physical count correction (cycle count or annual inventory) */
    PHYSICAL_COUNT("Adjustment", "Stock adjusted based on physical count"),

    /** Cycle count adjustment */
    CYCLE_COUNT("Adjustment", "Stock adjusted based on cycle count"),

    /** System data correction */
    SYSTEM_CORRECTION("Adjustment", "System data correction or reconciliation"),

    /** Inventory revaluation */
    REVALUATION("Adjustment", "Inventory value revaluation"),

    /** Quality control hold or release */
    QC_ADJUSTMENT("Adjustment", "Quality control related adjustment"),

    /** Expiry date adjustment */
    EXPIRY_ADJUSTMENT("Adjustment", "Adjustment for expired or near-expiry stock"),

    // ========== Operational Reasons ==========

    /** Stock picked for order fulfillment */
    ITEM_PICKED("Operational", "Stock picked for order fulfillment"),

    /** Stock allocation for order */
    ALLOCATION("Operational", "Stock allocated to order"),

    /** Stock deallocation (order cancelled) */
    DEALLOCATION("Operational", "Stock deallocated from cancelled order"),

    /** Stock reserved for future use */
    RESERVATION("Operational", "Stock reserved for future order or use"),

    /** Initial stock setup */
    INITIAL_STOCK("Operational", "Initial stock level setup"),

    /** Stock receipt processing */
    STOCK_RECEIPT("Operational", "Stock receipt from purchase order"),

    // ========== Quality & Status Changes ==========

    /** Stock quarantined for quality inspection */
    QUARANTINE("Quality", "Stock quarantined for quality inspection"),

    /** Stock released from quarantine */
    QUARANTINE_RELEASE("Quality", "Stock released from quarantine"),

    /** Stock rejected by quality control */
    QC_REJECT("Quality", "Stock rejected by quality control"),

    /** Stock approved by quality control */
    QC_APPROVE("Quality", "Stock approved by quality control"),

    // ========== Integration & System ==========

    /** External system integration sync */
    INTEGRATION_SYNC("System", "Stock synchronized from external system"),

    /** Migration or data import */
    DATA_MIGRATION("System", "Stock data migration or import"),

    /** Manual override by administrator */
    MANUAL_OVERRIDE("System", "Manual stock override by administrator");

    private final String category;
    private final String description;

    ReasonCode(String category, String description) {
        this.category = category;
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get display name (replaces underscores with spaces and title case)
     */
    public String getDisplayName() {
        return name().replace('_', ' ');
    }

    /**
     * Check if this is an inbound operation (increases stock)
     */
    public boolean isInbound() {
        return "Inbound".equals(category);
    }

    /**
     * Check if this is an outbound operation (decreases stock)
     */
    public boolean isOutbound() {
        return "Outbound".equals(category);
    }

    /**
     * Check if this is an adjustment operation (can increase or decrease)
     */
    public boolean isAdjustment() {
        return "Adjustment".equals(category);
    }

    /**
     * Parse from string with fallback to MANUAL_OVERRIDE for backward compatibility
     */
    public static ReasonCode fromStringOrDefault(String code) {
        if (code == null || code.trim().isEmpty()) {
            return MANUAL_OVERRIDE;
        }

        try {
            return ReasonCode.valueOf(code.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            // For backward compatibility with old string reasons
            return MANUAL_OVERRIDE;
        }
    }

    /**
     * Check if the code is valid
     */
    public static boolean isValid(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }

        try {
            ReasonCode.valueOf(code.toUpperCase().replace(' ', '_'));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
