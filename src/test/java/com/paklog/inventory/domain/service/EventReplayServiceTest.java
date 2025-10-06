package com.paklog.inventory.domain.service;

import com.paklog.inventory.domain.event.*;
import com.paklog.inventory.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Critical tests for EventReplayService - the core of time-travel functionality.
 * These tests ensure event replay correctly reconstructs historical inventory state.
 */
class EventReplayServiceTest {

    private EventReplayService eventReplayService;
    private String testSku;
    private LocalDateTime baselineTime;
    private LocalDateTime targetTime;

    @BeforeEach
    void setUp() {
        eventReplayService = new EventReplayService();
        testSku = "SKU-TEST-001";
        baselineTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        targetTime = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
    }

    @Test
    @DisplayName("Should replay stock level changed events correctly")
    void shouldReplayStockLevelChangedEvents() {
        // Given: Baseline snapshot with 100 units
        InventorySnapshot baselineSnapshot = createBaselineSnapshot(100, 0);

        // And: Events showing stock changes
        StockLevel oldLevel = StockLevel.of(100, 0);
        StockLevel newLevel = StockLevel.of(150, 20);

        List<DomainEvent> events = List.of(
            new StockLevelChangedEvent(testSku, oldLevel, newLevel, "RECEIPT")
        );

        // When: Replaying events
        InventorySnapshot result = eventReplayService.replayEvents(
            baselineSnapshot, events, targetTime, testSku
        );

        // Then: Final snapshot should reflect the changes
        assertEquals(150, result.getQuantityOnHand());
        assertEquals(20, result.getQuantityAllocated());
        assertEquals(130, result.getQuantityAvailable()); // 150 - 20
    }

    @Test
    @DisplayName("Should replay stock status changed events correctly")
    void shouldReplayStockStatusChangedEvents() {
        // Given: Baseline snapshot with 100 AVAILABLE units
        InventorySnapshot baselineSnapshot = createBaselineSnapshot(100, 0);

        // And: Events moving 30 units to QUARANTINE
        List<DomainEvent> events = List.of(
            new StockStatusChangedEvent(testSku, StockStatus.AVAILABLE,
                StockStatus.QUARANTINE, 30, "Quality issue", null)
        );

        // When: Replaying events
        InventorySnapshot result = eventReplayService.replayEvents(
            baselineSnapshot, events, targetTime, testSku
        );

        // Then: Status breakdown should be updated
        assertEquals(70, result.getQuantityInStatus(StockStatus.AVAILABLE));
        assertEquals(30, result.getQuantityInStatus(StockStatus.QUARANTINE));
    }

    @Test
    @DisplayName("Should replay multiple status changes correctly")
    void shouldReplayMultipleStatusChanges() {
        // Given: Baseline snapshot
        InventorySnapshot baselineSnapshot = createBaselineSnapshot(100, 0);

        // And: Multiple status change events
        List<DomainEvent> events = List.of(
            new StockStatusChangedEvent(testSku, StockStatus.AVAILABLE,
                StockStatus.QUARANTINE, 20, "QC check", null),
            new StockStatusChangedEvent(testSku, StockStatus.AVAILABLE,
                StockStatus.DAMAGED, 15, "Found damaged", null),
            new StockStatusChangedEvent(testSku, StockStatus.QUARANTINE,
                StockStatus.AVAILABLE, 10, "QC passed", null)
        );

        // When: Replaying events
        InventorySnapshot result = eventReplayService.replayEvents(
            baselineSnapshot, events, targetTime, testSku
        );

        // Then: Final status should be correct
        // Started: 100 AVAILABLE
        // -20 to QUARANTINE = 80 AVAILABLE, 20 QUARANTINE
        // -15 to DAMAGED = 65 AVAILABLE, 20 QUARANTINE, 15 DAMAGED
        // +10 from QUARANTINE = 75 AVAILABLE, 10 QUARANTINE, 15 DAMAGED
        assertEquals(75, result.getQuantityInStatus(StockStatus.AVAILABLE));
        assertEquals(10, result.getQuantityInStatus(StockStatus.QUARANTINE));
        assertEquals(15, result.getQuantityInStatus(StockStatus.DAMAGED));
    }

    @Test
    @DisplayName("Should replay hold placed and released events")
    void shouldReplayHoldEvents() {
        // Given: Baseline snapshot
        InventorySnapshot baselineSnapshot = createBaselineSnapshot(100, 0);

        // And: Hold events
        String holdId1 = "HOLD-001";
        String holdId2 = "HOLD-002";

        List<DomainEvent> events = List.of(
            new InventoryHoldPlacedEvent(testSku, holdId1, HoldType.QUALITY_HOLD, 20,
                "Quality check", null),
            new InventoryHoldPlacedEvent(testSku, holdId2, HoldType.CREDIT_HOLD, 15,
                "Credit hold", null),
            new InventoryHoldReleasedEvent(testSku, holdId1, 20, "USER1")
        );

        // When: Replaying events
        InventorySnapshot result = eventReplayService.replayEvents(
            baselineSnapshot, events, targetTime, testSku
        );

        // Then: Should have one active hold
        assertEquals(1, result.getActiveHolds().size());
        assertEquals(15, result.getTotalHeldQuantity());

        InventoryHoldSnapshot remainingHold = result.getActiveHolds().get(0);
        assertEquals(holdId2, remainingHold.getHoldId());
        assertEquals(15, remainingHold.getQuantity());
    }

    @Test
    @DisplayName("Should replay valuation changed events")
    void shouldReplayValuationChangedEvents() {
        // Given: Baseline snapshot with initial valuation
        InventorySnapshot baselineSnapshot = createBaselineSnapshot(100, 0);

        // And: Valuation change events
        List<DomainEvent> events = List.of(
            new InventoryValuationChangedEvent(testSku,
                ValuationMethod.WEIGHTED_AVERAGE,
                BigDecimal.valueOf(10.00),
                BigDecimal.valueOf(12.50),
                BigDecimal.valueOf(1000.00),
                BigDecimal.valueOf(1250.00),
                100,
                "STOCK_RECEIPT")
        );

        // When: Replaying events
        InventorySnapshot result = eventReplayService.replayEvents(
            baselineSnapshot, events, targetTime, testSku
        );

        // Then: Valuation should be updated
        assertTrue(result.getUnitCost().isPresent());
        assertEquals(new BigDecimal("12.50"), result.getUnitCost().get());
        assertEquals(new BigDecimal("1250.00"), result.getTotalValue().get());
        assertEquals("WEIGHTED_AVERAGE", result.getValuationMethod().get());
    }

    @Test
    @DisplayName("Should replay ABC classification changed events")
    void shouldReplayABCClassificationChangedEvents() {
        // Given: Baseline snapshot
        InventorySnapshot baselineSnapshot = createBaselineSnapshot(100, 0);

        // And: ABC classification change events
        List<DomainEvent> events = List.of(
            new ABCClassificationChangedEvent(testSku, null, ABCClass.A,
                ABCCriteria.VALUE_BASED, "ANNUAL_CLASSIFICATION")
        );

        // When: Replaying events
        InventorySnapshot result = eventReplayService.replayEvents(
            baselineSnapshot, events, targetTime, testSku
        );

        // Then: ABC class should be updated
        assertTrue(result.getAbcClass().isPresent());
        assertEquals("A", result.getAbcClass().get());
        assertEquals("VALUE_BASED", result.getAbcCriteria().get());
    }

    @Test
    @DisplayName("Should filter events by SKU")
    void shouldFilterEventsBySku() {
        // Given: Baseline snapshot for SKU-001
        InventorySnapshot baselineSnapshot = createBaselineSnapshot(100, 0);

        // And: Events for different SKUs
        StockLevel oldLevel = StockLevel.of(100, 0);
        StockLevel newLevel = StockLevel.of(150, 0);

        List<DomainEvent> events = List.of(
            new StockLevelChangedEvent("SKU-001", oldLevel, newLevel, "RECEIPT"),
            new StockLevelChangedEvent("SKU-002", oldLevel, newLevel, "RECEIPT"),
            new StockLevelChangedEvent("SKU-001", newLevel, StockLevel.of(160, 0), "RECEIPT")
        );

        // When: Replaying events for SKU-001
        InventorySnapshot result = eventReplayService.replayEvents(
            baselineSnapshot, events, targetTime, "SKU-001"
        );

        // Then: Should only apply SKU-001 events
        assertEquals(160, result.getQuantityOnHand());
    }

    @Test
    @DisplayName("Should filter events by time range")
    void shouldFilterEventsByTimeRange() {
        // Given: Baseline snapshot at Jan 1
        InventorySnapshot baselineSnapshot = createBaselineSnapshot(100, 0);

        // And: Events at different times
        StockLevel level1 = StockLevel.of(100, 0);
        StockLevel level2 = StockLevel.of(110, 0);
        StockLevel level3 = StockLevel.of(120, 0);
        StockLevel level4 = StockLevel.of(130, 0);

        List<DomainEvent> events = List.of(
            // Before baseline - should be ignored
            new StockLevelChangedEvent(testSku, level1, level2, "RECEIPT"),
            // Between baseline and target - should be applied
            new StockLevelChangedEvent(testSku, level2, level3, "RECEIPT"),
            // After target - should be ignored
            new StockLevelChangedEvent(testSku, level3, level4, "RECEIPT")
        );

        // When: Replaying events
        InventorySnapshot result = eventReplayService.replayEvents(
            baselineSnapshot, events, targetTime, testSku
        );

        // Then: Should only apply events in time range
        assertEquals(120, result.getQuantityOnHand());
    }

    @Test
    @DisplayName("Should handle empty event list")
    void shouldHandleEmptyEventList() {
        // Given: Baseline snapshot
        InventorySnapshot baselineSnapshot = createBaselineSnapshot(100, 20);

        // And: No events
        List<DomainEvent> events = Collections.emptyList();

        // When: Replaying events
        InventorySnapshot result = eventReplayService.replayEvents(
            baselineSnapshot, events, targetTime, testSku
        );

        // Then: Should return state identical to baseline
        assertEquals(100, result.getQuantityOnHand());
        assertEquals(20, result.getQuantityAllocated());
    }

    @Test
    @DisplayName("Should replay complex event sequence correctly")
    void shouldReplayComplexEventSequence() {
        // Given: Baseline snapshot
        InventorySnapshot baselineSnapshot = createBaselineSnapshot(1000, 0);

        // And: Complex sequence of events over a year
        List<DomainEvent> events = List.of(
            // Receipt in January
            new StockLevelChangedEvent(testSku, StockLevel.of(1000, 0),
                StockLevel.of(1500, 0), "RECEIPT"),

            // Allocation in February
            new StockLevelChangedEvent(testSku, StockLevel.of(1500, 0),
                StockLevel.of(1500, 200), "ALLOCATION"),

            // Status change in March
            new StockStatusChangedEvent(testSku, StockStatus.AVAILABLE,
                StockStatus.DAMAGED, 50, "Warehouse damage", null),

            // Hold in April
            new InventoryHoldPlacedEvent(testSku, "HOLD-001", HoldType.QUALITY_HOLD, 100,
                "Quality review", null),

            // Valuation update in June
            new InventoryValuationChangedEvent(testSku,
                ValuationMethod.WEIGHTED_AVERAGE,
                BigDecimal.valueOf(10.00),
                BigDecimal.valueOf(11.00),
                BigDecimal.valueOf(15000.00),
                BigDecimal.valueOf(16500.00),
                1500,
                "STOCK_RECEIPT"),

            // ABC classification in July
            new ABCClassificationChangedEvent(testSku, null, ABCClass.A,
                ABCCriteria.VALUE_BASED, "ANNUAL_CLASSIFICATION"),

            // Release hold in August
            new InventoryHoldReleasedEvent(testSku, "HOLD-001", 100, "SYSTEM"),

            // More receipts in November
            new StockLevelChangedEvent(testSku, StockLevel.of(1500, 200),
                StockLevel.of(2000, 200), "RECEIPT")
        );

        // When: Replaying events to year end
        InventorySnapshot result = eventReplayService.replayEvents(
            baselineSnapshot, events, targetTime, testSku
        );

        // Then: Final state should be correct
        assertEquals(2000, result.getQuantityOnHand());
        assertEquals(200, result.getQuantityAllocated());
        assertEquals(50, result.getQuantityInStatus(StockStatus.DAMAGED));
        assertEquals(0, result.getActiveHolds().size()); // Hold was released
        assertEquals(new BigDecimal("11.00"), result.getUnitCost().get());
        assertEquals("A", result.getAbcClass().get());
    }

    @Test
    @DisplayName("Should maintain event order when replaying")
    void shouldMaintainEventOrder() {
        // Given: Baseline snapshot
        InventorySnapshot baselineSnapshot = createBaselineSnapshot(100, 0);

        // And: Events that depend on order
        List<DomainEvent> events = List.of(
            // First, place a hold
            new InventoryHoldPlacedEvent(testSku, "HOLD-001", HoldType.QUALITY_HOLD, 50,
                "QC", null),
            // Then release it
            new InventoryHoldReleasedEvent(testSku, "HOLD-001", 50, "USER1"),
            // Then place another
            new InventoryHoldPlacedEvent(testSku, "HOLD-002", HoldType.CREDIT_HOLD, 30,
                "Credit", null)
        );

        // When: Replaying events
        InventorySnapshot result = eventReplayService.replayEvents(
            baselineSnapshot, events, targetTime, testSku
        );

        // Then: Final state should reflect correct order
        assertEquals(1, result.getActiveHolds().size());
        assertEquals("HOLD-002", result.getActiveHolds().get(0).getHoldId());
        assertEquals(30, result.getTotalHeldQuantity());
    }

    // Helper methods

    private InventorySnapshot createBaselineSnapshot(int quantityOnHand, int quantityAllocated) {
        ProductStock productStock = ProductStock.create(testSku, quantityOnHand);

        if (quantityAllocated > 0) {
            productStock.allocate(quantityAllocated);
        }

        return InventorySnapshot.fromProductStock(
            productStock,
            baselineTime,
            SnapshotType.DAILY,
            SnapshotReason.SCHEDULED,
            "TEST_USER"
        );
    }
}
