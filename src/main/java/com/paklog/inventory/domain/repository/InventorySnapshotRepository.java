package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.InventorySnapshot;
import com.paklog.inventory.domain.model.SnapshotType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for InventorySnapshot aggregate.
 */
public interface InventorySnapshotRepository {

    /**
     * Save snapshot
     */
    InventorySnapshot save(InventorySnapshot snapshot);

    /**
     * Find snapshot by ID
     */
    Optional<InventorySnapshot> findById(String snapshotId);

    /**
     * Find nearest snapshot BEFORE target timestamp for a SKU
     * (Critical for hybrid strategy - used as baseline for event replay)
     */
    Optional<InventorySnapshot> findNearestSnapshotBefore(String sku, LocalDateTime targetTimestamp);

    /**
     * Find snapshot at exact timestamp
     */
    Optional<InventorySnapshot> findBySkuAndTimestamp(String sku, LocalDateTime timestamp);

    /**
     * Find all snapshots for a SKU within date range
     */
    List<InventorySnapshot> findBySkuAndTimestampBetween(String sku, LocalDateTime start, LocalDateTime end);

    /**
     * Find snapshots by type (e.g., all MONTH_END snapshots)
     */
    List<InventorySnapshot> findByType(SnapshotType type);

    /**
     * Find snapshots by type for a SKU
     */
    List<InventorySnapshot> findBySkuAndType(String sku, SnapshotType type);

    /**
     * Find all snapshots at a specific timestamp (bulk query)
     */
    List<InventorySnapshot> findAllAtTimestamp(LocalDateTime timestamp);

    /**
     * Find all snapshots of a specific type at timestamp
     */
    List<InventorySnapshot> findAllByTypeAndTimestamp(SnapshotType type, LocalDateTime timestamp);

    /**
     * Delete old snapshots (for cleanup/archival)
     */
    void deleteOlderThan(LocalDateTime cutoffDate);

    /**
     * Delete snapshots by type older than cutoff
     */
    void deleteByTypeOlderThan(SnapshotType type, LocalDateTime cutoffDate);

    /**
     * Count snapshots
     */
    long count();

    /**
     * Count snapshots by type
     */
    long countByType(SnapshotType type);
}
