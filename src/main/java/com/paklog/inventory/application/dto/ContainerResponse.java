package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.Container;
import com.paklog.inventory.domain.model.ContainerStatus;
import com.paklog.inventory.domain.model.ContainerType;

import java.time.LocalDateTime;
import java.util.List;

public record ContainerResponse(
    String lpn,
    ContainerType type,
    ContainerStatus status,
    String warehouseId,
    String zoneId,
    List<ContainerItemResponse> items,
    int totalQuantity,
    String parentLpn,
    boolean atCapacity,
    boolean mixedSKU,
    LocalDateTime createdAt,
    LocalDateTime closedAt,
    LocalDateTime shippedAt
) {
    public static ContainerResponse fromDomain(Container container) {
        return new ContainerResponse(
            container.getLpn(),
            container.getType(),
            container.getStatus(),
            container.getCurrentLocation().getWarehouseId(),
            container.getCurrentLocation().getZoneId(),
            container.getItems().stream()
                .map(ContainerItemResponse::fromDomain)
                .toList(),
            container.getTotalQuantity(),
            container.getParentLpn(),
            container.isAtCapacity(),
            container.isMixedSKU(),
            container.getCreatedAt(),
            container.getClosedAt(),
            container.getShippedAt()
        );
    }
}
