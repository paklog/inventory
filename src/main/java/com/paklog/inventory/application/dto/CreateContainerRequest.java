package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.ContainerType;

public record CreateContainerRequest(
    ContainerType type,
    String warehouseId,
    String zoneId,
    String aisleId,
    String rackId,
    String shelfId,
    String binId,
    String createdBy
) {}
