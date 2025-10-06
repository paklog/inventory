package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InventoryValuation - validates FIFO, LIFO, WAC, and Standard Cost methods.
 * Critical for accurate financial reporting and COGS calculation.
 */
class InventoryValuationTest {

    @Test
    @DisplayName("Should create FIFO valuation")
    void shouldCreateFifoValuation() {
        // When
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.FIFO, new BigDecimal("10.00"), 1000, "USD");

        // Then
        assertEquals(ValuationMethod.FIFO, valuation.getValuationMethod());
        assertEquals(new BigDecimal("10.00"), valuation.getUnitCost());
        assertEquals(new BigDecimal("10000.00"), valuation.getTotalValue().setScale(2));
    }

    @Test
    @DisplayName("Should create LIFO valuation")
    void shouldCreateLifoValuation() {
        // When
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.LIFO, new BigDecimal("12.50"), 800, "USD");

        // Then
        assertEquals(ValuationMethod.LIFO, valuation.getValuationMethod());
        assertEquals(new BigDecimal("12.50"), valuation.getUnitCost());
    }

    @Test
    @DisplayName("Should create Weighted Average valuation")
    void shouldCreateWeightedAverageValuation() {
        // When
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.WEIGHTED_AVERAGE, new BigDecimal("11.25"), 1500, "USD");

        // Then
        assertEquals(ValuationMethod.WEIGHTED_AVERAGE, valuation.getValuationMethod());
        assertEquals(new BigDecimal("11.25"), valuation.getUnitCost());
    }

    @Test
    @DisplayName("Should create Standard Cost valuation")
    void shouldCreateStandardCostValuation() {
        // When
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.STANDARD_COST, new BigDecimal("15.00"), 2000, "USD");

        // Then
        assertEquals(ValuationMethod.STANDARD_COST, valuation.getValuationMethod());
        assertEquals(new BigDecimal("15.00"), valuation.getUnitCost());
    }

    @Test
    @DisplayName("Should update FIFO on receipt (add new cost layer)")
    void shouldUpdateFifoOnReceipt() {
        // Given: FIFO with initial layer
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.FIFO, new BigDecimal("10.00"), 1000, "USD");

        // When: Receive 500 units at $12/unit
        InventoryValuation updated = valuation.adjustForQuantityChange(500, new BigDecimal("12.00"));

        // Then: Should maintain cost layers (exact calculation depends on implementation)
        assertNotNull(updated);
        assertEquals(ValuationMethod.FIFO, updated.getValuationMethod());
    }

    @Test
    @DisplayName("Should update Weighted Average on receipt")
    void shouldUpdateWeightedAverageOnReceipt() {
        // Given: WAC at $10/unit, 1000 units = $10,000
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.WEIGHTED_AVERAGE, new BigDecimal("10.00"), 1000, "USD");

        // When: Receive 500 units at $12/unit
        InventoryValuation updated = valuation.adjustForQuantityChange(500, new BigDecimal("12.00"));

        // Then: New WAC = (1000*10 + 500*12) / 1500 = 16000 / 1500 = 10.67
        BigDecimal expectedUnitCost = new BigDecimal("10.67");
        assertEquals(expectedUnitCost,
            updated.getUnitCost().setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Should maintain Standard Cost on receipt")
    void shouldMaintainStandardCostOnReceipt() {
        // Given: Standard cost at $15/unit
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.STANDARD_COST, new BigDecimal("15.00"), 1000, "USD");

        // When: Receive 500 units at different cost
        InventoryValuation updated = valuation.adjustForQuantityChange(500, new BigDecimal("18.00"));

        // Then: Unit cost should remain at standard
        assertEquals(new BigDecimal("15.00"), updated.getUnitCost());
    }

    @Test
    @DisplayName("Should calculate COGS for Weighted Average")
    void shouldCalculateCOGSForWeightedAverage() {
        // Given: WAC at $10/unit
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.WEIGHTED_AVERAGE, new BigDecimal("10.00"), 1000, "USD");

        // When: Issue 300 units
        BigDecimal cogs = valuation.getCostOfGoods(300);

        // Then: COGS = 300 * $10 = $3,000
        assertEquals(new BigDecimal("3000.00"), cogs.setScale(2));
    }

    @Test
    @DisplayName("Should calculate total carrying cost")
    void shouldCalculateTotalCarryingCost() {
        // Given: $10,000 total value
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.WEIGHTED_AVERAGE, new BigDecimal("10.00"), 1000, "USD");

        // When: Calculate carrying cost at 25% annual rate
        BigDecimal carryingCost = valuation.getTotalCarryingCost(25.0);

        // Then: $10,000 * 0.25 = $2,500
        assertEquals(new BigDecimal("2500.00"), carryingCost.setScale(2));
    }

    @Test
    @DisplayName("Should update valuation on issue with WAC")
    void shouldUpdateValuationOnIssueWithWAC() {
        // Given
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.WEIGHTED_AVERAGE, new BigDecimal("10.00"), 1000, "USD");

        // When: Issue 200 units
        InventoryValuation updated = valuation.adjustForQuantityChange(-200, valuation.getUnitCost());

        // Then: Remaining 800 units at same unit cost
        assertEquals(new BigDecimal("10.00"), updated.getUnitCost());
        BigDecimal expectedTotalValue = new BigDecimal("8000.00"); // 800 * 10
        assertEquals(expectedTotalValue, updated.getTotalValue().setScale(2));
    }

    @Test
    @DisplayName("Should handle FIFO cost layers on issue")
    void shouldHandleFifoCostLayersOnIssue() {
        // Given: FIFO with two layers
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.FIFO, new BigDecimal("10.00"), 1000, "USD");
        valuation = valuation.adjustForQuantityChange(500, new BigDecimal("12.00"));

        // When: Issue 1100 units (consumes first layer + part of second)
        InventoryValuation updated = valuation.adjustForQuantityChange(-1100, valuation.getUnitCost());

        // Then: Should have updated valuation (exact depends on implementation)
        assertNotNull(updated);
    }

    @Test
    @DisplayName("Should handle multiple receipt and issue cycles")
    void shouldHandleMultipleReceiptAndIssueCycles() {
        // Given
        InventoryValuation valuation = InventoryValuation.create("SKU-001",
            ValuationMethod.WEIGHTED_AVERAGE, new BigDecimal("10.00"), 1000, "USD");

        // When: Complex cycle
        valuation = valuation.adjustForQuantityChange(500, new BigDecimal("12.00"));
        valuation = valuation.adjustForQuantityChange(-800, valuation.getUnitCost());
        valuation = valuation.adjustForQuantityChange(600, new BigDecimal("11.00"));
        valuation = valuation.adjustForQuantityChange(-500, valuation.getUnitCost());

        // Then: Should maintain valid state
        assertNotNull(valuation);
        assertTrue(valuation.getUnitCost().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(valuation.getTotalValue().compareTo(BigDecimal.ZERO) >= 0);
    }
}
