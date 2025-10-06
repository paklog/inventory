package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.InventoryValuation;
import com.paklog.inventory.domain.model.ValuationMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Embedded document for inventory valuation within ProductStockDocument
 */
public class InventoryValuationDocument {

    private String valuationMethod; // ValuationMethod enum as string
    private BigDecimal unitCost;
    private BigDecimal totalValue;
    private int quantity;
    private LocalDateTime valuationDate;
    private String currency;

    public InventoryValuationDocument() {
    }

    public static InventoryValuationDocument fromDomain(InventoryValuation valuation) {
        InventoryValuationDocument doc = new InventoryValuationDocument();
        doc.valuationMethod = valuation.getValuationMethod().name();
        doc.unitCost = valuation.getUnitCost();
        doc.totalValue = valuation.getTotalValue();
        doc.quantity = valuation.getQuantity();
        doc.valuationDate = valuation.getValuationDate();
        doc.currency = valuation.getCurrency();
        return doc;
    }

    public InventoryValuation toDomain(String sku) {
        return InventoryValuation.load(
            sku,
            ValuationMethod.valueOf(valuationMethod),
            unitCost,
            totalValue,
            quantity,
            valuationDate,
            currency
        );
    }

    // Getters and setters
    public String getValuationMethod() {
        return valuationMethod;
    }

    public void setValuationMethod(String valuationMethod) {
        this.valuationMethod = valuationMethod;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getValuationDate() {
        return valuationDate;
    }

    public void setValuationDate(LocalDateTime valuationDate) {
        this.valuationDate = valuationDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
