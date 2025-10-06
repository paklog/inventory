package com.paklog.inventory.domain.model;

/**
 * Resolution action for count variances.
 */
public enum VarianceResolution {
    /**
     * Adjust system to match count
     */
    ADJUST_SYSTEM,

    /**
     * Recount required
     */
    RECOUNT,

    /**
     * Accept variance as shrinkage
     */
    ACCEPT_SHRINKAGE,

    /**
     * Investigate discrepancy
     */
    INVESTIGATE,

    /**
     * No action needed (within tolerance)
     */
    NO_ACTION
}
