package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.LocationType;
import com.paklog.inventory.domain.model.StockTransfer;
import com.paklog.inventory.domain.model.TransferStatus;
import com.paklog.inventory.domain.repository.StockTransferRepository;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for StockTransferService - validates inter-location transfer operations.
 * Tests transfer lifecycle, shrinkage tracking, and status transitions.
 */
@ExtendWith(MockitoExtension.class)
class StockTransferServiceTest {

    @Mock
    private StockTransferRepository stockTransferRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private StockTransferService stockTransferService;

    private String testSku;
    private Location sourceLocation;
    private Location destinationLocation;
    private String initiatedBy;

    @BeforeEach
    void setUp() {
        testSku = "SKU-TEST-001";
        sourceLocation = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);
        destinationLocation = Location.of("WH01", "B", "05", "03", "C", LocationType.GENERAL);
        initiatedBy = "USER-001";
    }

    @Test
    @DisplayName("Should initiate transfer successfully")
    void shouldInitiateTransferSuccessfully() {
        // Given
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Replenishment");

        when(stockTransferRepository.save(any(StockTransfer.class))).thenReturn(transfer);

        // When
        String transferId = stockTransferService.initiateTransfer(
            testSku, sourceLocation, destinationLocation, 100, initiatedBy, "Replenishment"
        );

        // Then
        assertNotNull(transferId);
        verify(stockTransferRepository).save(any(StockTransfer.class));
        verify(outboxRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should mark transfer as in-transit")
    void shouldMarkTransferAsInTransit() {
        // Given
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Replenishment");

        when(stockTransferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        // When
        stockTransferService.markInTransit(transfer.getTransferId());

        // Then
        assertEquals(TransferStatus.IN_TRANSIT, transfer.getStatus());
        assertNotNull(transfer.getInTransitAt());
        verify(stockTransferRepository).save(transfer);
        verify(outboxRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should complete transfer with exact quantity received")
    void shouldCompleteTransferWithExactQuantityReceived() {
        // Given
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Replenishment");
        transfer.markInTransit();

        when(stockTransferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        // When
        stockTransferService.completeTransfer(transfer.getTransferId(), 100);

        // Then
        assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
        assertEquals(100, transfer.getActualQuantityReceived());
        assertFalse(transfer.hasShrinkage());
        assertEquals(0, transfer.getShrinkageQuantity());
        verify(stockTransferRepository).save(transfer);
    }

    @Test
    @DisplayName("Should complete transfer with shrinkage")
    void shouldCompleteTransferWithShrinkage() {
        // Given
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Replenishment");
        transfer.markInTransit();

        when(stockTransferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        // When: Receive only 95 units (5 units shrinkage)
        stockTransferService.completeTransfer(transfer.getTransferId(), 95);

        // Then
        assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
        assertEquals(95, transfer.getActualQuantityReceived());
        assertTrue(transfer.hasShrinkage());
        assertEquals(5, transfer.getShrinkageQuantity());
    }

    @Test
    @DisplayName("Should cancel transfer in INITIATED status")
    void shouldCancelTransferInInitiatedStatus() {
        // Given
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Replenishment");

        when(stockTransferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        // When
        stockTransferService.cancelTransfer(transfer.getTransferId(), "MANAGER-001", "No longer needed");

        // Then
        assertEquals(TransferStatus.CANCELLED, transfer.getStatus());
        assertEquals("MANAGER-001", transfer.getCancelledBy());
        assertEquals("No longer needed", transfer.getCancellationReason());
        verify(stockTransferRepository).save(transfer);
        verify(outboxRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should cancel transfer in IN_TRANSIT status")
    void shouldCancelTransferInInTransitStatus() {
        // Given
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Replenishment");
        transfer.markInTransit();

        when(stockTransferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        // When
        stockTransferService.cancelTransfer(transfer.getTransferId(), "MANAGER-001", "Emergency stop");

        // Then
        assertEquals(TransferStatus.CANCELLED, transfer.getStatus());
        assertNotNull(transfer.getCancelledAt());
    }

    @Test
    @DisplayName("Should throw exception when marking non-existent transfer in-transit")
    void shouldThrowExceptionWhenMarkingNonExistentTransferInTransit() {
        // Given
        when(stockTransferRepository.findById("INVALID-ID")).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            stockTransferService.markInTransit("INVALID-ID")
        );

        verify(stockTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when completing non-existent transfer")
    void shouldThrowExceptionWhenCompletingNonExistentTransfer() {
        // Given
        when(stockTransferRepository.findById("INVALID-ID")).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            stockTransferService.completeTransfer("INVALID-ID", 100)
        );

        verify(stockTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when cancelling non-existent transfer")
    void shouldThrowExceptionWhenCancellingNonExistentTransfer() {
        // Given
        when(stockTransferRepository.findById("INVALID-ID")).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            stockTransferService.cancelTransfer("INVALID-ID", "USER", "Reason")
        );

        verify(stockTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get transfer details")
    void shouldGetTransferDetails() {
        // Given
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Replenishment");

        when(stockTransferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        // When
        StockTransfer result = stockTransferService.getTransfer(transfer.getTransferId());

        // Then
        assertNotNull(result);
        assertEquals(testSku, result.getSku());
        assertEquals(100, result.getQuantity());
        assertEquals(sourceLocation, result.getSourceLocation());
        assertEquals(destinationLocation, result.getDestinationLocation());
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent transfer")
    void shouldThrowExceptionWhenGettingNonExistentTransfer() {
        // Given
        when(stockTransferRepository.findById("INVALID-ID")).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            stockTransferService.getTransfer("INVALID-ID")
        );
    }

    @Test
    @DisplayName("Should get all transfers for a SKU")
    void shouldGetAllTransfersForSku() {
        // Given
        StockTransfer transfer1 = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Replenishment");
        StockTransfer transfer2 = StockTransfer.create(testSku, destinationLocation, sourceLocation,
            50, initiatedBy, "Return");

        when(stockTransferRepository.findBySku(testSku)).thenReturn(List.of(transfer1, transfer2));

        // When
        List<StockTransfer> transfers = stockTransferService.getTransfersBySKU(testSku);

        // Then
        assertEquals(2, transfers.size());
        assertTrue(transfers.stream().allMatch(t -> t.getSku().equals(testSku)));
    }

    @Test
    @DisplayName("Should get all in-transit transfers")
    void shouldGetAllInTransitTransfers() {
        // Given
        StockTransfer transfer1 = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Transfer1");
        transfer1.markInTransit();

        StockTransfer transfer2 = StockTransfer.create("SKU-002", sourceLocation, destinationLocation,
            50, initiatedBy, "Transfer2");
        transfer2.markInTransit();

        when(stockTransferRepository.findInTransitTransfers()).thenReturn(List.of(transfer1, transfer2));

        // When
        List<StockTransfer> inTransitTransfers = stockTransferService.getInTransitTransfers();

        // Then
        assertEquals(2, inTransitTransfers.size());
        assertTrue(inTransitTransfers.stream().allMatch(t -> t.getStatus() == TransferStatus.IN_TRANSIT));
    }

    @Test
    @DisplayName("Should get transfers by status INITIATED")
    void shouldGetTransfersByStatusInitiated() {
        // Given
        StockTransfer transfer1 = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Transfer1");
        StockTransfer transfer2 = StockTransfer.create("SKU-002", sourceLocation, destinationLocation,
            50, initiatedBy, "Transfer2");

        when(stockTransferRepository.findByStatus(TransferStatus.INITIATED))
            .thenReturn(List.of(transfer1, transfer2));

        // When
        List<StockTransfer> transfers = stockTransferService.getTransfersByStatus(TransferStatus.INITIATED);

        // Then
        assertEquals(2, transfers.size());
        assertTrue(transfers.stream().allMatch(t -> t.getStatus() == TransferStatus.INITIATED));
    }

    @Test
    @DisplayName("Should get transfers by status COMPLETED")
    void shouldGetTransfersByStatusCompleted() {
        // Given
        StockTransfer transfer1 = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Transfer1");
        transfer1.markInTransit();
        transfer1.complete(100);

        when(stockTransferRepository.findByStatus(TransferStatus.COMPLETED))
            .thenReturn(List.of(transfer1));

        // When
        List<StockTransfer> transfers = stockTransferService.getTransfersByStatus(TransferStatus.COMPLETED);

        // Then
        assertEquals(1, transfers.size());
        assertEquals(TransferStatus.COMPLETED, transfers.get(0).getStatus());
    }

    @Test
    @DisplayName("Should get transfers within date range")
    void shouldGetTransfersWithinDateRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        StockTransfer transfer1 = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Transfer1");
        StockTransfer transfer2 = StockTransfer.create("SKU-002", sourceLocation, destinationLocation,
            50, initiatedBy, "Transfer2");

        when(stockTransferRepository.findByInitiatedAtBetween(start, end))
            .thenReturn(List.of(transfer1, transfer2));

        // When
        List<StockTransfer> transfers = stockTransferService.getTransfersByDateRange(start, end);

        // Then
        assertEquals(2, transfers.size());
    }

    @Test
    @DisplayName("Should get transfers with shrinkage")
    void shouldGetTransfersWithShrinkage() {
        // Given: Transfers with shrinkage
        StockTransfer transfer1 = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Transfer1");
        transfer1.markInTransit();
        transfer1.complete(95); // 5 units shrinkage

        StockTransfer transfer2 = StockTransfer.create("SKU-002", sourceLocation, destinationLocation,
            200, initiatedBy, "Transfer2");
        transfer2.markInTransit();
        transfer2.complete(190); // 10 units shrinkage

        when(stockTransferRepository.findTransfersWithShrinkage())
            .thenReturn(List.of(transfer1, transfer2));

        // When
        List<StockTransfer> transfersWithShrinkage = stockTransferService.getTransfersWithShrinkage();

        // Then
        assertEquals(2, transfersWithShrinkage.size());
        assertTrue(transfersWithShrinkage.stream().allMatch(StockTransfer::hasShrinkage));
    }

    @Test
    @DisplayName("Should count transfers by status")
    void shouldCountTransfersByStatus() {
        // Given
        when(stockTransferRepository.countByStatus(TransferStatus.IN_TRANSIT)).thenReturn(5L);

        // When
        long count = stockTransferService.countByStatus(TransferStatus.IN_TRANSIT);

        // Then
        assertEquals(5L, count);
    }

    @Test
    @DisplayName("Should handle complete transfer lifecycle")
    void shouldHandleCompleteTransferLifecycle() {
        // Given
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Replenishment");

        when(stockTransferRepository.save(any(StockTransfer.class))).thenReturn(transfer);
        when(stockTransferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        // When: Full lifecycle
        // 1. Initiate
        String transferId = stockTransferService.initiateTransfer(
            testSku, sourceLocation, destinationLocation, 100, initiatedBy, "Replenishment"
        );
        assertEquals(TransferStatus.INITIATED, transfer.getStatus());

        // 2. Mark in-transit
        stockTransferService.markInTransit(transferId);
        assertEquals(TransferStatus.IN_TRANSIT, transfer.getStatus());

        // 3. Complete
        stockTransferService.completeTransfer(transferId, 100);
        assertEquals(TransferStatus.COMPLETED, transfer.getStatus());

        // Then: All state transitions completed
        verify(stockTransferRepository, atLeast(3)).save(any(StockTransfer.class));
        verify(outboxRepository, atLeast(3)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should emit domain events on transfer operations")
    void shouldEmitDomainEventsOnTransferOperations() {
        // Given
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Replenishment");

        when(stockTransferRepository.save(any(StockTransfer.class))).thenReturn(transfer);

        // When
        stockTransferService.initiateTransfer(
            testSku, sourceLocation, destinationLocation, 100, initiatedBy, "Replenishment"
        );

        // Then: Events saved to outbox
        verify(outboxRepository).saveAll(argThat(events ->
            events.size() > 0 && events.stream()
                .anyMatch(e -> e.getEventType().equals("StockTransferInitiatedEvent"))
        ));
    }

    @Test
    @DisplayName("Should validate transfer between different zones")
    void shouldValidateTransferBetweenDifferentZones() {
        // Given: Transfer between different zones
        Location pickZone = Location.of("WH01", "PICK", "01", "01", "A", LocationType.GENERAL);
        Location reserveZone = Location.of("WH01", "RESERVE", "05", "03", "C", LocationType.GENERAL);

        StockTransfer transfer = StockTransfer.create(testSku, pickZone, reserveZone,
            100, initiatedBy, "Zone replenishment");

        when(stockTransferRepository.save(any(StockTransfer.class))).thenReturn(transfer);

        // When
        String transferId = stockTransferService.initiateTransfer(
            testSku, pickZone, reserveZone, 100, initiatedBy, "Zone replenishment"
        );

        // Then
        assertNotNull(transferId);
        verify(stockTransferRepository).save(any(StockTransfer.class));
    }

    @Test
    @DisplayName("Should track transfer timestamps correctly")
    void shouldTrackTransferTimestampsCorrectly() {
        // Given
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Replenishment");

        when(stockTransferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        LocalDateTime initiatedAt = transfer.getInitiatedAt();

        // When: Mark in-transit
        stockTransferService.markInTransit(transfer.getTransferId());
        LocalDateTime inTransitAt = transfer.getInTransitAt();

        // When: Complete
        stockTransferService.completeTransfer(transfer.getTransferId(), 100);
        LocalDateTime completedAt = transfer.getCompletedAt();

        // Then: Timestamps should be in correct order
        assertNotNull(initiatedAt);
        assertNotNull(inTransitAt);
        assertNotNull(completedAt);
        assertTrue(inTransitAt.isAfter(initiatedAt) || inTransitAt.isEqual(initiatedAt));
        assertTrue(completedAt.isAfter(inTransitAt) || completedAt.isEqual(inTransitAt));
    }

    @Test
    @DisplayName("Should calculate shrinkage percentage correctly")
    void shouldCalculateShrinkagePercentageCorrectly() {
        // Given
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            1000, initiatedBy, "Large transfer");
        transfer.markInTransit();

        when(stockTransferRepository.findById(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        // When: Receive 950 units (50 units = 5% shrinkage)
        stockTransferService.completeTransfer(transfer.getTransferId(), 950);

        // Then
        assertTrue(transfer.hasShrinkage());
        assertEquals(50, transfer.getShrinkageQuantity());
        assertEquals(5.0, transfer.getShrinkagePercentage(), 0.01);
    }

    @Test
    @DisplayName("Should handle multiple concurrent transfers for same SKU")
    void shouldHandleMultipleConcurrentTransfersForSameSku() {
        // Given: Multiple transfers for same SKU
        StockTransfer transfer1 = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            100, initiatedBy, "Transfer1");
        StockTransfer transfer2 = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            200, initiatedBy, "Transfer2");

        when(stockTransferRepository.save(any(StockTransfer.class)))
            .thenReturn(transfer1)
            .thenReturn(transfer2);

        // When: Initiate both transfers
        String transferId1 = stockTransferService.initiateTransfer(
            testSku, sourceLocation, destinationLocation, 100, initiatedBy, "Transfer1"
        );
        String transferId2 = stockTransferService.initiateTransfer(
            testSku, sourceLocation, destinationLocation, 200, initiatedBy, "Transfer2"
        );

        // Then: Both transfers created
        assertNotNull(transferId1);
        assertNotNull(transferId2);
        verify(stockTransferRepository, times(2)).save(any(StockTransfer.class));
    }

    @Test
    @DisplayName("Should validate transfer reason is tracked")
    void shouldValidateTransferReasonIsTracked() {
        // Given
        String reason = "Emergency stock replenishment for high-demand SKU";
        StockTransfer transfer = StockTransfer.create(testSku, sourceLocation, destinationLocation,
            500, initiatedBy, reason);

        when(stockTransferRepository.save(any(StockTransfer.class))).thenReturn(transfer);

        // When
        stockTransferService.initiateTransfer(
            testSku, sourceLocation, destinationLocation, 500, initiatedBy, reason
        );

        // Then
        assertEquals(reason, transfer.getReason());
    }
}
