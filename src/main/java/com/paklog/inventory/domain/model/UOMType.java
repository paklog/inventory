package com.paklog.inventory.domain.model;

/**
 * Types of units of measure.
 */
public enum UOMType {
    /**
     * Discrete/countable units (EA, CASE, PALLET, BOX)
     */
    DISCRETE,

    /**
     * Weight-based units (KG, LB, OZ, TON)
     */
    WEIGHT,

    /**
     * Volume-based units (L, GAL, ML, CBM)
     */
    VOLUME,

    /**
     * Length-based units (M, FT, CM, IN)
     */
    LENGTH
}
