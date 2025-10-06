package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.CreateSnapshotRequest;
import com.paklog.inventory.application.dto.SnapshotResponse;
import com.paklog.inventory.application.service.SnapshotService;
import com.paklog.inventory.domain.model.InventorySnapshot;
import com.paklog.inventory.domain.model.SnapshotReason;
import com.paklog.inventory.domain.model.SnapshotType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for inventory snapshots and time-travel queries.
 * Provides historical inventory state access and reconciliation capabilities.
 */
@RestController
@RequestMapping("/api/v1/inventory/snapshots")
public class SnapshotController {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotController.class);

    private final SnapshotService snapshotService;

    public SnapshotController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    /**
     * Create snapshot for a SKU
     * POST /api/v1/inventory/snapshots
     */
    @PostMapping
    public ResponseEntity<SnapshotResponse> createSnapshot(@RequestBody CreateSnapshotRequest request) {
        logger.info("Creating snapshot: SKU={}, type={}, reason={}",
                request.sku(), request.snapshotType(), request.reason());

        InventorySnapshot snapshot = snapshotService.createSnapshot(
            request.sku(),
            SnapshotType.valueOf(request.snapshotType()),
            SnapshotReason.valueOf(request.reason()),
            request.createdBy()
        );

        return ResponseEntity.ok(SnapshotResponse.fromDomain(snapshot));
    }

    /**
     * Get snapshot at specific point in time (TIME-TRAVEL QUERY)
     * GET /api/v1/inventory/snapshots/{sku}/at
     */
    @GetMapping("/{sku}/at")
    public ResponseEntity<SnapshotResponse> getSnapshotAt(
            @PathVariable String sku,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp) {
        logger.info("Time-travel query: SKU={}, timestamp={}", sku, timestamp);

        Optional<InventorySnapshot> snapshot = snapshotService.getSnapshotAt(sku, timestamp);

        return snapshot
            .map(s -> ResponseEntity.ok(SnapshotResponse.fromDomain(s)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all snapshots at specific time (bulk query)
     * GET /api/v1/inventory/snapshots/all/at
     */
    @GetMapping("/all/at")
    public ResponseEntity<List<SnapshotResponse>> getAllSnapshotsAt(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp) {
        logger.info("Bulk time-travel query: timestamp={}", timestamp);

        List<SnapshotResponse> snapshots = snapshotService.getAllSnapshotsAt(timestamp).stream()
            .map(SnapshotResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(snapshots);
    }

    /**
     * Get month-end snapshots for a SKU
     * GET /api/v1/inventory/snapshots/{sku}/month-end
     */
    @GetMapping("/{sku}/month-end")
    public ResponseEntity<List<SnapshotResponse>> getMonthEndSnapshots(
            @PathVariable String sku,
            @RequestParam int year) {
        logger.debug("Getting month-end snapshots: SKU={}, year={}", sku, year);

        List<SnapshotResponse> snapshots = snapshotService.getMonthEndSnapshots(sku, year).stream()
            .map(SnapshotResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(snapshots);
    }

    /**
     * Get year-end snapshots for a SKU
     * GET /api/v1/inventory/snapshots/{sku}/year-end
     */
    @GetMapping("/{sku}/year-end")
    public ResponseEntity<List<SnapshotResponse>> getYearEndSnapshots(@PathVariable String sku) {
        logger.debug("Getting year-end snapshots: SKU={}", sku);

        List<SnapshotResponse> snapshots = snapshotService.getYearEndSnapshots(sku).stream()
            .map(SnapshotResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(snapshots);
    }

    /**
     * Get snapshot delta (difference between two points in time)
     * GET /api/v1/inventory/snapshots/{sku}/delta
     */
    @GetMapping("/{sku}/delta")
    public ResponseEntity<SnapshotService.SnapshotDelta> getDelta(
            @PathVariable String sku,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime) {
        logger.debug("Getting snapshot delta: SKU={}, from={}, to={}", sku, fromTime, toTime);

        SnapshotService.SnapshotDelta delta = snapshotService.getDelta(sku, fromTime, toTime);
        return ResponseEntity.ok(delta);
    }

    /**
     * Get snapshot statistics
     * GET /api/v1/inventory/snapshots/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<SnapshotService.SnapshotStatistics> getStatistics() {
        logger.debug("Getting snapshot statistics");

        SnapshotService.SnapshotStatistics stats = snapshotService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Trigger cleanup of old snapshots (admin endpoint)
     * POST /api/v1/inventory/snapshots/cleanup
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Void> cleanupOldSnapshots() {
        logger.info("Triggering manual snapshot cleanup");

        snapshotService.cleanupOldSnapshots();
        return ResponseEntity.ok().build();
    }
}
