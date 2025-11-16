package com.paklog.inventory.domain.model;

import java.math.BigDecimal;
import java.util.*;

/**
 * Value object representing inventory quantity in multiple UOMs simultaneously.
 * Example: 2 PALLET + 3 CASE + 5 EA or 150.5 KG
 */
public class MultiUOMQuantity {

    private final UnitOfMeasure baseUOM;                    // Base tracking unit (e.g., EA)
    private final BigDecimal baseQuantity;                  // Quantity in base UOM
    private final Map<UnitOfMeasure, BigDecimal> quantities; // Quantities in various UOMs
    private final CatchWeight catchWeight;                   // Optional catch weight

    private MultiUOMQuantity(UnitOfMeasure baseUOM, BigDecimal baseQuantity,
                            Map<UnitOfMeasure, BigDecimal> quantities,
                            CatchWeight catchWeight) {
        if (baseUOM == null) {
            throw new IllegalArgumentException("Base UOM cannot be null");
        }
        if (baseQuantity == null || baseQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base quantity must be non-negative");
        }

        baseUOM.validateQuantity(baseQuantity);

        this.baseUOM = baseUOM;
        this.baseQuantity = baseQuantity;
        this.quantities = quantities != null ? new HashMap<>(quantities) : new HashMap<>();
        this.catchWeight = catchWeight;
    }


    /**
     * Create from base UOM quantity only
     */
    public static MultiUOMQuantity fromBase(UnitOfMeasure baseUOM, BigDecimal quantity) {
        return new MultiUOMQuantity(baseUOM, quantity, null, null);
    }

    /**
     * Create with catch weight
     */
    public static MultiUOMQuantity fromBaseWithCatchWeight(UnitOfMeasure baseUOM,
                                                           BigDecimal quantity,
                                                           CatchWeight catchWeight) {
        return new MultiUOMQuantity(baseUOM, quantity, null, catchWeight);
    }

    /**
     * Create with multiple UOM quantities (e.g., 2 PALLET + 3 CASE)
     */
    public static MultiUOMQuantity fromMultiple(UnitOfMeasure baseUOM, BigDecimal baseQuantity,
                                               Map<UnitOfMeasure, BigDecimal> quantities) {
        return new MultiUOMQuantity(baseUOM, baseQuantity, quantities, null);
    }

    /**
     * Add quantity in any UOM (must be convertible to base)
     */
    public MultiUOMQuantity add(BigDecimal quantity, UnitOfMeasure uom, UOMConversion conversion) {
        if (uom.equals(baseUOM)) {
            return new MultiUOMQuantity(baseUOM, baseQuantity.add(quantity), quantities, catchWeight);
        }

        if (!conversion.canConvert(uom, baseUOM)) {
            throw new IllegalArgumentException("Cannot convert " + uom + " to " + baseUOM);
        }

        BigDecimal convertedQty = uom.equals(conversion.getFromUOM()) ?
            conversion.convert(quantity) : conversion.convertReverse(quantity);

        Map<UnitOfMeasure, BigDecimal> newQuantities = new HashMap<>(quantities);
        newQuantities.merge(uom, quantity, BigDecimal::add);

        return new MultiUOMQuantity(baseUOM, baseQuantity.add(convertedQty), newQuantities, catchWeight);
    }

    /**
     * Subtract quantity in any UOM
     */
    public MultiUOMQuantity subtract(BigDecimal quantity, UnitOfMeasure uom, UOMConversion conversion) {
        if (uom.equals(baseUOM)) {
            BigDecimal newQty = baseQuantity.subtract(quantity);
            if (newQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Insufficient quantity in " + baseUOM);
            }
            return new MultiUOMQuantity(baseUOM, newQty, quantities, catchWeight);
        }

        if (!conversion.canConvert(uom, baseUOM)) {
            throw new IllegalArgumentException("Cannot convert " + uom + " to " + baseUOM);
        }

        BigDecimal convertedQty = uom.equals(conversion.getFromUOM()) ?
            conversion.convert(quantity) : conversion.convertReverse(quantity);

        if (baseQuantity.compareTo(convertedQty) < 0) {
            throw new IllegalArgumentException("Insufficient quantity in base UOM");
        }

        Map<UnitOfMeasure, BigDecimal> newQuantities = new HashMap<>(quantities);
        newQuantities.merge(uom, quantity.negate(), BigDecimal::add);
        newQuantities.entrySet().removeIf(e -> e.getValue().compareTo(BigDecimal.ZERO) <= 0);

        return new MultiUOMQuantity(baseUOM, baseQuantity.subtract(convertedQty), newQuantities, catchWeight);
    }

    /**
     * Get quantity in specific UOM
     */
    public Optional<BigDecimal> getQuantityInUOM(UnitOfMeasure uom) {
        return Optional.ofNullable(quantities.get(uom));
    }

    /**
     * Convert entire quantity to different UOM
     */
    public BigDecimal convertTo(UnitOfMeasure targetUOM, UOMConversion conversion) {
        if (targetUOM.equals(baseUOM)) {
            return baseQuantity;
        }

        if (!conversion.canConvert(baseUOM, targetUOM)) {
            throw new IllegalArgumentException("Cannot convert " + baseUOM + " to " + targetUOM);
        }

        return baseUOM.equals(conversion.getFromUOM()) ?
            conversion.convert(baseQuantity) : conversion.convertReverse(baseQuantity);
    }

    /**
     * Check if zero quantity
     */
    public boolean isZero() {
        return baseQuantity.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if has catch weight
     */
    public boolean hasCatchWeight() {
        return catchWeight != null;
    }

    public UnitOfMeasure getBaseUOM() {
        return baseUOM;
    }

    public BigDecimal getBaseQuantity() {
        return baseQuantity;
    }

    public Map<UnitOfMeasure, BigDecimal> getQuantities() {
        return Collections.unmodifiableMap(quantities);
    }

    public Optional<CatchWeight> getCatchWeight() {
        return Optional.ofNullable(catchWeight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiUOMQuantity that = (MultiUOMQuantity) o;
        return Objects.equals(baseUOM, that.baseUOM) &&
               Objects.equals(baseQuantity, that.baseQuantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseUOM, baseQuantity);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(baseQuantity).append(" ").append(baseUOM);

        if (catchWeight != null) {
            sb.append(" (").append(catchWeight).append(")");
        }

        if (quantities.size() > 1) {
            sb.append(" [");
            quantities.entrySet().stream()
                .filter(e -> !e.getKey().equals(baseUOM))
                .forEach(e -> sb.append(e.getValue()).append(" ").append(e.getKey()).append(" "));
            sb.append("]");
        }

        return sb.toString();
    }
}
