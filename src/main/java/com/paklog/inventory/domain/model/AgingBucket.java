package com.paklog.inventory.domain.model;

/**
 * Aging buckets for inventory classification based on days since last movement.
 */
public enum AgingBucket {

    /**
     * Fresh inventory, moved within last 30 days
     */
    FRESH_0_30("0-30 days", "Fresh"),

    /**
     * Aging inventory, 31-60 days since last movement
     */
    AGING_31_60("31-60 days", "Aging"),

    /**
     * Slow-moving inventory, 61-90 days since last movement
     */
    SLOW_61_90("61-90 days", "Slow-moving"),

    /**
     * Very slow-moving inventory, 91-180 days since last movement
     */
    VERY_SLOW_91_180("91-180 days", "Very slow-moving"),

    /**
     * Obsolete inventory, 180+ days since last movement
     */
    OBSOLETE_180_PLUS("180+ days", "Obsolete");

    private final String range;
    private final String description;

    AgingBucket(String range, String description) {
        this.range = range;
        this.description = description;
    }

    public String getRange() {
        return range;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get risk level associated with this aging bucket
     */
    public String getRiskLevel() {
        return switch (this) {
            case FRESH_0_30 -> "Low";
            case AGING_31_60 -> "Medium";
            case SLOW_61_90 -> "High";
            case VERY_SLOW_91_180, OBSOLETE_180_PLUS -> "Critical";
        };
    }
}
