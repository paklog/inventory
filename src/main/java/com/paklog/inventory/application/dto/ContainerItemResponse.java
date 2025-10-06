package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.ContainerItem;

import java.time.LocalDateTime;

public record ContainerItemResponse(
    String sku,
    int quantity,
    String lotNumber,
    String sourceWarehouseId,
    String sourceZoneId,
    LocalDateTime addedAt
) {
    public static ContainerItemResponse fromDomain(ContainerItem item) {
        return new ContainerItemResponse(
            item.getSku(),
            item.getQuantity(),
            item.getLotNumber(),
            item.getSourceLocation().getWarehouseId(),
            item.getSourceLocation().getZoneId(),
            item.getAddedAt()
        );
    }
}
