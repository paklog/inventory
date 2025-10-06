package com.paklog.inventory.domain.model;

/**
 * Classification criteria for ABC analysis.
 */
public enum ABCCriteria {

    /**
     * Pure value-based classification (Annual Usage Value)
     * Traditional Pareto analysis
     */
    VALUE_BASED(12),

    /**
     * Movement velocity based (frequency of picks/issues)
     */
    VELOCITY_BASED(6),

    /**
     * Business criticality based (strategic importance)
     */
    CRITICALITY_BASED(12),

    /**
     * Combined approach (weighted: value, velocity, criticality)
     * Recommended for most operations
     */
    COMBINED(6);

    private final int validityMonths;

    ABCCriteria(int validityMonths) {
        this.validityMonths = validityMonths;
    }

    public int getValidityMonths() {
        return validityMonths;
    }
}
