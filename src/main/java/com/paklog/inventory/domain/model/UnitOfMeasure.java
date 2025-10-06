package com.paklog.inventory.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a unit of measure.
 * Immutable representation of how inventory is measured and tracked.
 */
public class UnitOfMeasure {

    private final String code;           // EA, CASE, PALLET, KG, LB
    private final String description;    // Each, Case, Pallet, Kilogram, Pound
    private final UOMType type;          // DISCRETE, WEIGHT, VOLUME, LENGTH
    private final int decimalPrecision;  // 0 for discrete, 2-3 for weight/volume

    private UnitOfMeasure(String code, String description, UOMType type, int decimalPrecision) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("UOM code cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("UOM type cannot be null");
        }
        if (decimalPrecision < 0 || decimalPrecision > 6) {
            throw new IllegalArgumentException("Decimal precision must be between 0 and 6");
        }

        this.code = code.toUpperCase();
        this.description = description;
        this.type = type;
        this.decimalPrecision = decimalPrecision;
    }

    /**
     * Create a discrete UOM (countable units like EA, CASE, PALLET)
     */
    public static UnitOfMeasure discrete(String code, String description) {
        return new UnitOfMeasure(code, description, UOMType.DISCRETE, 0);
    }

    /**
     * Create a weight-based UOM (KG, LB, etc.)
     */
    public static UnitOfMeasure weight(String code, String description, int decimalPrecision) {
        return new UnitOfMeasure(code, description, UOMType.WEIGHT, decimalPrecision);
    }

    /**
     * Create a volume-based UOM (L, GAL, etc.)
     */
    public static UnitOfMeasure volume(String code, String description, int decimalPrecision) {
        return new UnitOfMeasure(code, description, UOMType.VOLUME, decimalPrecision);
    }

    /**
     * Create a length-based UOM (M, FT, etc.)
     */
    public static UnitOfMeasure length(String code, String description, int decimalPrecision) {
        return new UnitOfMeasure(code, description, UOMType.LENGTH, decimalPrecision);
    }

    /**
     * Check if this UOM is discrete (whole units only)
     */
    public boolean isDiscrete() {
        return type == UOMType.DISCRETE;
    }

    /**
     * Check if this UOM allows fractional quantities
     */
    public boolean allowsFractional() {
        return decimalPrecision > 0;
    }

    /**
     * Validate quantity precision for this UOM
     */
    public void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative");
        }

        if (isDiscrete() && quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                "Discrete UOM " + code + " does not allow fractional quantities: " + quantity);
        }

        if (quantity.scale() > decimalPrecision) {
            throw new IllegalArgumentException(
                "Quantity " + quantity + " exceeds decimal precision " + decimalPrecision + " for UOM " + code);
        }
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public UOMType getType() {
        return type;
    }

    public int getDecimalPrecision() {
        return decimalPrecision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnitOfMeasure that = (UnitOfMeasure) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return code;
    }
}
