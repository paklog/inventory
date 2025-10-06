package com.paklog.inventory.domain.model;

/**
 * Types of holds that can be placed on inventory.
 *
 * Industry standard: SAP IM Blocking Reasons, Oracle Hold Types
 */
public enum HoldType {

    /**
     * Quality inspection required before release
     */
    QUALITY_HOLD("Quality inspection required"),

    /**
     * Legal or compliance hold (litigation, investigation)
     */
    LEGAL_HOLD("Legal or compliance hold"),

    /**
     * Customer-specific allocation hold
     */
    ALLOCATION_HOLD("Reserved for specific customer"),

    /**
     * Product recall hold
     */
    RECALL_HOLD("Product recall"),

    /**
     * Approaching expiry, pending review
     */
    EXPIRY_HOLD("Approaching expiry date"),

    /**
     * Damage investigation
     */
    DAMAGE_HOLD("Damage investigation"),

    /**
     * Credit hold - customer payment issues
     */
    CREDIT_HOLD("Credit hold - payment required"),

    /**
     * Inventory count in progress
     */
    COUNT_HOLD("Cycle count or physical inventory in progress"),

    /**
     * System or administrative hold
     */
    ADMINISTRATIVE_HOLD("Administrative hold"),

    /**
     * Custom business rule hold
     */
    CUSTOM_HOLD("Custom business hold");

    private final String description;

    HoldType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determine if this hold type requires supervisor approval to release
     */
    public boolean requiresApproval() {
        return this == LEGAL_HOLD ||
               this == RECALL_HOLD ||
               this == CREDIT_HOLD;
    }

    /**
     * Determine if this hold type can auto-expire
     */
    public boolean canAutoExpire() {
        return this == EXPIRY_HOLD ||
               this == COUNT_HOLD ||
               this == ALLOCATION_HOLD;
    }
}
