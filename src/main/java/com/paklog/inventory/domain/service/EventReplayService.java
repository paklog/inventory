package com.paklog.inventory.domain.service;

import com.paklog.inventory.domain.event.*;
import com.paklog.inventory.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Domain service for replaying events to reconstruct historical inventory state.
 * Core component of the hybrid snapshot strategy for time-travel queries.
 */
@Component
public class EventReplayService {

    private static final Logger logger = LoggerFactory.getLogger(EventReplayService.class);

    /**
     * Replay events from a baseline snapshot to target timestamp
     */
    public InventorySnapshot replayEvents(InventorySnapshot baselineSnapshot,
                                         List<DomainEvent> events,
                                         LocalDateTime targetTimestamp,
                                         String sku) {
        logger.debug("Replaying {} events from {} to {} for SKU={}",
                events.size(), baselineSnapshot.getSnapshotTimestamp(), targetTimestamp, sku);

        // Start with baseline state
        SnapshotBuilder builder = SnapshotBuilder.fromSnapshot(baselineSnapshot);

        // Filter and sort events
        List<DomainEvent> relevantEvents = events.stream()
            .filter(e -> isEventRelevant(e, sku))
            .filter(e -> isEventInTimeRange(e, baselineSnapshot.getSnapshotTimestamp(), targetTimestamp))
            .sorted(Comparator.comparing(this::getEventTimestamp))
            .toList();

        logger.debug("Applying {} relevant events", relevantEvents.size());

        // Replay each event
        for (DomainEvent event : relevantEvents) {
            applyEvent(builder, event);
        }

        // Build final snapshot
        return builder.build(targetTimestamp, SnapshotType.AD_HOC, SnapshotReason.INVESTIGATION,
                "SYSTEM");
    }

    /**
     * Apply a single event to the snapshot builder
     */
    private void applyEvent(SnapshotBuilder builder, DomainEvent event) {
        switch (event) {
            case StockLevelChangedEvent e -> applyStockLevelChanged(builder, e);
            case StockStatusChangedEvent e -> applyStockStatusChanged(builder, e);
            case InventoryHoldPlacedEvent e -> applyHoldPlaced(builder, e);
            case InventoryHoldReleasedEvent e -> applyHoldReleased(builder, e);
            case InventoryValuationChangedEvent e -> applyValuationChanged(builder, e);
            case ABCClassificationChangedEvent e -> applyABCClassificationChanged(builder, e);
            default -> logger.warn("Unknown event type for replay: {}", event.getClass().getSimpleName());
        }
    }

    private void applyStockLevelChanged(SnapshotBuilder builder, StockLevelChangedEvent event) {
        builder.setQuantityOnHand(event.getNewStockLevel().getQuantityOnHand());
        builder.setQuantityAllocated(event.getNewStockLevel().getQuantityAllocated());
        builder.setQuantityReserved(event.getNewStockLevel().getQuantityReserved());
        builder.setQuantityAvailable(event.getNewStockLevel().getAvailableToPromise());
    }

    private void applyStockStatusChanged(SnapshotBuilder builder, StockStatusChangedEvent event) {
        // Adjust quantity in old status
        builder.adjustStockByStatus(event.getPreviousStatus(), -event.getQuantity());

        // Adjust quantity in new status
        builder.adjustStockByStatus(event.getNewStatus(), event.getQuantity());
    }

    private void applyHoldPlaced(SnapshotBuilder builder, InventoryHoldPlacedEvent event) {
        InventoryHoldSnapshot hold = InventoryHoldSnapshot.of(
            event.getHoldId(), event.getHoldType().name(), event.getQuantityOnHold(), event.getReason(),
            "SYSTEM", event.getOccurredAt(), null, true
        );
        builder.addHold(hold);
    }

    private void applyHoldReleased(SnapshotBuilder builder, InventoryHoldReleasedEvent event) {
        builder.removeHold(event.getHoldId());
    }

    private void applyValuationChanged(SnapshotBuilder builder, InventoryValuationChangedEvent event) {
        builder.setUnitCost(event.getNewUnitCost());
        builder.setTotalValue(event.getNewTotalValue());
        builder.setValuationMethod(event.getValuationMethod().name());
    }

    private void applyABCClassificationChanged(SnapshotBuilder builder, ABCClassificationChangedEvent event) {
        builder.setAbcClass(event.getNewClass().name());
        builder.setAbcCriteria(event.getCriteria().name());
    }

    private boolean isEventRelevant(DomainEvent event, String sku) {
        // Check if event is for the target SKU
        return switch (event) {
            case StockLevelChangedEvent e -> e.getSku().equals(sku);
            case StockStatusChangedEvent e -> e.getSku().equals(sku);
            case InventoryHoldPlacedEvent e -> e.getSku().equals(sku);
            case InventoryHoldReleasedEvent e -> e.getSku().equals(sku);
            case InventoryValuationChangedEvent e -> e.getSku().equals(sku);
            case ABCClassificationChangedEvent e -> e.getSku().equals(sku);
            default -> false;
        };
    }

    private boolean isEventInTimeRange(DomainEvent event, LocalDateTime start, LocalDateTime end) {
        LocalDateTime eventTime = getEventTimestamp(event);
        return eventTime.isAfter(start) && !eventTime.isAfter(end);
    }

    private LocalDateTime getEventTimestamp(DomainEvent event) {
        return switch (event) {
            case StockLevelChangedEvent e -> e.getOccurredAt();
            case StockStatusChangedEvent e -> e.getOccurredAt();
            case InventoryHoldPlacedEvent e -> e.getOccurredAt();
            case InventoryHoldReleasedEvent e -> e.getOccurredAt();
            case InventoryValuationChangedEvent e -> e.occurredOn();
            case ABCClassificationChangedEvent e -> e.occurredOn();
            default -> LocalDateTime.now();
        };
    }

    /**
     * Builder for constructing snapshots during event replay
     */
    private static class SnapshotBuilder {
        private final String sku;
        private int quantityOnHand;
        private int quantityAllocated;
        private int quantityReserved;
        private int quantityAvailable;
        private final Map<StockStatus, Integer> stockByStatus;
        private final List<InventoryHoldSnapshot> activeHolds;
        private BigDecimal unitCost;
        private BigDecimal totalValue;
        private String valuationMethod;
        private String abcClass;
        private String abcCriteria;
        private final List<LotBatchSnapshot> lotBatches;
        private final List<String> serialNumbers;

        private SnapshotBuilder(String sku) {
            this.sku = sku;
            this.stockByStatus = new HashMap<>();
            this.activeHolds = new ArrayList<>();
            this.lotBatches = new ArrayList<>();
            this.serialNumbers = new ArrayList<>();
        }

        static SnapshotBuilder fromSnapshot(InventorySnapshot snapshot) {
            SnapshotBuilder builder = new SnapshotBuilder(snapshot.getSku());
            builder.quantityOnHand = snapshot.getQuantityOnHand();
            builder.quantityAllocated = snapshot.getQuantityAllocated();
            builder.quantityReserved = snapshot.getQuantityReserved();
            builder.quantityAvailable = snapshot.getQuantityAvailable();
            builder.stockByStatus.putAll(snapshot.getStockByStatus());
            builder.activeHolds.addAll(snapshot.getActiveHolds());
            builder.unitCost = snapshot.getUnitCost().orElse(null);
            builder.totalValue = snapshot.getTotalValue().orElse(null);
            builder.valuationMethod = snapshot.getValuationMethod().orElse(null);
            builder.abcClass = snapshot.getAbcClass().orElse(null);
            builder.abcCriteria = snapshot.getAbcCriteria().orElse(null);
            builder.lotBatches.addAll(snapshot.getLotBatches());
            builder.serialNumbers.addAll(snapshot.getSerialNumbers());
            return builder;
        }

        void setQuantityOnHand(int qty) { this.quantityOnHand = qty; }
        void setQuantityAllocated(int qty) { this.quantityAllocated = qty; }
        void setQuantityReserved(int qty) { this.quantityReserved = qty; }
        void setQuantityAvailable(int qty) { this.quantityAvailable = qty; }

        void adjustStockByStatus(StockStatus status, int adjustment) {
            int current = stockByStatus.getOrDefault(status, 0);
            int newQty = Math.max(0, current + adjustment);
            if (newQty > 0) {
                stockByStatus.put(status, newQty);
            } else {
                stockByStatus.remove(status);
            }
        }

        void addHold(InventoryHoldSnapshot hold) {
            activeHolds.add(hold);
        }

        void removeHold(String holdId) {
            activeHolds.removeIf(h -> h.getHoldId().equals(holdId));
        }

        void setUnitCost(BigDecimal cost) { this.unitCost = cost; }
        void setTotalValue(BigDecimal value) { this.totalValue = value; }
        void setValuationMethod(String method) { this.valuationMethod = method; }
        void setAbcClass(String abcClass) { this.abcClass = abcClass; }
        void setAbcCriteria(String criteria) { this.abcCriteria = criteria; }

        InventorySnapshot build(LocalDateTime timestamp, SnapshotType type,
                               SnapshotReason reason, String createdBy) {
            return new InventorySnapshot(
                UUID.randomUUID().toString(), timestamp, type, reason, sku,
                quantityOnHand, quantityAllocated, quantityReserved, quantityAvailable,
                stockByStatus, activeHolds, unitCost, totalValue, valuationMethod,
                abcClass, abcCriteria, lotBatches, serialNumbers, createdBy
            );
        }
    }
}
