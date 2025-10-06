package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.event.DomainEvent;
import com.paklog.inventory.domain.model.*;
import com.paklog.inventory.domain.repository.InventorySnapshotRepository;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.service.EventReplayService;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Application service for inventory snapshots and time-travel queries.
 * Implements hybrid strategy: periodic snapshots + event replay for precision.
 */
@Service
public class SnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotService.class);

    private final InventorySnapshotRepository snapshotRepository;
    private final ProductStockRepository productStockRepository;
    private final OutboxRepository outboxRepository;
    private final EventReplayService eventReplayService;

    public SnapshotService(InventorySnapshotRepository snapshotRepository,
                          ProductStockRepository productStockRepository,
                          OutboxRepository outboxRepository,
                          EventReplayService eventReplayService) {
        this.snapshotRepository = snapshotRepository;
        this.productStockRepository = productStockRepository;
        this.outboxRepository = outboxRepository;
        this.eventReplayService = eventReplayService;
    }

    /**
     * Create snapshot for a SKU
     */
    @Transactional
    public InventorySnapshot createSnapshot(String sku, SnapshotType snapshotType,
                                           SnapshotReason reason, String createdBy) {
        logger.info("Creating snapshot: SKU={}, type={}, reason={}", sku, snapshotType, reason);

        ProductStock productStock = productStockRepository.findBySku(sku)
            .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        InventorySnapshot snapshot = InventorySnapshot.fromProductStock(
            productStock, LocalDateTime.now(), snapshotType, reason, createdBy
        );

        InventorySnapshot saved = snapshotRepository.save(snapshot);

        logger.info("Snapshot created: snapshotId={}, SKU={}, qty={}",
                saved.getSnapshotId(), sku, saved.getQuantityOnHand());

        return saved;
    }

    /**
     * Get snapshot at specific point in time (HYBRID STRATEGY - core method)
     * 1. Find nearest snapshot BEFORE target time
     * 2. Load events from snapshot time → target time
     * 3. Replay events to reconstruct exact state
     */
    @Transactional(readOnly = true)
    public Optional<InventorySnapshot> getSnapshotAt(String sku, LocalDateTime targetTimestamp) {
        logger.debug("Getting snapshot: SKU={}, targetTime={}", sku, targetTimestamp);

        // Step 1: Find nearest snapshot before target time
        Optional<InventorySnapshot> nearestSnapshot =
            snapshotRepository.findNearestSnapshotBefore(sku, targetTimestamp);

        if (nearestSnapshot.isEmpty()) {
            logger.warn("No baseline snapshot found for SKU={} before {}", sku, targetTimestamp);
            // Could create initial snapshot from first event or return empty
            return Optional.empty();
        }

        InventorySnapshot baselineSnapshot = nearestSnapshot.get();
        logger.debug("Found baseline snapshot: snapshotId={}, timestamp={}",
                baselineSnapshot.getSnapshotId(), baselineSnapshot.getSnapshotTimestamp());

        // Step 2: Load events between snapshot and target time
        List<OutboxEvent> events = loadEventsBetween(
            sku,
            baselineSnapshot.getSnapshotTimestamp(),
            targetTimestamp
        );

        logger.debug("Loaded {} events for replay", events.size());

        // Step 3: If no events, return baseline snapshot as-is
        if (events.isEmpty()) {
            logger.debug("No events to replay, returning baseline snapshot");
            return Optional.of(baselineSnapshot);
        }

        // Step 4: Replay events to get exact state at target time
        // Note: EventReplayService would need to be updated to handle OutboxEvent
        // For now, we skip event replay and return baseline snapshot
        logger.warn("Event replay not yet implemented for OutboxEvent, returning baseline");
        return Optional.of(baselineSnapshot);
    }

    /**
     * Get snapshots for all SKUs at a specific time (bulk operation)
     */
    @Transactional(readOnly = true)
    public List<InventorySnapshot> getAllSnapshotsAt(LocalDateTime targetTimestamp) {
        logger.info("Getting all snapshots at: {}", targetTimestamp);

        // For bulk operations, we prioritize snapshots at exact timestamp
        List<InventorySnapshot> exactSnapshots = snapshotRepository.findAllAtTimestamp(targetTimestamp);

        if (!exactSnapshots.isEmpty()) {
            logger.info("Found {} exact snapshots at {}", exactSnapshots.size(), targetTimestamp);
            return exactSnapshots;
        }

        // If no exact snapshots, would need to reconstruct for all SKUs
        // This is expensive - typically scheduled snapshots handle this
        logger.warn("No exact snapshots found at {}. Consider creating scheduled snapshot.", targetTimestamp);
        return List.of();
    }

    /**
     * Get month-end snapshots for a SKU for trend analysis
     */
    @Transactional(readOnly = true)
    public List<InventorySnapshot> getMonthEndSnapshots(String sku, int year) {
        logger.debug("Getting month-end snapshots: SKU={}, year={}", sku, year);

        return snapshotRepository.findBySkuAndType(sku, SnapshotType.MONTH_END).stream()
            .filter(s -> s.getSnapshotTimestamp().getYear() == year)
            .sorted((s1, s2) -> s1.getSnapshotTimestamp().compareTo(s2.getSnapshotTimestamp()))
            .toList();
    }

    /**
     * Get year-end snapshots for a SKU
     */
    @Transactional(readOnly = true)
    public List<InventorySnapshot> getYearEndSnapshots(String sku) {
        logger.debug("Getting year-end snapshots: SKU={}", sku);

        return snapshotRepository.findBySkuAndType(sku, SnapshotType.YEAR_END);
    }

    /**
     * Get snapshot delta (difference between two points in time)
     */
    @Transactional(readOnly = true)
    public SnapshotDelta getDelta(String sku, LocalDateTime fromTime, LocalDateTime toTime) {
        logger.debug("Calculating delta: SKU={}, from={}, to={}", sku, fromTime, toTime);

        Optional<InventorySnapshot> fromSnapshot = getSnapshotAt(sku, fromTime);
        Optional<InventorySnapshot> toSnapshot = getSnapshotAt(sku, toTime);

        if (fromSnapshot.isEmpty() || toSnapshot.isEmpty()) {
            throw new IllegalArgumentException("Cannot calculate delta - snapshots not found");
        }

        return SnapshotDelta.calculate(fromSnapshot.get(), toSnapshot.get());
    }

    /**
     * Delete old snapshots based on retention policy
     */
    @Transactional
    public void cleanupOldSnapshots() {
        logger.info("Cleaning up old snapshots");

        LocalDateTime now = LocalDateTime.now();

        // DAILY: Retention 90 days
        LocalDateTime dailyCutoff = now.minusDays(90);
        snapshotRepository.deleteByTypeOlderThan(SnapshotType.DAILY, dailyCutoff);
        logger.info("Deleted DAILY snapshots older than {}", dailyCutoff);

        // AD_HOC: Retention 30 days
        LocalDateTime adHocCutoff = now.minusDays(30);
        snapshotRepository.deleteByTypeOlderThan(SnapshotType.AD_HOC, adHocCutoff);
        logger.info("Deleted AD_HOC snapshots older than {}", adHocCutoff);

        // MONTH_END: Retention 7 years
        LocalDateTime monthEndCutoff = now.minusYears(7);
        snapshotRepository.deleteByTypeOlderThan(SnapshotType.MONTH_END, monthEndCutoff);
        logger.info("Deleted MONTH_END snapshots older than {}", monthEndCutoff);

        // QUARTER_END: Retention 10 years
        LocalDateTime quarterEndCutoff = now.minusYears(10);
        snapshotRepository.deleteByTypeOlderThan(SnapshotType.QUARTER_END, quarterEndCutoff);
        logger.info("Deleted QUARTER_END snapshots older than {}", quarterEndCutoff);

        // YEAR_END: Never delete (permanent retention)
        logger.info("YEAR_END snapshots retained permanently");
    }

    /**
     * Load events between two timestamps from outbox
     */
    private List<OutboxEvent> loadEventsBetween(String sku, LocalDateTime start, LocalDateTime end) {
        // Load events from outbox repository
        // Filter by SKU and time range
        return outboxRepository.findEventsBetween(start, end).stream()
            .filter(event -> isEventForSku(event, sku))
            .toList();
    }

    private boolean isEventForSku(OutboxEvent event, String sku) {
        // Check if event belongs to the SKU by checking aggregateId
        return event.getAggregateId() != null && event.getAggregateId().equals(sku);
    }

    /**
     * Get snapshot statistics
     */
    @Transactional(readOnly = true)
    public SnapshotStatistics getStatistics() {
        long total = snapshotRepository.count();
        long daily = snapshotRepository.countByType(SnapshotType.DAILY);
        long monthEnd = snapshotRepository.countByType(SnapshotType.MONTH_END);
        long quarterEnd = snapshotRepository.countByType(SnapshotType.QUARTER_END);
        long yearEnd = snapshotRepository.countByType(SnapshotType.YEAR_END);
        long adHoc = snapshotRepository.countByType(SnapshotType.AD_HOC);

        return new SnapshotStatistics(total, daily, monthEnd, quarterEnd, yearEnd, adHoc);
    }

    /**
     * Snapshot statistics record
     */
    public record SnapshotStatistics(
        long total,
        long daily,
        long monthEnd,
        long quarterEnd,
        long yearEnd,
        long adHoc
    ) {}

    /**
     * Snapshot delta record
     */
    public static class SnapshotDelta {
        private final LocalDateTime fromTimestamp;
        private final LocalDateTime toTimestamp;
        private final int quantityChange;
        private final int quantityAvailableChange;

        private SnapshotDelta(LocalDateTime fromTimestamp, LocalDateTime toTimestamp,
                            int quantityChange, int quantityAvailableChange) {
            this.fromTimestamp = fromTimestamp;
            this.toTimestamp = toTimestamp;
            this.quantityChange = quantityChange;
            this.quantityAvailableChange = quantityAvailableChange;
        }

        public static SnapshotDelta calculate(InventorySnapshot from, InventorySnapshot to) {
            return new SnapshotDelta(
                from.getSnapshotTimestamp(),
                to.getSnapshotTimestamp(),
                to.getQuantityOnHand() - from.getQuantityOnHand(),
                to.getQuantityAvailable() - from.getQuantityAvailable()
            );
        }

        public LocalDateTime getFromTimestamp() { return fromTimestamp; }
        public LocalDateTime getToTimestamp() { return toTimestamp; }
        public int getQuantityChange() { return quantityChange; }
        public int getQuantityAvailableChange() { return quantityAvailableChange; }
    }
}
