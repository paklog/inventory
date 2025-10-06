package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.SerialNumber;
import com.paklog.inventory.domain.model.SerialStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MongoDB document for serial number tracking (separate collection)
 */
@Document(collection = "serial_numbers")
@CompoundIndexes({
    @CompoundIndex(name = "serial_sku_idx", def = "{'serialNumber': 1, 'sku': 1}", unique = true),
    @CompoundIndex(name = "sku_status_idx", def = "{'sku': 1, 'status': 1}"),
    @CompoundIndex(name = "customer_idx", def = "{'customerId': 1}"),
    @CompoundIndex(name = "location_idx", def = "{'currentLocation.zoneId': 1, 'currentLocation.aisle': 1}")
})
public class SerialNumberDocument {

    @Id
    private String id; // MongoDB internal ID
    private String serialNumber; // The actual serial number (barcode/IMEI/etc)
    private String sku;
    private String status; // SerialStatus enum as string
    private LocationDocument currentLocation;
    private String customerId;
    private LocalDate manufactureDate;
    private LocalDate warrantyExpiryDate;
    private LocalDateTime receivedAt;
    private LocalDateTime allocatedAt;

    public SerialNumberDocument() {
    }

    public static SerialNumberDocument fromDomain(SerialNumber serial) {
        SerialNumberDocument doc = new SerialNumberDocument();
        doc.serialNumber = serial.getSerialNumber();
        doc.sku = serial.getSku();
        doc.status = serial.getStatus().name();
        doc.currentLocation = serial.getCurrentLocation() != null ?
                LocationDocument.fromDomain(serial.getCurrentLocation()) : null;
        doc.customerId = serial.getCustomerId();
        doc.manufactureDate = serial.getManufactureDate();
        doc.warrantyExpiryDate = serial.getWarrantyExpiryDate();
        doc.receivedAt = serial.getReceivedAt();
        doc.allocatedAt = serial.getAllocatedAt();
        return doc;
    }

    public SerialNumber toDomain() {
        // Use the load factory method if we have all fields
        if (currentLocation != null) {
            return SerialNumber.load(
                serialNumber,
                sku,
                SerialStatus.valueOf(status),
                null, // lotNumber
                currentLocation.toDomain(),
                receivedAt,
                warrantyExpiryDate,
                null, // manufacturerSerialNumber
                customerId
            );
        } else {
            // Fallback for minimal data
            return SerialNumber.load(
                serialNumber,
                sku,
                SerialStatus.valueOf(status),
                null,
                null,
                receivedAt != null ? receivedAt : LocalDateTime.now(),
                warrantyExpiryDate,
                null,
                customerId
            );
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocationDocument getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(LocationDocument currentLocation) {
        this.currentLocation = currentLocation;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(LocalDate manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public LocalDate getWarrantyExpiryDate() {
        return warrantyExpiryDate;
    }

    public void setWarrantyExpiryDate(LocalDate warrantyExpiryDate) {
        this.warrantyExpiryDate = warrantyExpiryDate;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public void setAllocatedAt(LocalDateTime allocatedAt) {
        this.allocatedAt = allocatedAt;
    }
}
