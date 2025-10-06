package com.paklog.inventory.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for Inventory Snapshots.
 */
@Repository
public interface InventorySnapshotSpringRepository extends MongoRepository<InventorySnapshotDocument, String> {

    Optional<InventorySnapshotDocument> findBySnapshotId(String snapshotId);

    // Find nearest snapshot before target timestamp (critical for hybrid strategy)
    @Query("{ 'sku': ?0, 'snapshotTimestamp': { '$lt': ?1 } }")
    List<InventorySnapshotDocument> findBySkuAndSnapshotTimestampBefore(String sku, LocalDateTime targetTimestamp);

    Optional<InventorySnapshotDocument> findBySkuAndSnapshotTimestamp(String sku, LocalDateTime timestamp);

    List<InventorySnapshotDocument> findBySkuAndSnapshotTimestampBetween(String sku, LocalDateTime start, LocalDateTime end);

    List<InventorySnapshotDocument> findBySnapshotType(String snapshotType);

    List<InventorySnapshotDocument> findBySkuAndSnapshotType(String sku, String snapshotType);

    List<InventorySnapshotDocument> findBySnapshotTimestamp(LocalDateTime timestamp);

    List<InventorySnapshotDocument> findBySnapshotTypeAndSnapshotTimestamp(String snapshotType, LocalDateTime timestamp);

    void deleteBySnapshotTimestampBefore(LocalDateTime cutoffDate);

    void deleteBySnapshotTypeAndSnapshotTimestampBefore(String snapshotType, LocalDateTime cutoffDate);

    long countBySnapshotType(String snapshotType);
}
