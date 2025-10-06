package com.paklog.inventory.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Value object for tracking inventory aging and identifying slow-moving stock.
 * Used for obsolescence management and working capital optimization.
 *
 * Part of the Inventory bounded context domain model.
 */
public class InventoryAging {

    private final String sku;
    private final int quantityOnHand;
    private final LocalDate firstReceivedDate;
    private final LocalDate lastMovementDate; // Last pick/issue
    private final LocalDate lastReceiptDate;
    private final int daysOnHand;
    private final int daysSinceLastMovement;
    private final AgingBucket agingBucket;
    private final double turnoverRate; // Annual turnover

    private InventoryAging(String sku, int quantityOnHand,
                          LocalDate firstReceivedDate, LocalDate lastMovementDate,
                          LocalDate lastReceiptDate, int daysOnHand,
                          int daysSinceLastMovement, AgingBucket agingBucket,
                          double turnoverRate) {
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.firstReceivedDate = firstReceivedDate;
        this.lastMovementDate = lastMovementDate;
        this.lastReceiptDate = lastReceiptDate;
        this.daysOnHand = daysOnHand;
        this.daysSinceLastMovement = daysSinceLastMovement;
        this.agingBucket = agingBucket;
        this.turnoverRate = turnoverRate;
        validateInvariants();
    }

    /**
     * Factory method to calculate aging
     */
    public static InventoryAging calculate(String sku, int quantityOnHand,
                                          LocalDate firstReceivedDate,
                                          LocalDate lastMovementDate,
                                          LocalDate lastReceiptDate,
                                          int annualDemandQuantity) {
        LocalDate now = LocalDate.now();

        int daysOnHand = firstReceivedDate != null ?
            (int) ChronoUnit.DAYS.between(firstReceivedDate, now) : 0;

        int daysSinceLastMovement = lastMovementDate != null ?
            (int) ChronoUnit.DAYS.between(lastMovementDate, now) : daysOnHand;

        AgingBucket bucket = determineAgingBucket(daysSinceLastMovement);

        // Turnover rate = annual demand / average inventory
        double turnoverRate = quantityOnHand > 0 ?
            (double) annualDemandQuantity / quantityOnHand : 0.0;

        return new InventoryAging(sku, quantityOnHand, firstReceivedDate,
            lastMovementDate, lastReceiptDate, daysOnHand, daysSinceLastMovement,
            bucket, turnoverRate);
    }

    private static AgingBucket determineAgingBucket(int daysSinceLastMovement) {
        if (daysSinceLastMovement <= 30) {
            return AgingBucket.FRESH_0_30;
        } else if (daysSinceLastMovement <= 60) {
            return AgingBucket.AGING_31_60;
        } else if (daysSinceLastMovement <= 90) {
            return AgingBucket.SLOW_61_90;
        } else if (daysSinceLastMovement <= 180) {
            return AgingBucket.VERY_SLOW_91_180;
        } else {
            return AgingBucket.OBSOLETE_180_PLUS;
        }
    }

    /**
     * Check if inventory is considered obsolete
     */
    public boolean isObsolete() {
        return agingBucket == AgingBucket.OBSOLETE_180_PLUS;
    }

    /**
     * Check if inventory is slow-moving
     */
    public boolean isSlowMoving() {
        return agingBucket.ordinal() >= AgingBucket.SLOW_61_90.ordinal();
    }

    /**
     * Check if inventory has high turnover (fast-moving)
     */
    public boolean isFastMoving() {
        return turnoverRate >= 12.0; // Turns over monthly or faster
    }

    /**
     * Get recommended action based on aging
     */
    public String getRecommendedAction() {
        return switch (agingBucket) {
            case FRESH_0_30 -> "No action required";
            case AGING_31_60 -> "Monitor closely";
            case SLOW_61_90 -> "Consider promotion or transfer";
            case VERY_SLOW_91_180 -> "Initiate clearance or discount";
            case OBSOLETE_180_PLUS -> "Mark for disposal or liquidation";
        };
    }

    /**
     * Calculate days of supply (inventory / daily demand)
     */
    public int getDaysOfSupply(int annualDemandQuantity) {
        if (annualDemandQuantity <= 0) {
            return Integer.MAX_VALUE; // Infinite supply if no demand
        }
        double dailyDemand = annualDemandQuantity / 365.0;
        return (int) Math.ceil(quantityOnHand / dailyDemand);
    }

    /**
     * Get inventory health score (0-100, higher is better)
     */
    public int getHealthScore() {
        int score = 100;

        // Penalize based on aging bucket
        score -= agingBucket.ordinal() * 20;

        // Bonus for good turnover
        if (turnoverRate >= 12) {
            score += 10;
        } else if (turnoverRate >= 6) {
            score += 5;
        }

        return Math.max(0, Math.min(100, score));
    }

    private void validateInvariants() {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be blank");
        }
        if (quantityOnHand < 0) {
            throw new IllegalArgumentException("Quantity on hand cannot be negative");
        }
        if (daysOnHand < 0) {
            throw new IllegalArgumentException("Days on hand cannot be negative");
        }
        if (daysSinceLastMovement < 0) {
            throw new IllegalArgumentException("Days since last movement cannot be negative");
        }
        if (agingBucket == null) {
            throw new IllegalArgumentException("Aging bucket cannot be null");
        }
        if (turnoverRate < 0) {
            throw new IllegalArgumentException("Turnover rate cannot be negative");
        }
    }

    // Getters
    public String getSku() {
        return sku;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public LocalDate getFirstReceivedDate() {
        return firstReceivedDate;
    }

    public LocalDate getLastMovementDate() {
        return lastMovementDate;
    }

    public LocalDate getLastReceiptDate() {
        return lastReceiptDate;
    }

    public int getDaysOnHand() {
        return daysOnHand;
    }

    public int getDaysSinceLastMovement() {
        return daysSinceLastMovement;
    }

    public AgingBucket getAgingBucket() {
        return agingBucket;
    }

    public double getTurnoverRate() {
        return turnoverRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryAging that = (InventoryAging) o;
        return Objects.equals(sku, that.sku) &&
               Objects.equals(firstReceivedDate, that.firstReceivedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, firstReceivedDate);
    }

    @Override
    public String toString() {
        return String.format("InventoryAging{sku='%s', qty=%d, bucket=%s, " +
                "daysOnHand=%d, daysSinceMovement=%d, turnover=%.2f, health=%d%%}",
                sku, quantityOnHand, agingBucket, daysOnHand,
                daysSinceLastMovement, turnoverRate, getHealthScore());
    }
}
