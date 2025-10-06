package com.paklog.inventory.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object representing inventory cost and valuation.
 * Supports multiple valuation methods (FIFO, LIFO, Weighted Average).
 *
 * Industry pattern: SAP IM Material Valuation, Oracle Inventory Valuation
 */
public class InventoryValuation {

    private final String sku;
    private final ValuationMethod valuationMethod;
    private final BigDecimal unitCost;
    private final BigDecimal totalValue;
    private final int quantity;
    private final LocalDateTime valuationDate;
    private final String currency;

    private InventoryValuation(String sku, ValuationMethod valuationMethod,
                              BigDecimal unitCost, BigDecimal totalValue,
                              int quantity, LocalDateTime valuationDate, String currency) {
        this.sku = sku;
        this.valuationMethod = valuationMethod;
        this.unitCost = unitCost;
        this.totalValue = totalValue;
        this.quantity = quantity;
        this.valuationDate = valuationDate;
        this.currency = currency;
        validateInvariants();
    }

    public static InventoryValuation create(String sku, ValuationMethod valuationMethod,
                                           BigDecimal unitCost, int quantity, String currency) {
        BigDecimal totalValue = unitCost.multiply(BigDecimal.valueOf(quantity))
                                       .setScale(2, RoundingMode.HALF_UP);
        return new InventoryValuation(sku, valuationMethod, unitCost, totalValue,
                                     quantity, LocalDateTime.now(), currency);
    }

    public static InventoryValuation load(String sku, ValuationMethod valuationMethod,
                                         BigDecimal unitCost, BigDecimal totalValue,
                                         int quantity, LocalDateTime valuationDate, String currency) {
        return new InventoryValuation(sku, valuationMethod, unitCost, totalValue,
                                     quantity, valuationDate, currency);
    }

    /**
     * Adjust valuation for quantity change
     */
    public InventoryValuation adjustForQuantityChange(int quantityChange, BigDecimal newUnitCost) {
        int newQuantity = this.quantity + quantityChange;
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Adjusted quantity cannot be negative");
        }

        BigDecimal adjustedUnitCost;
        switch (valuationMethod) {
            case WEIGHTED_AVERAGE:
                // Recalculate weighted average
                if (quantityChange > 0) {
                    // Receiving stock
                    BigDecimal currentValue = this.totalValue;
                    BigDecimal addedValue = newUnitCost.multiply(BigDecimal.valueOf(quantityChange));
                    BigDecimal newTotalValue = currentValue.add(addedValue);
                    adjustedUnitCost = newQuantity > 0 ?
                        newTotalValue.divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP) :
                        BigDecimal.ZERO;
                } else {
                    // Consuming stock - keep current weighted average
                    adjustedUnitCost = this.unitCost;
                }
                break;

            case FIFO:
            case LIFO:
                // For FIFO/LIFO, unit cost changes based on consumption order
                // In practice, requires tracking cost layers (simplified here)
                adjustedUnitCost = quantityChange > 0 ? newUnitCost : this.unitCost;
                break;

            case STANDARD_COST:
                // Standard cost doesn't change with movements
                adjustedUnitCost = this.unitCost;
                break;

            default:
                throw new IllegalStateException("Unknown valuation method: " + valuationMethod);
        }

        BigDecimal newTotalValue = adjustedUnitCost.multiply(BigDecimal.valueOf(newQuantity))
                                                   .setScale(2, RoundingMode.HALF_UP);

        return new InventoryValuation(sku, valuationMethod, adjustedUnitCost, newTotalValue,
                                     newQuantity, LocalDateTime.now(), currency);
    }

    /**
     * Get cost of goods for specific quantity (COGS calculation)
     */
    public BigDecimal getCostOfGoods(int quantityIssued) {
        if (quantityIssued < 0 || quantityIssued > quantity) {
            throw new IllegalArgumentException("Invalid quantity issued: " + quantityIssued);
        }
        return unitCost.multiply(BigDecimal.valueOf(quantityIssued))
                      .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get inventory carrying cost per unit (for financial analysis)
     */
    public BigDecimal getCarryingCostPerUnit(double annualCarryingCostPercentage) {
        return unitCost.multiply(BigDecimal.valueOf(annualCarryingCostPercentage / 100))
                      .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get total inventory carrying cost
     */
    public BigDecimal getTotalCarryingCost(double annualCarryingCostPercentage) {
        return totalValue.multiply(BigDecimal.valueOf(annualCarryingCostPercentage / 100))
                        .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateInvariants() {
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU cannot be null or empty");
        }
        if (valuationMethod == null) {
            throw new IllegalArgumentException("Valuation method cannot be null");
        }
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit cost must be non-negative");
        }
        if (totalValue == null || totalValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total value must be non-negative");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative");
        }
        if (valuationDate == null) {
            throw new IllegalArgumentException("Valuation date cannot be null");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        // Validate total value consistency
        BigDecimal expectedTotal = unitCost.multiply(BigDecimal.valueOf(quantity))
                                          .setScale(2, RoundingMode.HALF_UP);
        if (totalValue.compareTo(expectedTotal) != 0) {
            throw new IllegalArgumentException(
                String.format("Total value mismatch. Expected: %s, Actual: %s",
                             expectedTotal, totalValue));
        }
    }

    // Getters
    public String getSku() {
        return sku;
    }

    public ValuationMethod getValuationMethod() {
        return valuationMethod;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    // Alias for getUnitCost
    public BigDecimal getCostPerUnit() {
        return unitCost;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getValuationDate() {
        return valuationDate;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryValuation that = (InventoryValuation) o;
        return quantity == that.quantity &&
               Objects.equals(sku, that.sku) &&
               valuationMethod == that.valuationMethod &&
               Objects.equals(unitCost, that.unitCost) &&
               Objects.equals(totalValue, that.totalValue) &&
               Objects.equals(valuationDate, that.valuationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, valuationMethod, unitCost, totalValue, quantity, valuationDate);
    }

    @Override
    public String toString() {
        return "InventoryValuation{" +
                "sku='" + sku + '\'' +
                ", method=" + valuationMethod +
                ", unitCost=" + unitCost +
                ", quantity=" + quantity +
                ", totalValue=" + totalValue +
                ", currency='" + currency + '\'' +
                ", valuationDate=" + valuationDate +
                '}';
    }
}
