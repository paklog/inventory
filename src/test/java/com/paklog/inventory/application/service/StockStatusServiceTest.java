package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.event.StockStatusChangedEvent;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.StockStatus;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for StockStatusService - critical for managing stock availability and quality control.
 * Validates status transitions, quarantine operations, and damage/expiry tracking.
 */
@ExtendWith(MockitoExtension.class)
class StockStatusServiceTest {

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private StockStatusService stockStatusService;

    private String testSku;
    private ProductStock productStock;

    @BeforeEach
    void setUp() {
        testSku = "SKU-TEST-001";
        productStock = ProductStock.create(testSku, 1000);
    }

    @Test
    @DisplayName("Should change stock status from AVAILABLE to QUARANTINE")
    void shouldChangeStockStatusFromAvailableToQuarantine() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        stockStatusService.changeStockStatus(
            testSku, 200, StockStatus.AVAILABLE, StockStatus.QUARANTINE, "Quality inspection required"
        );

        // Then
        verify(productStockRepository).save(productStock);
        verify(outboxRepository).saveAll(anyList());

        // Verify stock quantities
        assertEquals(800, productStock.getQuantityByStatus(StockStatus.AVAILABLE));
        assertEquals(200, productStock.getQuantityByStatus(StockStatus.QUARANTINE));
        assertEquals(1000, productStock.getQuantityOnHand());

        // Available to Promise should be reduced
        assertEquals(800, productStock.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should change stock status from QUARANTINE to AVAILABLE")
    void shouldChangeStockStatusFromQuarantineToAvailable() {
        // Given: Stock with quarantined units
        productStock.changeStockStatus(300, StockStatus.AVAILABLE, StockStatus.QUARANTINE, "Initial QC");
        productStock.markEventsAsCommitted();

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Release from quarantine
        stockStatusService.changeStockStatus(
            testSku, 200, StockStatus.QUARANTINE, StockStatus.AVAILABLE, "QC passed"
        );

        // Then
        assertEquals(900, productStock.getQuantityByStatus(StockStatus.AVAILABLE)); // 700 + 200
        assertEquals(100, productStock.getQuantityByStatus(StockStatus.QUARANTINE)); // 300 - 200
        assertEquals(900, productStock.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should move stock to quarantine using convenience method")
    void shouldMoveStockToQuarantineUsingConvenienceMethod() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        stockStatusService.moveToQuarantine(testSku, 150, "Failed quality check");

        // Then
        verify(productStockRepository).save(productStock);
        assertEquals(850, productStock.getQuantityByStatus(StockStatus.AVAILABLE));
        assertEquals(150, productStock.getQuantityByStatus(StockStatus.QUARANTINE));
    }

    @Test
    @DisplayName("Should release stock from quarantine using convenience method")
    void shouldReleaseStockFromQuarantineUsingConvenienceMethod() {
        // Given: Stock with quarantined units
        productStock.changeStockStatus(200, StockStatus.AVAILABLE, StockStatus.QUARANTINE, "Initial QC");
        productStock.markEventsAsCommitted();

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        stockStatusService.releaseFromQuarantine(testSku, 200, "QC passed");

        // Then
        assertEquals(1000, productStock.getQuantityByStatus(StockStatus.AVAILABLE));
        assertEquals(0, productStock.getQuantityByStatus(StockStatus.QUARANTINE));
    }

    @Test
    @DisplayName("Should mark stock as damaged from AVAILABLE")
    void shouldMarkStockAsDamagedFromAvailable() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        stockStatusService.markAsDamaged(testSku, 100, StockStatus.AVAILABLE, "Dropped pallet");

        // Then
        assertEquals(900, productStock.getQuantityByStatus(StockStatus.AVAILABLE));
        assertEquals(100, productStock.getQuantityByStatus(StockStatus.DAMAGED));
        assertEquals(900, productStock.getAvailableToPromise()); // Damaged units not ATP
    }

    @Test
    @DisplayName("Should mark stock as damaged from QUARANTINE")
    void shouldMarkStockAsDamagedFromQuarantine() {
        // Given: Stock in quarantine
        productStock.changeStockStatus(300, StockStatus.AVAILABLE, StockStatus.QUARANTINE, "QC");
        productStock.markEventsAsCommitted();

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Mark quarantined stock as damaged
        stockStatusService.markAsDamaged(testSku, 50, StockStatus.QUARANTINE, "Failed QC - damaged");

        // Then
        assertEquals(700, productStock.getQuantityByStatus(StockStatus.AVAILABLE));
        assertEquals(250, productStock.getQuantityByStatus(StockStatus.QUARANTINE)); // 300 - 50
        assertEquals(50, productStock.getQuantityByStatus(StockStatus.DAMAGED));
    }

    @Test
    @DisplayName("Should mark stock as expired from AVAILABLE")
    void shouldMarkStockAsExpiredFromAvailable() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        stockStatusService.markAsExpired(testSku, 200, StockStatus.AVAILABLE, "Past expiration date");

        // Then
        assertEquals(800, productStock.getQuantityByStatus(StockStatus.AVAILABLE));
        assertEquals(200, productStock.getQuantityByStatus(StockStatus.EXPIRED));
        assertEquals(800, productStock.getAvailableToPromise()); // Expired units not ATP
    }

    @Test
    @DisplayName("Should mark stock as expired from QUARANTINE")
    void shouldMarkStockAsExpiredFromQuarantine() {
        // Given: Stock in quarantine
        productStock.changeStockStatus(400, StockStatus.AVAILABLE, StockStatus.QUARANTINE, "QC");
        productStock.markEventsAsCommitted();

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Quarantined stock expires
        stockStatusService.markAsExpired(testSku, 100, StockStatus.QUARANTINE, "Expired during QC");

        // Then
        assertEquals(600, productStock.getQuantityByStatus(StockStatus.AVAILABLE));
        assertEquals(300, productStock.getQuantityByStatus(StockStatus.QUARANTINE)); // 400 - 100
        assertEquals(100, productStock.getQuantityByStatus(StockStatus.EXPIRED));
    }

    @Test
    @DisplayName("Should throw exception when changing status for non-existent SKU")
    void shouldThrowExceptionWhenChangingStatusForNonExistentSku() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            stockStatusService.changeStockStatus(
                testSku, 100, StockStatus.AVAILABLE, StockStatus.QUARANTINE, "Test"
            )
        );

        // Verify no save or outbox operations
        verify(productStockRepository, never()).save(any());
        verify(outboxRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should get quantity by status for AVAILABLE")
    void shouldGetQuantityByStatusForAvailable() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        int quantity = stockStatusService.getQuantityByStatus(testSku, StockStatus.AVAILABLE);

        // Then
        assertEquals(1000, quantity);
    }

    @Test
    @DisplayName("Should get quantity by status for QUARANTINE")
    void shouldGetQuantityByStatusForQuarantine() {
        // Given: Stock with quarantined units
        productStock.changeStockStatus(250, StockStatus.AVAILABLE, StockStatus.QUARANTINE, "QC");
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        int quantity = stockStatusService.getQuantityByStatus(testSku, StockStatus.QUARANTINE);

        // Then
        assertEquals(250, quantity);
    }

    @Test
    @DisplayName("Should get zero quantity for status with no stock")
    void shouldGetZeroQuantityForStatusWithNoStock() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        int quantity = stockStatusService.getQuantityByStatus(testSku, StockStatus.DAMAGED);

        // Then
        assertEquals(0, quantity);
    }

    @Test
    @DisplayName("Should receive stock with AVAILABLE status")
    void shouldReceiveStockWithAvailableStatus() {
        // Given: New SKU (will be created)
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.empty());

        // When
        stockStatusService.receiveStockWithStatus(testSku, 500, StockStatus.AVAILABLE, "Initial receipt");

        // Then
        verify(productStockRepository).save(any(ProductStock.class));
        verify(outboxRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should receive stock with DAMAGED status on receipt")
    void shouldReceiveStockWithDamagedStatusOnReceipt() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Receive damaged goods
        stockStatusService.receiveStockWithStatus(testSku, 75, StockStatus.DAMAGED, "Received damaged");

        // Then
        verify(productStockRepository).save(productStock);

        // Verify stock levels
        assertEquals(1000, productStock.getQuantityByStatus(StockStatus.AVAILABLE));
        assertEquals(75, productStock.getQuantityByStatus(StockStatus.DAMAGED));
        assertEquals(1075, productStock.getQuantityOnHand()); // Total increased
        assertEquals(1000, productStock.getAvailableToPromise()); // ATP unchanged
    }

    @Test
    @DisplayName("Should receive stock with QUARANTINE status on receipt")
    void shouldReceiveStockWithQuarantineStatusOnReceipt() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Receive stock directly to quarantine
        stockStatusService.receiveStockWithStatus(testSku, 300, StockStatus.QUARANTINE,
            "Requires QC before release");

        // Then
        assertEquals(1000, productStock.getQuantityByStatus(StockStatus.AVAILABLE));
        assertEquals(300, productStock.getQuantityByStatus(StockStatus.QUARANTINE));
        assertEquals(1300, productStock.getQuantityOnHand());
        assertEquals(1000, productStock.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should handle multiple status changes in sequence")
    void shouldHandleMultipleStatusChangesInSequence() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Complex sequence of status changes
        // 1. Move 200 to quarantine
        stockStatusService.moveToQuarantine(testSku, 200, "Initial QC");

        // 2. Mark 50 as damaged
        stockStatusService.markAsDamaged(testSku, 50, StockStatus.AVAILABLE, "Found damaged");

        // 3. Release 100 from quarantine
        stockStatusService.releaseFromQuarantine(testSku, 100, "QC passed");

        // 4. Mark 25 from quarantine as expired
        stockStatusService.markAsExpired(testSku, 25, StockStatus.QUARANTINE, "Expired during QC");

        // Then: Final state should be correct
        // Started: 1000 AVAILABLE
        // -200 to QUARANTINE = 800 AVAILABLE, 200 QUARANTINE
        // -50 to DAMAGED = 750 AVAILABLE, 200 QUARANTINE, 50 DAMAGED
        // +100 from QUARANTINE = 850 AVAILABLE, 100 QUARANTINE, 50 DAMAGED
        // -25 QUARANTINE to EXPIRED = 850 AVAILABLE, 75 QUARANTINE, 50 DAMAGED, 25 EXPIRED

        assertEquals(850, productStock.getQuantityByStatus(StockStatus.AVAILABLE));
        assertEquals(75, productStock.getQuantityByStatus(StockStatus.QUARANTINE));
        assertEquals(50, productStock.getQuantityByStatus(StockStatus.DAMAGED));
        assertEquals(25, productStock.getQuantityByStatus(StockStatus.EXPIRED));
        assertEquals(1000, productStock.getQuantityOnHand());
        assertEquals(850, productStock.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should emit domain events for status changes")
    void shouldEmitDomainEventsForStatusChanges() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        stockStatusService.changeStockStatus(
            testSku, 150, StockStatus.AVAILABLE, StockStatus.QUARANTINE, "QC check"
        );

        // Then: Events should be saved to outbox
        verify(outboxRepository).saveAll(argThat(events ->
            events.size() > 0 && events.stream()
                .anyMatch(e -> e.getEventType().equals("StockStatusChangedEvent"))
        ));
    }

    @Test
    @DisplayName("Should validate status transition correctness")
    void shouldValidateStatusTransitionCorrectness() {
        // Given: Stock with 100 units in QUARANTINE
        productStock.changeStockStatus(100, StockStatus.AVAILABLE, StockStatus.QUARANTINE, "QC");
        productStock.markEventsAsCommitted();

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Try to release more than quarantined
        // Then: Should throw exception (domain model validation)
        assertThrows(IllegalArgumentException.class, () ->
            stockStatusService.changeStockStatus(
                testSku, 150, StockStatus.QUARANTINE, StockStatus.AVAILABLE, "Invalid release"
            )
        );
    }

    @Test
    @DisplayName("Should handle receiving stock for new SKU")
    void shouldHandleReceivingStockForNewSku() {
        // Given: SKU doesn't exist yet
        String newSku = "SKU-NEW-001";
        when(productStockRepository.findBySku(newSku)).thenReturn(Optional.empty());

        // When: Receive stock
        stockStatusService.receiveStockWithStatus(newSku, 500, StockStatus.AVAILABLE, "First receipt");

        // Then: Should create new ProductStock and save
        verify(productStockRepository).save(argThat(stock ->
            stock.getSku().equals(newSku) &&
            stock.getQuantityOnHand() == 500
        ));
        verify(outboxRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should maintain total quantity on hand across status changes")
    void shouldMaintainTotalQuantityOnHandAcrossStatusChanges() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        int initialOnHand = productStock.getQuantityOnHand();

        // When: Multiple status changes
        stockStatusService.moveToQuarantine(testSku, 100, "QC");
        stockStatusService.markAsDamaged(testSku, 50, StockStatus.AVAILABLE, "Damaged");
        stockStatusService.markAsExpired(testSku, 25, StockStatus.QUARANTINE, "Expired");

        // Then: Total on hand should remain constant
        assertEquals(initialOnHand, productStock.getQuantityOnHand());
    }
}
