package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.SerialNumber;
import com.paklog.inventory.domain.model.SerialStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SerialNumberResponse(
    String serialNumber,
    String sku,
    SerialStatus status,
    String warehouseId,
    String zoneId,
    String customerId,
    LocalDate manufactureDate,
    LocalDate warrantyExpiryDate,
    LocalDateTime receivedAt,
    LocalDateTime allocatedAt,
    boolean warrantyValid
) {
    public static SerialNumberResponse fromDomain(SerialNumber serial) {
        return new SerialNumberResponse(
            serial.getSerialNumber(),
            serial.getSku(),
            serial.getStatus(),
            serial.getCurrentLocation() != null ? serial.getCurrentLocation().getWarehouseId() : null,
            serial.getCurrentLocation() != null ? serial.getCurrentLocation().getZoneId() : null,
            serial.getCustomerId(),
            serial.getManufactureDate(),
            serial.getWarrantyExpiryDate(),
            serial.getReceivedAt(),
            serial.getAllocatedAt(),
            serial.isWarrantyValid()
        );
    }
}
