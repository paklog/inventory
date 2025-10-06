package com.paklog.inventory.domain.event;

import java.time.LocalDateTime;

/**
 * Domain event published when an inventory snapshot is created.
 */
public record InventorySnapshotCreatedEvent(
    String snapshotId,
    String sku,
    LocalDateTime snapshotTimestamp,
    String snapshotType,
    String reason,
    int quantityOnHand,
    int quantityAvailable,
    String createdBy,
    LocalDateTime occurredAt
) {
    public InventorySnapshotCreatedEvent(String snapshotId, String sku,
                                        LocalDateTime snapshotTimestamp, String snapshotType,
                                        String reason, int quantityOnHand, int quantityAvailable,
                                        String createdBy) {
        this(snapshotId, sku, snapshotTimestamp, snapshotType, reason, quantityOnHand,
            quantityAvailable, createdBy, LocalDateTime.now());
    }
}
