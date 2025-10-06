package com.paklog.inventory.application.dto;

public record InitiateTransferRequest(
    String sku,
    String sourceWarehouseId,
    String sourceZoneId,
    String sourceAisleId,
    String sourceRackId,
    String sourceShelfId,
    String sourceBinId,
    String destinationWarehouseId,
    String destinationZoneId,
    String destinationAisleId,
    String destinationRackId,
    String destinationShelfId,
    String destinationBinId,
    int quantity,
    String initiatedBy,
    String reason
) {}
