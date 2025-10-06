package com.paklog.inventory.application.dto;

public record AddContainerItemRequest(
    String sku,
    int quantity,
    String lotNumber,
    String sourceWarehouseId,
    String sourceZoneId,
    String sourceAisleId,
    String sourceRackId,
    String sourceShelfId,
    String sourceBinId
) {}
