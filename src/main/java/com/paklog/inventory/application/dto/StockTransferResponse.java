package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.StockTransfer;
import com.paklog.inventory.domain.model.TransferStatus;

import java.time.LocalDateTime;

public record StockTransferResponse(
    String transferId,
    String sku,
    String sourceWarehouseId,
    String sourceZoneId,
    String destinationWarehouseId,
    String destinationZoneId,
    int quantityToTransfer,
    int actualQuantityReceived,
    TransferStatus status,
    String initiatedBy,
    String reason,
    LocalDateTime initiatedAt,
    LocalDateTime inTransitAt,
    LocalDateTime completedAt,
    boolean hasShrinkage,
    int shrinkageQuantity
) {
    public static StockTransferResponse fromDomain(StockTransfer transfer) {
        return new StockTransferResponse(
            transfer.getTransferId(),
            transfer.getSku(),
            transfer.getSourceLocation().getWarehouseId(),
            transfer.getSourceLocation().getZoneId(),
            transfer.getDestinationLocation().getWarehouseId(),
            transfer.getDestinationLocation().getZoneId(),
            transfer.getQuantityToTransfer(),
            transfer.getActualQuantityReceived(),
            transfer.getStatus(),
            transfer.getInitiatedBy(),
            transfer.getReason(),
            transfer.getInitiatedAt(),
            transfer.getInTransitAt(),
            transfer.getCompletedAt(),
            transfer.hasShrinkage(),
            transfer.getShrinkageQuantity()
        );
    }
}
