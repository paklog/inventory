package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StockTransfer - validates transfer state transitions and shrinkage calculation.
 */
class StockTransferTest {

    @Test
    @DisplayName("Should create stock transfer")
    void shouldCreateStockTransfer() {
        // Given
        Location source = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);
        Location destination = Location.of("WH01", "B", "05", "03", "C", LocationType.GENERAL);

        // When
        StockTransfer transfer = StockTransfer.create("SKU-001", source, destination,
            100, "USER1", "Replenishment");

        // Then
        assertNotNull(transfer.getTransferId());
        assertEquals("SKU-001", transfer.getSku());
        assertEquals(source, transfer.getSourceLocation());
        assertEquals(destination, transfer.getDestinationLocation());
        assertEquals(100, transfer.getQuantity());
        assertEquals(TransferStatus.INITIATED, transfer.getStatus());
        assertEquals("USER1", transfer.getInitiatedBy());
        assertEquals("Replenishment", transfer.getReason());
    }

    @Test
    @DisplayName("Should mark transfer as in-transit")
    void shouldMarkTransferAsInTransit() {
        // Given
        StockTransfer transfer = createTestTransfer();

        // When
        transfer.markInTransit();

        // Then
        assertEquals(TransferStatus.IN_TRANSIT, transfer.getStatus());
        assertNotNull(transfer.getInTransitAt());
    }

    @Test
    @DisplayName("Should complete transfer with exact quantity")
    void shouldCompleteTransferWithExactQuantity() {
        // Given
        StockTransfer transfer = createTestTransfer();
        transfer.markInTransit();

        // When
        transfer.complete(100);

        // Then
        assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
        assertEquals(100, transfer.getActualQuantityReceived());
        assertFalse(transfer.hasShrinkage());
        assertEquals(0, transfer.getShrinkageQuantity());
        assertEquals(0.0, transfer.getShrinkagePercentage(), 0.01);
    }

    @Test
    @DisplayName("Should complete transfer with shrinkage")
    void shouldCompleteTransferWithShrinkage() {
        // Given
        StockTransfer transfer = createTestTransfer();
        transfer.markInTransit();

        // When: Receive 95 out of 100 units
        transfer.complete(95);

        // Then
        assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
        assertEquals(95, transfer.getActualQuantityReceived());
        assertTrue(transfer.hasShrinkage());
        assertEquals(5, transfer.getShrinkageQuantity());
        assertEquals(5.0, transfer.getShrinkagePercentage(), 0.01);
    }

    @Test
    @DisplayName("Should cancel transfer in INITIATED status")
    void shouldCancelTransferInInitiatedStatus() {
        // Given
        StockTransfer transfer = createTestTransfer();

        // When
        transfer.cancel("MANAGER", "No longer needed");

        // Then
        assertEquals(TransferStatus.CANCELLED, transfer.getStatus());
        assertEquals("MANAGER", transfer.getCancelledBy());
        assertEquals("No longer needed", transfer.getCancellationReason());
        assertNotNull(transfer.getCancelledAt());
    }

    @Test
    @DisplayName("Should cancel transfer in IN_TRANSIT status")
    void shouldCancelTransferInInTransitStatus() {
        // Given
        StockTransfer transfer = createTestTransfer();
        transfer.markInTransit();

        // When
        transfer.cancel("MANAGER", "Emergency stop");

        // Then
        assertEquals(TransferStatus.CANCELLED, transfer.getStatus());
    }

    @Test
    @DisplayName("Should calculate shrinkage percentage correctly")
    void shouldCalculateShrinkagePercentageCorrectly() {
        // Given
        StockTransfer transfer = StockTransfer.create("SKU-001",
            Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL),
            Location.of("WH01", "B", "05", "03", "C", LocationType.GENERAL),
            1000, "USER1", "Large transfer");
        transfer.markInTransit();

        // When: Receive 950 units (50 shrinkage)
        transfer.complete(950);

        // Then
        assertEquals(50, transfer.getShrinkageQuantity());
        assertEquals(5.0, transfer.getShrinkagePercentage(), 0.01);
    }

    @Test
    @DisplayName("Should track transfer timestamps")
    void shouldTrackTransferTimestamps() {
        // Given
        StockTransfer transfer = createTestTransfer();

        // When
        transfer.markInTransit();
        transfer.complete(100);

        // Then
        assertNotNull(transfer.getInitiatedAt());
        assertNotNull(transfer.getInTransitAt());
        assertNotNull(transfer.getCompletedAt());
        assertTrue(transfer.getInTransitAt().isAfter(transfer.getInitiatedAt()) ||
                   transfer.getInTransitAt().isEqual(transfer.getInitiatedAt()));
        assertTrue(transfer.getCompletedAt().isAfter(transfer.getInTransitAt()) ||
                   transfer.getCompletedAt().isEqual(transfer.getInTransitAt()));
    }

    private StockTransfer createTestTransfer() {
        Location source = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);
        Location destination = Location.of("WH01", "B", "05", "03", "C", LocationType.GENERAL);
        return StockTransfer.create("SKU-001", source, destination, 100, "USER1", "Test");
    }
}
