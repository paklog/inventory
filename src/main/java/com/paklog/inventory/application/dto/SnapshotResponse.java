package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.InventorySnapshot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record SnapshotResponse(
    String snapshotId,
    LocalDateTime snapshotTimestamp,
    String snapshotType,
    String reason,
    String sku,
    int quantityOnHand,
    int quantityAllocated,
    int quantityReserved,
    int quantityAvailable,
    Map<String, Integer> stockByStatus,
    int activeHoldsCount,
    BigDecimal unitCost,
    BigDecimal totalValue,
    String valuationMethod,
    String abcClass,
    int lotBatchCount,
    int serialNumberCount,
    String createdBy,
    LocalDateTime createdAt
) {
    public static SnapshotResponse fromDomain(InventorySnapshot snapshot) {
        Map<String, Integer> stockByStatus = snapshot.getStockByStatus().entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                e -> e.getKey().name(),
                Map.Entry::getValue
            ));

        return new SnapshotResponse(
            snapshot.getSnapshotId(),
            snapshot.getSnapshotTimestamp(),
            snapshot.getSnapshotType().name(),
            snapshot.getReason().name(),
            snapshot.getSku(),
            snapshot.getQuantityOnHand(),
            snapshot.getQuantityAllocated(),
            snapshot.getQuantityReserved(),
            snapshot.getQuantityAvailable(),
            stockByStatus,
            snapshot.getActiveHolds().size(),
            snapshot.getUnitCost().orElse(null),
            snapshot.getTotalValue().orElse(null),
            snapshot.getValuationMethod().orElse(null),
            snapshot.getAbcClass().orElse(null),
            snapshot.getLotBatches().size(),
            snapshot.getSerialNumbers().size(),
            snapshot.getCreatedBy(),
            snapshot.getCreatedAt()
        );
    }
}
