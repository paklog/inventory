package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.ValuationMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain event published when inventory valuation changes.
 * Important for financial systems and cost accounting.
 */
public class InventoryValuationChangedEvent extends DomainEvent {

    private final String sku;
    private final ValuationMethod valuationMethod;
    private final BigDecimal previousUnitCost;
    private final BigDecimal newUnitCost;
    private final BigDecimal previousTotalValue;
    private final BigDecimal newTotalValue;
    private final int quantity;
    private final String reason;

    public InventoryValuationChangedEvent(String sku,
                                         ValuationMethod valuationMethod,
                                         BigDecimal previousUnitCost,
                                         BigDecimal newUnitCost,
                                         BigDecimal previousTotalValue,
                                         BigDecimal newTotalValue,
                                         int quantity,
                                         String reason) {
        super(sku);
        this.sku = sku;
        this.valuationMethod = valuationMethod;
        this.previousUnitCost = previousUnitCost;
        this.newUnitCost = newUnitCost;
        this.previousTotalValue = previousTotalValue;
        this.newTotalValue = newTotalValue;
        this.quantity = quantity;
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return CloudEventType.INVENTORY_VALUATION_CHANGED.getType();
    }

    @Override
    public java.util.Map<String, Object> getEventData() {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("sku", sku);
        data.put("valuationMethod", valuationMethod.name());
        data.put("previousUnitCost", previousUnitCost);
        data.put("newUnitCost", newUnitCost);
        data.put("previousTotalValue", previousTotalValue);
        data.put("newTotalValue", newTotalValue);
        data.put("quantity", quantity);
        data.put("reason", reason);
        return java.util.Collections.unmodifiableMap(data);
    }

    public LocalDateTime occurredOn() {
        return getOccurredOn();
    }

    public String getSku() {
        return sku;
    }

    public ValuationMethod getValuationMethod() {
        return valuationMethod;
    }

    public BigDecimal getPreviousUnitCost() {
        return previousUnitCost;
    }

    public BigDecimal getNewUnitCost() {
        return newUnitCost;
    }

    public BigDecimal getPreviousTotalValue() {
        return previousTotalValue;
    }

    public BigDecimal getNewTotalValue() {
        return newTotalValue;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    /**
     * Get the change in total inventory value
     */
    public BigDecimal getValueChange() {
        return newTotalValue.subtract(previousTotalValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryValuationChangedEvent that = (InventoryValuationChangedEvent) o;
        return quantity == that.quantity &&
               Objects.equals(sku, that.sku) &&
               valuationMethod == that.valuationMethod &&
               Objects.equals(occurredOn, that.occurredOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, valuationMethod, quantity, occurredOn);
    }

    @Override
    public String toString() {
        return String.format("InventoryValuationChangedEvent{sku='%s', method=%s, " +
                "unitCost: %s->%s, totalValue: %s->%s, reason='%s'}",
                sku, valuationMethod, previousUnitCost, newUnitCost,
                previousTotalValue, newTotalValue, reason);
    }
}
