package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.ValuationMethod;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ValuationService - critical for financial reporting and COGS calculation.
 * Validates FIFO, LIFO, Weighted Average, and Standard Cost methods.
 */
@ExtendWith(MockitoExtension.class)
class ValuationServiceTest {

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private ValuationService valuationService;

    private String testSku;
    private ProductStock productStock;

    @BeforeEach
    void setUp() {
        testSku = "SKU-TEST-001";
        productStock = ProductStock.create(testSku, 1000);
    }

    @Test
    @DisplayName("Should initialize valuation with Weighted Average method")
    void shouldInitializeValuationWithWeightedAverageMethod() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        BigDecimal initialUnitCost = new BigDecimal("10.50");

        // When
        valuationService.initializeValuation(
            testSku, ValuationMethod.WEIGHTED_AVERAGE, initialUnitCost, "USD"
        );

        // Then
        verify(productStockRepository).save(productStock);

        assertEquals(initialUnitCost, productStock.getUnitCost().orElse(null));
        assertEquals(ValuationMethod.WEIGHTED_AVERAGE,
            productStock.getValuation().get().getValuationMethod());

        // Total value = quantity * unit cost
        BigDecimal expectedTotalValue = initialUnitCost.multiply(new BigDecimal("1000"));
        assertEquals(expectedTotalValue, productStock.getTotalValue().orElse(null));
    }

    @Test
    @DisplayName("Should initialize valuation with FIFO method")
    void shouldInitializeValuationWithFIFOMethod() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        valuationService.initializeValuation(
            testSku, ValuationMethod.FIFO, new BigDecimal("12.00"), "USD"
        );

        // Then
        assertEquals(ValuationMethod.FIFO,
            productStock.getValuation().get().getValuationMethod());
    }

    @Test
    @DisplayName("Should initialize valuation with LIFO method")
    void shouldInitializeValuationWithLIFOMethod() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        valuationService.initializeValuation(
            testSku, ValuationMethod.LIFO, new BigDecimal("11.50"), "USD"
        );

        // Then
        assertEquals(ValuationMethod.LIFO,
            productStock.getValuation().get().getValuationMethod());
    }

    @Test
    @DisplayName("Should initialize valuation with Standard Cost method")
    void shouldInitializeValuationWithStandardCostMethod() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        valuationService.initializeValuation(
            testSku, ValuationMethod.STANDARD_COST, new BigDecimal("15.00"), "USD"
        );

        // Then
        assertEquals(ValuationMethod.STANDARD_COST,
            productStock.getValuation().get().getValuationMethod());
    }

    @Test
    @DisplayName("Should throw exception when initializing valuation for non-existent SKU")
    void shouldThrowExceptionWhenInitializingValuationForNonExistentSku() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            valuationService.initializeValuation(
                testSku, ValuationMethod.WEIGHTED_AVERAGE, BigDecimal.TEN, "USD"
            )
        );

        verify(productStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update valuation on receipt with Weighted Average")
    void shouldUpdateValuationOnReceiptWithWeightedAverage() {
        // Given: Initialize with WAC at $10/unit
        productStock.initializeValuation(ValuationMethod.WEIGHTED_AVERAGE,
            new BigDecimal("10.00"), "USD");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Receive 500 units at $12/unit
        valuationService.updateValuationOnReceipt(testSku, 500, new BigDecimal("12.00"));

        // Then: New weighted average = (1000*10 + 500*12) / 1500 = 16000 / 1500 = 10.67
        verify(productStockRepository).save(productStock);
        verify(outboxRepository).saveAll(anyList());

        BigDecimal expectedUnitCost = new BigDecimal("10.67");
        assertEquals(expectedUnitCost,
            productStock.getUnitCost().orElse(null).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Should update valuation on receipt with FIFO")
    void shouldUpdateValuationOnReceiptWithFIFO() {
        // Given: Initialize with FIFO
        productStock.initializeValuation(ValuationMethod.FIFO,
            new BigDecimal("10.00"), "USD");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Receive 300 units at $11/unit
        valuationService.updateValuationOnReceipt(testSku, 300, new BigDecimal("11.00"));

        // Then: Should maintain separate cost layers
        verify(productStockRepository).save(productStock);
        verify(outboxRepository).saveAll(anyList());

        // FIFO maintains multiple cost layers
        assertTrue(productStock.getValuation().isPresent());
    }

    @Test
    @DisplayName("Should calculate COGS on issue with Weighted Average")
    void shouldCalculateCOGSOnIssueWithWeightedAverage() {
        // Given: Initialize with WAC at $10/unit, 1000 units
        productStock.initializeValuation(ValuationMethod.WEIGHTED_AVERAGE,
            new BigDecimal("10.00"), "USD");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Issue 200 units
        BigDecimal cogs = valuationService.updateValuationOnIssue(testSku, 200);

        // Then: COGS = 200 * $10 = $2000
        assertEquals(new BigDecimal("2000.00"), cogs.setScale(2, java.math.RoundingMode.HALF_UP));
        verify(productStockRepository).save(productStock);
        verify(outboxRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should calculate COGS on issue with FIFO (first layer consumed)")
    void shouldCalculateCOGSOnIssueWithFIFO() {
        // Given: FIFO with two cost layers
        productStock.initializeValuation(ValuationMethod.FIFO,
            new BigDecimal("10.00"), "USD"); // 1000 units @ $10
        productStock.updateValuationOnReceipt(300, new BigDecimal("12.00")); // 300 units @ $12
        productStock.markEventsAsCommitted();

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Issue 1100 units (consumes first layer + part of second)
        BigDecimal cogs = valuationService.updateValuationOnIssue(testSku, 1100);

        // Then: COGS = (1000 * $10) + (100 * $12) = $10,000 + $1,200 = $11,200
        BigDecimal expectedCogs = new BigDecimal("11200.00");
        assertEquals(expectedCogs, cogs.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Should calculate COGS on issue with LIFO (last layer consumed)")
    void shouldCalculateCOGSOnIssueWithLIFO() {
        // Given: LIFO with two cost layers
        productStock.initializeValuation(ValuationMethod.LIFO,
            new BigDecimal("10.00"), "USD"); // 1000 units @ $10
        productStock.updateValuationOnReceipt(500, new BigDecimal("13.00")); // 500 units @ $13
        productStock.markEventsAsCommitted();

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Issue 700 units (consumes last layer + part of first)
        BigDecimal cogs = valuationService.updateValuationOnIssue(testSku, 700);

        // Then: COGS = (500 * $13) + (200 * $10) = $6,500 + $2,000 = $8,500
        BigDecimal expectedCogs = new BigDecimal("8500.00");
        assertEquals(expectedCogs, cogs.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Should maintain Standard Cost regardless of receipts")
    void shouldMaintainStandardCostRegardlessOfReceipts() {
        // Given: Standard cost at $15/unit
        productStock.initializeValuation(ValuationMethod.STANDARD_COST,
            new BigDecimal("15.00"), "USD");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Receive units at different costs
        valuationService.updateValuationOnReceipt(testSku, 200, new BigDecimal("18.00"));
        valuationService.updateValuationOnReceipt(testSku, 300, new BigDecimal("12.00"));

        // Then: Unit cost should remain at standard cost $15
        assertEquals(new BigDecimal("15.00"),
            productStock.getUnitCost().orElse(null).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Should get current unit cost")
    void shouldGetCurrentUnitCost() {
        // Given
        productStock.initializeValuation(ValuationMethod.WEIGHTED_AVERAGE,
            new BigDecimal("10.50"), "USD");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        Optional<BigDecimal> unitCost = valuationService.getUnitCost(testSku);

        // Then
        assertTrue(unitCost.isPresent());
        assertEquals(new BigDecimal("10.50"), unitCost.get());
    }

    @Test
    @DisplayName("Should return empty unit cost when valuation not initialized")
    void shouldReturnEmptyUnitCostWhenValuationNotInitialized() {
        // Given: No valuation initialized
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        Optional<BigDecimal> unitCost = valuationService.getUnitCost(testSku);

        // Then
        assertTrue(unitCost.isEmpty());
    }

    @Test
    @DisplayName("Should get total inventory value")
    void shouldGetTotalInventoryValue() {
        // Given: 1000 units @ $10.50/unit
        productStock.initializeValuation(ValuationMethod.WEIGHTED_AVERAGE,
            new BigDecimal("10.50"), "USD");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        Optional<BigDecimal> totalValue = valuationService.getTotalValue(testSku);

        // Then
        assertTrue(totalValue.isPresent());
        BigDecimal expectedValue = new BigDecimal("10500.00"); // 1000 * 10.50
        assertEquals(expectedValue, totalValue.get().setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Should calculate COGS without updating valuation (what-if analysis)")
    void shouldCalculateCOGSWithoutUpdatingValuation() {
        // Given: Initialize valuation
        productStock.initializeValuation(ValuationMethod.WEIGHTED_AVERAGE,
            new BigDecimal("10.00"), "USD");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        BigDecimal initialTotalValue = productStock.getTotalValue().orElse(null);

        // When: Calculate COGS for 300 units (without updating)
        BigDecimal cogs = valuationService.calculateCOGS(testSku, 300);

        // Then: COGS calculated but valuation unchanged
        assertEquals(new BigDecimal("3000.00"), cogs.setScale(2, java.math.RoundingMode.HALF_UP));
        assertEquals(initialTotalValue, productStock.getTotalValue().orElse(null));

        // Verify no save occurred
        verify(productStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should calculate inventory carrying cost")
    void shouldCalculateInventoryCarryingCost() {
        // Given: 1000 units @ $10/unit = $10,000 total value
        productStock.initializeValuation(ValuationMethod.WEIGHTED_AVERAGE,
            new BigDecimal("10.00"), "USD");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Calculate carrying cost at 25% annual rate
        BigDecimal carryingCost = valuationService.getCarryingCost(testSku, 25.0);

        // Then: Carrying cost = $10,000 * 0.25 = $2,500
        assertEquals(new BigDecimal("2500.00"),
            carryingCost.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Should get valuation method")
    void shouldGetValuationMethod() {
        // Given
        productStock.initializeValuation(ValuationMethod.FIFO,
            new BigDecimal("10.00"), "USD");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        Optional<ValuationMethod> method = valuationService.getValuationMethod(testSku);

        // Then
        assertTrue(method.isPresent());
        assertEquals(ValuationMethod.FIFO, method.get());
    }

    @Test
    @DisplayName("Should handle multiple receipt and issue cycles with Weighted Average")
    void shouldHandleMultipleReceiptAndIssueCyclesWithWeightedAverage() {
        // Given
        productStock.initializeValuation(ValuationMethod.WEIGHTED_AVERAGE,
            new BigDecimal("10.00"), "USD"); // 1000 @ $10

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Complex sequence
        // 1. Receive 500 @ $12
        valuationService.updateValuationOnReceipt(testSku, 500, new BigDecimal("12.00"));
        // WAC = (1000*10 + 500*12) / 1500 = 10.67

        // 2. Issue 800
        BigDecimal cogs1 = valuationService.updateValuationOnIssue(testSku, 800);
        // COGS = 800 * 10.67 = 8533.33
        // Remaining: 700 @ 10.67

        // 3. Receive 600 @ $11
        valuationService.updateValuationOnReceipt(testSku, 600, new BigDecimal("11.00"));
        // WAC = (700*10.67 + 600*11) / 1300 = 10.83

        // 4. Issue 500
        BigDecimal cogs2 = valuationService.updateValuationOnIssue(testSku, 500);
        // COGS = 500 * 10.83 = 5415

        // Then: Verify all operations succeeded
        assertTrue(cogs1.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(cogs2.compareTo(BigDecimal.ZERO) > 0);
        verify(productStockRepository, atLeast(4)).save(productStock);
    }

    @Test
    @DisplayName("Should emit valuation changed events on receipt")
    void shouldEmitValuationChangedEventsOnReceipt() {
        // Given
        productStock.initializeValuation(ValuationMethod.WEIGHTED_AVERAGE,
            new BigDecimal("10.00"), "USD");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        valuationService.updateValuationOnReceipt(testSku, 500, new BigDecimal("12.00"));

        // Then: Events should be saved to outbox
        verify(outboxRepository).saveAll(argThat(events ->
            events.size() > 0 && events.stream()
                .anyMatch(e -> e.getEventType().equals("InventoryValuationChangedEvent"))
        ));
    }

    @Test
    @DisplayName("Should emit valuation changed events on issue")
    void shouldEmitValuationChangedEventsOnIssue() {
        // Given
        productStock.initializeValuation(ValuationMethod.WEIGHTED_AVERAGE,
            new BigDecimal("10.00"), "USD");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        valuationService.updateValuationOnIssue(testSku, 200);

        // Then
        verify(outboxRepository).saveAll(argThat(events ->
            events.size() > 0 && events.stream()
                .anyMatch(e -> e.getEventType().equals("InventoryValuationChangedEvent"))
        ));
    }

    @Test
    @DisplayName("Should throw exception for receipt on non-existent SKU")
    void shouldThrowExceptionForReceiptOnNonExistentSku() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            valuationService.updateValuationOnReceipt(testSku, 100, new BigDecimal("10.00"))
        );
    }

    @Test
    @DisplayName("Should throw exception for issue on non-existent SKU")
    void shouldThrowExceptionForIssueOnNonExistentSku() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            valuationService.updateValuationOnIssue(testSku, 100)
        );
    }

    @Test
    @DisplayName("Should return zero COGS when no valuation initialized")
    void shouldReturnZeroCOGSWhenNoValuationInitialized() {
        // Given: No valuation
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        BigDecimal cogs = valuationService.calculateCOGS(testSku, 100);

        // Then
        assertEquals(BigDecimal.ZERO, cogs);
    }

    @Test
    @DisplayName("Should return zero carrying cost when no valuation initialized")
    void shouldReturnZeroCarryingCostWhenNoValuationInitialized() {
        // Given: No valuation
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        BigDecimal carryingCost = valuationService.getCarryingCost(testSku, 25.0);

        // Then
        assertEquals(BigDecimal.ZERO, carryingCost);
    }

    @Test
    @DisplayName("Should validate different valuation methods produce different COGS")
    void shouldValidateDifferentValuationMethodsProduceDifferentCOGS() {
        // Given: Two stocks with different methods but same receipts
        ProductStock fifoStock = ProductStock.create("SKU-FIFO", 1000);
        fifoStock.initializeValuation(ValuationMethod.FIFO, new BigDecimal("10.00"), "USD");
        fifoStock.updateValuationOnReceipt(500, new BigDecimal("12.00"));

        ProductStock lifoStock = ProductStock.create("SKU-LIFO", 1000);
        lifoStock.initializeValuation(ValuationMethod.LIFO, new BigDecimal("10.00"), "USD");
        lifoStock.updateValuationOnReceipt(500, new BigDecimal("12.00"));

        // When: Issue same quantity from both
        fifoStock.updateValuationOnIssue(400);
        lifoStock.updateValuationOnIssue(400);

        // Then: FIFO and LIFO should produce different costs
        // FIFO: 400 * $10 = $4,000 (from first layer)
        // LIFO: 400 * $12 = $4,800 (from last layer)
        assertNotEquals(fifoStock.getTotalValue(), lifoStock.getTotalValue());
    }
}
