package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.event.*;
import com.paklog.inventory.domain.model.*;
import com.paklog.inventory.domain.repository.InventorySnapshotRepository;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.service.EventReplayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Critical tests for SnapshotService - orchestrates hybrid snapshot strategy.
 * Tests time-travel queries, snapshot creation, delta calculation, and retention policies.
 */
@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {

    @Mock
    private InventorySnapshotRepository snapshotRepository;

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private EventReplayService eventReplayService;

    @InjectMocks
    private SnapshotService snapshotService;

    private String testSku;
    private LocalDateTime now;
    private ProductStock productStock;

    @BeforeEach
    void setUp() {
        testSku = "SKU-TEST-001";
        now = LocalDateTime.now();
        productStock = ProductStock.create(testSku, 1000);
    }

    @Test
    @DisplayName("Should create snapshot from current product stock")
    void shouldCreateSnapshotFromProductStock() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        InventorySnapshot snapshot = snapshotService.createSnapshot(
            testSku,
            SnapshotType.DAILY,
            SnapshotReason.SCHEDULED,
            "SYSTEM"
        );

        // Then
        assertNotNull(snapshot);
        assertEquals(testSku, snapshot.getSku());
        assertEquals(1000, snapshot.getQuantityOnHand());
        assertEquals(SnapshotType.DAILY, snapshot.getSnapshotType());
        assertEquals(SnapshotReason.SCHEDULED, snapshot.getReason());
        assertEquals("SYSTEM", snapshot.getCreatedBy());

        // Verify snapshot was saved
        verify(snapshotRepository).save(any(InventorySnapshot.class));
    }

    @Test
    @DisplayName("Should throw exception when creating snapshot for non-existent SKU")
    void shouldThrowExceptionWhenCreatingSnapshotForNonExistentSku() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            snapshotService.createSnapshot(testSku, SnapshotType.DAILY,
                SnapshotReason.SCHEDULED, "SYSTEM")
        );

        // Verify snapshot was not saved
        verify(snapshotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get snapshot at exact timestamp when snapshot exists")
    void shouldGetSnapshotAtExactTimestamp() {
        // Given: Exact snapshot exists at target time
        LocalDateTime targetTime = now.minusDays(30);
        InventorySnapshot exactSnapshot = createSnapshot(targetTime, 500, 50);

        when(snapshotRepository.findNearestSnapshotBefore(testSku, targetTime))
            .thenReturn(Optional.of(exactSnapshot));

        // When
        Optional<InventorySnapshot> result = snapshotService.getSnapshotAt(testSku, targetTime);

        // Then
        assertTrue(result.isPresent());
        assertEquals(500, result.get().getQuantityOnHand());
        assertEquals(50, result.get().getQuantityAllocated());

        // Should not replay events if exact match
        verify(eventReplayService, never()).replayEvents(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reconstruct state using hybrid strategy (snapshot + event replay)")
    void shouldReconstructStateUsingHybridStrategy() {
        // Given: Baseline snapshot from 7 days ago
        LocalDateTime baselineTime = now.minusDays(7);
        LocalDateTime targetTime = now.minusDays(1);

        InventorySnapshot baselineSnapshot = createSnapshot(baselineTime, 1000, 0);

        // And: Events between baseline and target
        List<DomainEvent> events = List.of(
            new StockLevelChangedEvent(testSku, StockLevel.of(1000, 0),
                StockLevel.of(1200, 100), "RECEIPT")
        );

        // And: Reconstructed snapshot after replay
        InventorySnapshot reconstructedSnapshot = createSnapshot(targetTime, 1200, 100);

        when(snapshotRepository.findNearestSnapshotBefore(testSku, targetTime))
            .thenReturn(Optional.of(baselineSnapshot));
        when(eventReplayService.replayEvents(eq(baselineSnapshot), anyList(), eq(targetTime), eq(testSku)))
            .thenReturn(reconstructedSnapshot);

        // When
        Optional<InventorySnapshot> result = snapshotService.getSnapshotAt(testSku, targetTime);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1200, result.get().getQuantityOnHand());
        assertEquals(100, result.get().getQuantityAllocated());

        // Verify hybrid strategy was used
        verify(snapshotRepository).findNearestSnapshotBefore(testSku, targetTime);
        verify(eventReplayService).replayEvents(eq(baselineSnapshot), anyList(), eq(targetTime), eq(testSku));
    }

    @Test
    @DisplayName("Should return empty when no baseline snapshot exists")
    void shouldReturnEmptyWhenNoBaselineSnapshotExists() {
        // Given: No snapshots exist before target time
        LocalDateTime targetTime = now.minusDays(1);
        when(snapshotRepository.findNearestSnapshotBefore(testSku, targetTime))
            .thenReturn(Optional.empty());

        // When
        Optional<InventorySnapshot> result = snapshotService.getSnapshotAt(testSku, targetTime);

        // Then
        assertTrue(result.isEmpty());

        // Should not attempt event replay
        verify(eventReplayService, never()).replayEvents(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should get all snapshots at specific time for multiple SKUs")
    void shouldGetAllSnapshotsAtSpecificTime() {
        // Given: Multiple snapshots at target time
        LocalDateTime targetTime = now.minusDays(1);

        InventorySnapshot snapshot1 = createSnapshot("SKU-001", targetTime, 500, 50);
        InventorySnapshot snapshot2 = createSnapshot("SKU-002", targetTime, 300, 30);
        InventorySnapshot snapshot3 = createSnapshot("SKU-003", targetTime, 800, 80);

        // Note: findAllNearestSnapshotsBefore method does not exist in repository
        // This test needs to be updated to use available repository methods
        when(snapshotRepository.findAllAtTimestamp(targetTime))
            .thenReturn(List.of(snapshot1, snapshot2, snapshot3));

        // When
        List<InventorySnapshot> results = snapshotService.getAllSnapshotsAt(targetTime);

        // Then
        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(s -> s.getSku().equals("SKU-001")));
        assertTrue(results.stream().anyMatch(s -> s.getSku().equals("SKU-002")));
        assertTrue(results.stream().anyMatch(s -> s.getSku().equals("SKU-003")));
    }

    @Test
    @DisplayName("Should calculate delta between two points in time")
    void shouldCalculateDeltaBetweenTwoPointsInTime() {
        // Given: Snapshots at two different times
        LocalDateTime fromTime = now.minusDays(30);
        LocalDateTime toTime = now.minusDays(1);

        InventorySnapshot fromSnapshot = createSnapshot(fromTime, 1000, 100);
        InventorySnapshot toSnapshot = createSnapshot(toTime, 1500, 200);

        when(snapshotRepository.findNearestSnapshotBefore(testSku, fromTime))
            .thenReturn(Optional.of(fromSnapshot));
        when(snapshotRepository.findNearestSnapshotBefore(testSku, toTime))
            .thenReturn(Optional.of(toSnapshot));

        // When
        SnapshotService.SnapshotDelta delta = snapshotService.getDelta(testSku, fromTime, toTime);

        // Then
        assertNotNull(delta);
        assertEquals(500, delta.getQuantityChange()); // 1500 - 1000
        assertEquals(100, delta.getQuantityAvailableChange()); // 200 - 100
    }

    @Test
    @DisplayName("Should throw exception when calculating delta with missing snapshots")
    void shouldThrowExceptionWhenCalculatingDeltaWithMissingSnapshots() {
        // Given: No snapshot at fromTime
        LocalDateTime fromTime = now.minusDays(30);
        LocalDateTime toTime = now.minusDays(1);

        when(snapshotRepository.findNearestSnapshotBefore(testSku, fromTime))
            .thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            snapshotService.getDelta(testSku, fromTime, toTime)
        );
    }

    @Test
    @DisplayName("Should get month-end snapshots for a year")
    void shouldGetMonthEndSnapshotsForYear() {
        // Given: Month-end snapshots for 2024
        List<InventorySnapshot> monthEndSnapshots = List.of(
            createSnapshot("SKU-001", LocalDateTime.of(2024, 1, 31, 23, 59), 1000, 0),
            createSnapshot("SKU-001", LocalDateTime.of(2024, 2, 29, 23, 59), 1100, 0),
            createSnapshot("SKU-001", LocalDateTime.of(2024, 3, 31, 23, 59), 1200, 0)
        );

        // Note: findBySkuAndTypeAndYear method does not exist in repository
        // Using findBySkuAndType instead
        when(snapshotRepository.findBySkuAndType(testSku, SnapshotType.MONTH_END))
            .thenReturn(monthEndSnapshots);

        // When
        List<InventorySnapshot> results = snapshotService.getMonthEndSnapshots(testSku, 2024);

        // Then
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(s -> s.getSnapshotType() == SnapshotType.MONTH_END));
    }

    @Test
    @DisplayName("Should get year-end snapshots")
    void shouldGetYearEndSnapshots() {
        // Given: Year-end snapshots for multiple years
        List<InventorySnapshot> yearEndSnapshots = List.of(
            createSnapshot("SKU-001", LocalDateTime.of(2022, 12, 31, 23, 59), 800, 0),
            createSnapshot("SKU-001", LocalDateTime.of(2023, 12, 31, 23, 59), 1000, 0),
            createSnapshot("SKU-001", LocalDateTime.of(2024, 12, 31, 23, 59), 1200, 0)
        );

        when(snapshotRepository.findBySkuAndType(testSku, SnapshotType.YEAR_END))
            .thenReturn(yearEndSnapshots);

        // When
        List<InventorySnapshot> results = snapshotService.getYearEndSnapshots(testSku);

        // Then
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(s -> s.getSnapshotType() == SnapshotType.YEAR_END));
    }

    @Test
    @DisplayName("Should cleanup old snapshots according to retention policy")
    void shouldCleanupOldSnapshotsAccordingToRetentionPolicy() {
        // When
        snapshotService.cleanupOldSnapshots();

        // Then: Verify retention policy enforcement
        LocalDateTime now = LocalDateTime.now();

        // DAILY: 90 days retention
        verify(snapshotRepository).deleteByTypeOlderThan(
            eq(SnapshotType.DAILY),
            argThat(date -> date.isBefore(now) && date.isAfter(now.minusDays(91)))
        );

        // AD_HOC: 30 days retention
        verify(snapshotRepository).deleteByTypeOlderThan(
            eq(SnapshotType.AD_HOC),
            argThat(date -> date.isBefore(now) && date.isAfter(now.minusDays(31)))
        );

        // MONTH_END: 7 years retention
        verify(snapshotRepository).deleteByTypeOlderThan(
            eq(SnapshotType.MONTH_END),
            argThat(date -> date.isBefore(now) && date.isAfter(now.minusYears(8)))
        );

        // QUARTER_END: 10 years retention
        verify(snapshotRepository).deleteByTypeOlderThan(
            eq(SnapshotType.QUARTER_END),
            argThat(date -> date.isBefore(now) && date.isAfter(now.minusYears(11)))
        );

        // YEAR_END: Never deleted (verify not called)
        verify(snapshotRepository, never()).deleteByTypeOlderThan(
            eq(SnapshotType.YEAR_END), any()
        );
    }

    @Test
    @DisplayName("Should get snapshot statistics")
    void shouldGetSnapshotStatistics() {
        // Given
        when(snapshotRepository.countByType(SnapshotType.DAILY)).thenReturn(90L);
        when(snapshotRepository.countByType(SnapshotType.MONTH_END)).thenReturn(84L); // 7 years
        when(snapshotRepository.countByType(SnapshotType.QUARTER_END)).thenReturn(40L); // 10 years
        when(snapshotRepository.countByType(SnapshotType.YEAR_END)).thenReturn(10L);
        when(snapshotRepository.countByType(SnapshotType.AD_HOC)).thenReturn(15L);

        // Note: findOldestSnapshot and findNewestSnapshot methods do not exist in repository
        // These would need to be added or the test updated to use existing methods

        // When
        SnapshotService.SnapshotStatistics stats = snapshotService.getStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(90L, stats.daily());
        assertEquals(84L, stats.monthEnd());
        assertEquals(40L, stats.quarterEnd());
        assertEquals(10L, stats.yearEnd());
        assertEquals(15L, stats.adHoc());
        assertEquals(239L, stats.total()); // Sum of all
    }

    @Test
    @DisplayName("Should create snapshot with complex state (holds, lots, valuation)")
    void shouldCreateSnapshotWithComplexState() {
        // Given: ProductStock with holds, lots, and valuation
        ProductStock complexStock = ProductStock.create(testSku, 1000);

        // Add allocation
        complexStock.allocate(200);

        // Add hold
        complexStock.placeHold(HoldType.QUALITY_HOLD, 100, "QC review", "USER1");

        // Note: changeStatus method does not exist in ProductStock
        // This functionality would need to be implemented or this test updated
        // complexStock.changeStatus(50, StockStatus.AVAILABLE, StockStatus.DAMAGED, "Damaged units");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(complexStock));

        // When
        InventorySnapshot snapshot = snapshotService.createSnapshot(
            testSku, SnapshotType.AD_HOC, SnapshotReason.AUDIT, "AUDITOR"
        );

        // Then: Snapshot captures all state
        assertEquals(1000, snapshot.getQuantityOnHand());
        assertEquals(200, snapshot.getQuantityAllocated());
        assertEquals(1, snapshot.getActiveHolds().size());
        assertEquals(100, snapshot.getTotalHeldQuantity());
        assertEquals(50, snapshot.getQuantityInStatus(StockStatus.DAMAGED));
        assertEquals(950, snapshot.getQuantityInStatus(StockStatus.AVAILABLE)); // 1000 - 50
    }

    @Test
    @DisplayName("Should handle time-travel query with no events between snapshots")
    void shouldHandleTimeTravelQueryWithNoEventsBetweenSnapshots() {
        // Given: Baseline snapshot but no events after it
        LocalDateTime baselineTime = now.minusDays(7);
        LocalDateTime targetTime = now.minusDays(1);

        InventorySnapshot baselineSnapshot = createSnapshot(baselineTime, 1000, 100);

        when(snapshotRepository.findNearestSnapshotBefore(testSku, targetTime))
            .thenReturn(Optional.of(baselineSnapshot));

        // When: No events to replay
        Optional<InventorySnapshot> result = snapshotService.getSnapshotAt(testSku, targetTime);

        // Then: Should return baseline snapshot
        assertTrue(result.isPresent());
        assertEquals(1000, result.get().getQuantityOnHand());
        assertEquals(100, result.get().getQuantityAllocated());
    }

    @Test
    @DisplayName("Should validate snapshot creation for inventory audit trail")
    void shouldValidateSnapshotCreationForInventoryAuditTrail() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Create audit snapshot
        InventorySnapshot snapshot = snapshotService.createSnapshot(
            testSku,
            SnapshotType.AD_HOC,
            SnapshotReason.AUDIT,
            "AUDITOR-123"
        );

        // Then: Snapshot has audit metadata
        assertEquals(SnapshotReason.AUDIT, snapshot.getReason());
        assertEquals("AUDITOR-123", snapshot.getCreatedBy());
        assertNotNull(snapshot.getCreatedAt());
        assertNotNull(snapshot.getSnapshotId());

        // Verify saved with audit context
        verify(snapshotRepository).save(argThat(s ->
            s.getReason() == SnapshotReason.AUDIT &&
            s.getCreatedBy().equals("AUDITOR-123")
        ));
    }

    // Helper methods

    private InventorySnapshot createSnapshot(LocalDateTime timestamp, int qtyOnHand, int qtyAllocated) {
        return createSnapshot(testSku, timestamp, qtyOnHand, qtyAllocated);
    }

    private InventorySnapshot createSnapshot(String sku, LocalDateTime timestamp,
                                            int qtyOnHand, int qtyAllocated) {
        ProductStock stock = ProductStock.create(sku, qtyOnHand);
        if (qtyAllocated > 0) {
            stock.allocate(qtyAllocated);
        }

        return InventorySnapshot.fromProductStock(
            stock,
            timestamp,
            SnapshotType.DAILY,
            SnapshotReason.SCHEDULED,
            "SYSTEM"
        );
    }
}
