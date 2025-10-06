package com.paklog.inventory.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing a conversion between two units of measure.
 * Example: 1 CASE = 12 EA, 1 PALLET = 48 CASE
 */
public class UOMConversion {

    private final UnitOfMeasure fromUOM;
    private final UnitOfMeasure toUOM;
    private final BigDecimal conversionFactor;  // How many toUOM in 1 fromUOM
    private final boolean isReversible;          // Can convert both ways

    private UOMConversion(UnitOfMeasure fromUOM, UnitOfMeasure toUOM,
                         BigDecimal conversionFactor, boolean isReversible) {
        if (fromUOM == null || toUOM == null) {
            throw new IllegalArgumentException("UOMs cannot be null");
        }
        if (fromUOM.equals(toUOM)) {
            throw new IllegalArgumentException("Cannot convert UOM to itself");
        }
        if (conversionFactor == null || conversionFactor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Conversion factor must be positive");
        }

        this.fromUOM = fromUOM;
        this.toUOM = toUOM;
        this.conversionFactor = conversionFactor;
        this.isReversible = isReversible;
    }

    /**
     * Create a one-way conversion (e.g., PALLET → EA only)
     */
    public static UOMConversion oneWay(UnitOfMeasure fromUOM, UnitOfMeasure toUOM,
                                       BigDecimal conversionFactor) {
        return new UOMConversion(fromUOM, toUOM, conversionFactor, false);
    }

    /**
     * Create a two-way conversion (e.g., CASE ↔ EA)
     */
    public static UOMConversion twoWay(UnitOfMeasure fromUOM, UnitOfMeasure toUOM,
                                       BigDecimal conversionFactor) {
        return new UOMConversion(fromUOM, toUOM, conversionFactor, true);
    }

    /**
     * Convert quantity from fromUOM to toUOM
     */
    public BigDecimal convert(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative");
        }

        fromUOM.validateQuantity(quantity);

        BigDecimal result = quantity.multiply(conversionFactor);

        // Round to target UOM's precision
        return result.setScale(toUOM.getDecimalPrecision(), RoundingMode.HALF_UP);
    }

    /**
     * Convert quantity in reverse direction (only if reversible)
     */
    public BigDecimal convertReverse(BigDecimal quantity) {
        if (!isReversible) {
            throw new IllegalStateException(
                "Cannot reverse convert from " + toUOM + " to " + fromUOM + " - conversion is one-way");
        }

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative");
        }

        toUOM.validateQuantity(quantity);

        BigDecimal result = quantity.divide(conversionFactor, 10, RoundingMode.HALF_UP);

        // Round to target UOM's precision
        return result.setScale(fromUOM.getDecimalPrecision(), RoundingMode.HALF_UP);
    }

    /**
     * Check if this conversion can convert between the given UOMs
     */
    public boolean canConvert(UnitOfMeasure from, UnitOfMeasure to) {
        boolean forward = fromUOM.equals(from) && toUOM.equals(to);
        boolean reverse = isReversible && fromUOM.equals(to) && toUOM.equals(from);
        return forward || reverse;
    }

    /**
     * Get reverse conversion (only if reversible)
     */
    public UOMConversion reverse() {
        if (!isReversible) {
            throw new IllegalStateException("Cannot reverse non-reversible conversion");
        }
        return new UOMConversion(toUOM, fromUOM,
            BigDecimal.ONE.divide(conversionFactor, 10, RoundingMode.HALF_UP), true);
    }

    public UnitOfMeasure getFromUOM() {
        return fromUOM;
    }

    public UnitOfMeasure getToUOM() {
        return toUOM;
    }

    public BigDecimal getConversionFactor() {
        return conversionFactor;
    }

    public boolean isReversible() {
        return isReversible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UOMConversion that = (UOMConversion) o;
        return Objects.equals(fromUOM, that.fromUOM) &&
               Objects.equals(toUOM, that.toUOM);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromUOM, toUOM);
    }

    @Override
    public String toString() {
        return "1 " + fromUOM + " = " + conversionFactor + " " + toUOM;
    }
}
