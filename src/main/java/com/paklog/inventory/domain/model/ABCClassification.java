package com.paklog.inventory.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Value object representing ABC classification for inventory optimization.
 * A-items: High value/velocity (20% of SKUs, 80% of value)
 * B-items: Medium value/velocity (30% of SKUs, 15% of value)
 * C-items: Low value/velocity (50% of SKUs, 5% of value)
 *
 * Industry pattern: SAP IM ABC Analysis, Oracle ABC Classification
 */
public class ABCClassification {

    private final String sku;
    private final ABCClass abcClass;
    private final ABCCriteria classificationCriteria;
    private final LocalDate classifiedOn;
    private final LocalDate validUntil;

    // Metrics used for classification
    private final BigDecimal annualUsageValue; // Quantity * Unit Cost
    private final int annualUsageQuantity;
    private final BigDecimal unitCost;
    private final double velocityScore; // Movement frequency
    private final double criticalityScore; // Business importance (0-100)

    private ABCClassification(String sku, ABCClass abcClass, ABCCriteria classificationCriteria,
                             LocalDate classifiedOn, LocalDate validUntil,
                             BigDecimal annualUsageValue, int annualUsageQuantity,
                             BigDecimal unitCost, double velocityScore, double criticalityScore) {
        this.sku = sku;
        this.abcClass = abcClass;
        this.classificationCriteria = classificationCriteria;
        this.classifiedOn = classifiedOn;
        this.validUntil = validUntil;
        this.annualUsageValue = annualUsageValue;
        this.annualUsageQuantity = annualUsageQuantity;
        this.unitCost = unitCost;
        this.velocityScore = velocityScore;
        this.criticalityScore = criticalityScore;
        validateInvariants();
    }

    public static ABCClassification classify(String sku, BigDecimal annualUsageValue,
                                            int annualUsageQuantity, BigDecimal unitCost,
                                            double velocityScore, double criticalityScore,
                                            ABCCriteria criteria) {
        ABCClass abcClass = determineClass(annualUsageValue, velocityScore, criticalityScore, criteria);
        LocalDate classifiedOn = LocalDate.now();
        LocalDate validUntil = classifiedOn.plusMonths(criteria.getValidityMonths());

        return new ABCClassification(sku, abcClass, criteria, classifiedOn, validUntil,
                                    annualUsageValue, annualUsageQuantity, unitCost,
                                    velocityScore, criticalityScore);
    }

    public static ABCClassification load(String sku, ABCClass abcClass, ABCCriteria criteria,
                                        LocalDate classifiedOn, LocalDate validUntil,
                                        BigDecimal annualUsageValue, int annualUsageQuantity,
                                        BigDecimal unitCost, double velocityScore, double criticalityScore) {
        return new ABCClassification(sku, abcClass, criteria, classifiedOn, validUntil,
                                    annualUsageValue, annualUsageQuantity, unitCost,
                                    velocityScore, criticalityScore);
    }

    private static ABCClass determineClass(BigDecimal annualUsageValue, double velocityScore,
                                          double criticalityScore, ABCCriteria criteria) {
        switch (criteria) {
            case VALUE_BASED:
                // Pure value-based classification (Pareto principle)
                // In practice, this requires cumulative percentage calculation across all SKUs
                // Simplified here: high value = A, medium = B, low = C
                if (annualUsageValue.compareTo(BigDecimal.valueOf(100000)) > 0) {
                    return ABCClass.A;
                } else if (annualUsageValue.compareTo(BigDecimal.valueOf(10000)) > 0) {
                    return ABCClass.B;
                } else {
                    return ABCClass.C;
                }

            case VELOCITY_BASED:
                // Movement frequency based
                if (velocityScore >= 80.0) {
                    return ABCClass.A;
                } else if (velocityScore >= 50.0) {
                    return ABCClass.B;
                } else {
                    return ABCClass.C;
                }

            case CRITICALITY_BASED:
                // Business criticality based
                if (criticalityScore >= 80.0) {
                    return ABCClass.A;
                } else if (criticalityScore >= 50.0) {
                    return ABCClass.B;
                } else {
                    return ABCClass.C;
                }

            case COMBINED:
                // Weighted combination: 50% value, 30% velocity, 20% criticality
                double valueScore = Math.min(100, annualUsageValue.doubleValue() / 1000);
                double combinedScore = (valueScore * 0.5) + (velocityScore * 0.3) + (criticalityScore * 0.2);
                if (combinedScore >= 70.0) {
                    return ABCClass.A;
                } else if (combinedScore >= 40.0) {
                    return ABCClass.B;
                } else {
                    return ABCClass.C;
                }

            default:
                throw new IllegalArgumentException("Unknown classification criteria: " + criteria);
        }
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(validUntil);
    }

    public boolean requiresReclassification() {
        return isExpired() || LocalDate.now().isAfter(validUntil.minusMonths(1));
    }

    /**
     * Get recommended cycle count frequency in days
     */
    public int getRecommendedCountFrequency() {
        return switch (abcClass) {
            case A -> 30;  // Monthly
            case B -> 90;  // Quarterly
            case C -> 180; // Semi-annually
        };
    }

    /**
     * Alias for getRecommendedCountFrequency
     */
    public int getRecommendedCycleCountDays() {
        return getRecommendedCountFrequency();
    }

    /**
     * Get recommended service level target
     */
    public double getRecommendedServiceLevel() {
        return switch (abcClass) {
            case A -> 99.0;  // 99% service level for A items
            case B -> 95.0;  // 95% service level for B items
            case C -> 90.0;  // 90% service level for C items
        };
    }

    private void validateInvariants() {
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU cannot be null or empty");
        }
        if (abcClass == null) {
            throw new IllegalArgumentException("ABC class cannot be null");
        }
        if (classificationCriteria == null) {
            throw new IllegalArgumentException("Classification criteria cannot be null");
        }
        if (classifiedOn == null) {
            throw new IllegalArgumentException("ClassifiedOn date cannot be null");
        }
        if (validUntil == null) {
            throw new IllegalArgumentException("ValidUntil date cannot be null");
        }
        if (validUntil.isBefore(classifiedOn)) {
            throw new IllegalArgumentException("ValidUntil must be after ClassifiedOn");
        }
        if (annualUsageValue == null || annualUsageValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Annual usage value must be non-negative");
        }
        if (annualUsageQuantity < 0) {
            throw new IllegalArgumentException("Annual usage quantity must be non-negative");
        }
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit cost must be non-negative");
        }
        if (velocityScore < 0 || velocityScore > 100) {
            throw new IllegalArgumentException("Velocity score must be between 0 and 100");
        }
        if (criticalityScore < 0 || criticalityScore > 100) {
            throw new IllegalArgumentException("Criticality score must be between 0 and 100");
        }
    }

    // Getters
    public String getSku() {
        return sku;
    }

    public ABCClass getAbcClass() {
        return abcClass;
    }

    public ABCCriteria getClassificationCriteria() {
        return classificationCriteria;
    }

    // Alias for getClassificationCriteria
    public ABCCriteria getCriteria() {
        return classificationCriteria;
    }

    // Alias for getAbcClass to return as string if needed
    public String classification() {
        return abcClass.name();
    }

    public LocalDate getClassifiedOn() {
        return classifiedOn;
    }

    /**
     * Alias for getClassifiedOn
     */
    public LocalDate getClassifiedAt() {
        return classifiedOn;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public BigDecimal getAnnualUsageValue() {
        return annualUsageValue;
    }

    public int getAnnualUsageQuantity() {
        return annualUsageQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public double getVelocityScore() {
        return velocityScore;
    }

    public double getCriticalityScore() {
        return criticalityScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ABCClassification that = (ABCClassification) o;
        return Objects.equals(sku, that.sku) &&
               Objects.equals(classifiedOn, that.classifiedOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, classifiedOn);
    }

    @Override
    public String toString() {
        return "ABCClassification{" +
                "sku='" + sku + '\'' +
                ", abcClass=" + abcClass +
                ", criteria=" + classificationCriteria +
                ", annualUsageValue=" + annualUsageValue +
                ", velocityScore=" + velocityScore +
                ", validUntil=" + validUntil +
                ", expired=" + isExpired() +
                ", recommendedCountFrequency=" + getRecommendedCountFrequency() + " days" +
                '}';
    }
}
