package com.paklog.inventory.domain.model;

/**
 * Type of inventory snapshot.
 * Determines retention policy and granularity.
 */
public enum SnapshotType {
    /**
     * Daily snapshot taken at midnight
     * Retention: 90 days
     */
    DAILY,

    /**
     * Month-end snapshot (last day of month)
     * Retention: 7 years
     */
    MONTH_END,

    /**
     * Quarter-end snapshot (last day of quarter)
     * Retention: 10 years
     */
    QUARTER_END,

    /**
     * Year-end snapshot (December 31)
     * Retention: Permanent
     */
    YEAR_END,

    /**
     * Ad-hoc snapshot created on-demand
     * Retention: 30 days
     */
    AD_HOC
}
