package com.paklog.inventory.application.dto;

import java.time.LocalDate;

public record ReceiveSerialNumberRequest(
    String sku,
    String serialNumber,
    String warehouseId,
    String zoneId,
    String aisleId,
    String rackId,
    String shelfId,
    String binId,
    LocalDate manufactureDate,
    LocalDate warrantyExpiryDate
) {}
