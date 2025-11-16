package com.paklog.inventory.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object for catch weight items (variable weight products).
 * Used for items like fresh produce, meat, where each unit has different weight.
 */
public class CatchWeight {

    private final BigDecimal nominalWeight;    // Expected/average weight per unit
    private final BigDecimal actualWeight;     // Actual weighed amount
    private final UnitOfMeasure weightUOM;     // KG, LB, etc.
    private final BigDecimal tolerance;        // Acceptable variance (%)
    private final boolean withinTolerance;

    private CatchWeight(BigDecimal nominalWeight, BigDecimal actualWeight,
                       UnitOfMeasure weightUOM, BigDecimal tolerance) {
        if (nominalWeight == null || nominalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nominal weight must be positive");
        }
        if (actualWeight == null || actualWeight.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Actual weight must be non-negative");
        }
        if (weightUOM == null || weightUOM.getType() != UOMType.WEIGHT) {
            throw new IllegalArgumentException("Weight UOM must be of type WEIGHT");
        }
        if (tolerance == null || tolerance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tolerance must be non-negative");
        }

        this.nominalWeight = nominalWeight;
        this.actualWeight = actualWeight;
        this.weightUOM = weightUOM;
        this.tolerance = tolerance;
        this.withinTolerance = calculateWithinTolerance();
    }

    public CatchWeight(
        BigDecimal nominalWeight,
        BigDecimal actualWeight,
        UnitOfMeasure weightUOM,
        BigDecimal tolerance,
        boolean withinTolerance
    ) {
        this.nominalWeight = nominalWeight;
        this.actualWeight = actualWeight;
        this.weightUOM = weightUOM;
        this.tolerance = tolerance;
        this.withinTolerance = withinTolerance;
    }


    /**
     * Create catch weight with actual measurement
     */
    public static CatchWeight of(BigDecimal nominalWeight, BigDecimal actualWeight,
                                 UnitOfMeasure weightUOM, BigDecimal tolerancePercentage) {
        return new CatchWeight(nominalWeight, actualWeight, weightUOM, tolerancePercentage);
    }

    /**
     * Create catch weight using nominal only (before weighing)
     */
    public static CatchWeight nominal(BigDecimal nominalWeight, UnitOfMeasure weightUOM,
                                     BigDecimal tolerancePercentage) {
        return new CatchWeight(nominalWeight, nominalWeight, weightUOM, tolerancePercentage);
    }

    /**
     * Update with actual weighed amount
     */
    public CatchWeight withActualWeight(BigDecimal actualWeight) {
        return new CatchWeight(this.nominalWeight, actualWeight, this.weightUOM, this.tolerance);
    }

    /**
     * Calculate if actual weight is within tolerance
     */
    private boolean calculateWithinTolerance() {
        BigDecimal variance = actualWeight.subtract(nominalWeight)
            .abs()
            .divide(nominalWeight, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        return variance.compareTo(tolerance) <= 0;
    }

    /**
     * Get variance percentage (positive = over, negative = under)
     */
    public BigDecimal getVariancePercentage() {
        return actualWeight.subtract(nominalWeight)
            .divide(nominalWeight, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get absolute variance amount
     */
    public BigDecimal getVarianceAmount() {
        return actualWeight.subtract(nominalWeight).abs();
    }

    /**
     * Check if overweight (actual > nominal)
     */
    public boolean isOverweight() {
        return actualWeight.compareTo(nominalWeight) > 0;
    }

    /**
     * Check if underweight (actual < nominal)
     */
    public boolean isUnderweight() {
        return actualWeight.compareTo(nominalWeight) < 0;
    }

    public BigDecimal getNominalWeight() {
        return nominalWeight;
    }

    public BigDecimal getActualWeight() {
        return actualWeight;
    }

    public UnitOfMeasure getWeightUOM() {
        return weightUOM;
    }

    public BigDecimal getTolerance() {
        return tolerance;
    }

    public boolean isWithinTolerance() {
        return withinTolerance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CatchWeight that = (CatchWeight) o;
        return Objects.equals(nominalWeight, that.nominalWeight) &&
               Objects.equals(actualWeight, that.actualWeight) &&
               Objects.equals(weightUOM, that.weightUOM);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nominalWeight, actualWeight, weightUOM);
    }

    @Override
    public String toString() {
        return actualWeight + " " + weightUOM + " (nominal: " + nominalWeight + ")";
    }
}
