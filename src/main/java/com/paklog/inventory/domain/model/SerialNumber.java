package com.paklog.inventory.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object representing a unique serial number for individual unit tracking.
 * Critical for high-value items, warranty tracking, and regulatory compliance.
 *
 * Industry pattern: SAP IM Serial Number Management, Oracle Item Instances
 */
public class SerialNumber {

    private final String serialNumber;
    private final String sku;
    private final SerialStatus status;
    private final String lotNumber; // Optional association with lot/batch
    private final Location currentLocation;
    private final LocalDateTime receivedAt;
    private final LocalDate warrantyExpiryDate; // Optional
    private final String manufacturerSerialNumber; // Optional vendor serial number
    private final String currentOwner; // Customer ID if sold, null if in inventory
    private final LocalDate manufactureDate; // Manufacturing date
    private final LocalDateTime allocatedAt; // When allocated to an order

    private SerialNumber(String serialNumber, String sku, SerialStatus status,
                        String lotNumber, Location currentLocation,
                        LocalDateTime receivedAt, LocalDate warrantyExpiryDate,
                        String manufacturerSerialNumber, String currentOwner,
                        LocalDate manufactureDate, LocalDateTime allocatedAt) {
        this.serialNumber = serialNumber;
        this.sku = sku;
        this.status = status;
        this.lotNumber = lotNumber;
        this.currentLocation = currentLocation;
        this.receivedAt = receivedAt;
        this.warrantyExpiryDate = warrantyExpiryDate;
        this.manufacturerSerialNumber = manufacturerSerialNumber;
        this.currentOwner = currentOwner;
        this.manufactureDate = manufactureDate;
        this.allocatedAt = allocatedAt;
        validateInvariants();
    }

    public static SerialNumber create(String serialNumber, String sku, Location receivedLocation) {
        return new SerialNumber(
            serialNumber,
            sku,
            SerialStatus.IN_INVENTORY,
            null,
            receivedLocation,
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            null
        );
    }

    public static SerialNumber create(String serialNumber, String sku, String lotNumber,
                                     Location receivedLocation, LocalDate warrantyExpiryDate) {
        return new SerialNumber(
            serialNumber,
            sku,
            SerialStatus.IN_INVENTORY,
            lotNumber,
            receivedLocation,
            LocalDateTime.now(),
            warrantyExpiryDate,
            null,
            null,
            null,
            null
        );
    }

    public static SerialNumber create(String serialNumber, String sku, Location receivedLocation,
                                     LocalDate manufactureDate, LocalDate warrantyExpiryDate) {
        return new SerialNumber(
            serialNumber,
            sku,
            SerialStatus.IN_INVENTORY,
            null,
            receivedLocation,
            LocalDateTime.now(),
            warrantyExpiryDate,
            null,
            null,
            manufactureDate,
            null
        );
    }

    public static SerialNumber load(String serialNumber, String sku, SerialStatus status,
                                   String lotNumber, Location currentLocation,
                                   LocalDateTime receivedAt, LocalDate warrantyExpiryDate,
                                   String manufacturerSerialNumber, String currentOwner) {
        return new SerialNumber(serialNumber, sku, status, lotNumber, currentLocation,
                               receivedAt, warrantyExpiryDate, manufacturerSerialNumber, currentOwner,
                               null, null);
    }

    /**
     * Mark serial number as allocated to an order
     */
    public SerialNumber allocate() {
        if (!canBeAllocated()) {
            throw new IllegalStateException("Serial number cannot be allocated in status: " + status);
        }
        return new SerialNumber(serialNumber, sku, SerialStatus.ALLOCATED, lotNumber,
                               currentLocation, receivedAt, warrantyExpiryDate,
                               manufacturerSerialNumber, currentOwner,
                               manufactureDate, LocalDateTime.now());
    }

    /**
     * Mark serial number as shipped (without customerId)
     */
    public SerialNumber ship() {
        if (status != SerialStatus.ALLOCATED) {
            throw new IllegalStateException("Can only ship allocated serial numbers");
        }
        return new SerialNumber(serialNumber, sku, SerialStatus.SHIPPED, lotNumber,
                               null, receivedAt, warrantyExpiryDate,
                               manufacturerSerialNumber, null,
                               manufactureDate, allocatedAt);
    }

    /**
     * Mark serial number as shipped to customer
     */
    public SerialNumber ship(String customerId) {
        if (status != SerialStatus.ALLOCATED) {
            throw new IllegalStateException("Can only ship allocated serial numbers");
        }
        return new SerialNumber(serialNumber, sku, SerialStatus.SHIPPED, lotNumber,
                               null, receivedAt, warrantyExpiryDate,
                               manufacturerSerialNumber, customerId,
                               manufactureDate, allocatedAt);
    }

    /**
     * Process return from customer
     */
    public SerialNumber returnFromCustomer(Location returnLocation) {
        if (status != SerialStatus.SHIPPED) {
            throw new IllegalStateException("Can only return shipped serial numbers");
        }
        return new SerialNumber(serialNumber, sku, SerialStatus.RETURNED, lotNumber,
                               returnLocation, receivedAt, warrantyExpiryDate,
                               manufacturerSerialNumber, null,
                               manufactureDate, allocatedAt);
    }

    /**
     * Move serial number to new location (alias)
     */
    public SerialNumber moveTo(Location newLocation) {
        return moveToLocation(newLocation);
    }

    /**
     * Move serial number to new location
     */
    public SerialNumber moveToLocation(Location newLocation) {
        if (status != SerialStatus.IN_INVENTORY) {
            throw new IllegalStateException("Can only move serial numbers in inventory");
        }
        return new SerialNumber(serialNumber, sku, status, lotNumber,
                               newLocation, receivedAt, warrantyExpiryDate,
                               manufacturerSerialNumber, currentOwner,
                               manufactureDate, allocatedAt);
    }

    /**
     * Mark as quarantined for quality inspection
     */
    public SerialNumber quarantine() {
        return new SerialNumber(serialNumber, sku, SerialStatus.QUARANTINE, lotNumber,
                               currentLocation, receivedAt, warrantyExpiryDate,
                               manufacturerSerialNumber, currentOwner,
                               manufactureDate, allocatedAt);
    }

    /**
     * Mark as damaged
     */
    public SerialNumber markDamaged() {
        return new SerialNumber(serialNumber, sku, SerialStatus.DAMAGED, lotNumber,
                               currentLocation, receivedAt, warrantyExpiryDate,
                               manufacturerSerialNumber, currentOwner,
                               manufactureDate, allocatedAt);
    }

    /**
     * Release from quarantine back to available
     */
    public SerialNumber releaseFromQuarantine() {
        if (status != SerialStatus.QUARANTINE) {
            throw new IllegalStateException("Can only release from quarantine status");
        }
        return new SerialNumber(serialNumber, sku, SerialStatus.IN_INVENTORY, lotNumber,
                               currentLocation, receivedAt, warrantyExpiryDate,
                               manufacturerSerialNumber, currentOwner,
                               manufactureDate, allocatedAt);
    }

    /**
     * Change serial number status
     */
    public SerialNumber changeStatus(SerialStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        return new SerialNumber(serialNumber, sku, newStatus, lotNumber,
                               currentLocation, receivedAt, warrantyExpiryDate,
                               manufacturerSerialNumber, currentOwner,
                               manufactureDate, allocatedAt);
    }

    public boolean canBeAllocated() {
        return status == SerialStatus.IN_INVENTORY;
    }

    public boolean isInWarranty() {
        return warrantyExpiryDate != null && LocalDate.now().isBefore(warrantyExpiryDate);
    }

    public boolean isWarrantyValid() {
        return isInWarranty();
    }

    public boolean isAtLocation(Location location) {
        return currentLocation != null && currentLocation.equals(location);
    }

    private void validateInvariants() {
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Serial number cannot be null or empty");
        }
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU cannot be null or empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (receivedAt == null) {
            throw new IllegalArgumentException("ReceivedAt timestamp cannot be null");
        }
        if (warrantyExpiryDate != null && warrantyExpiryDate.isBefore(receivedAt.toLocalDate())) {
            throw new IllegalArgumentException("Warranty expiry cannot be before received date");
        }
    }

    // Getters
    public String getSerialNumber() {
        return serialNumber;
    }

    public String getSku() {
        return sku;
    }

    public SerialStatus getStatus() {
        return status;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public LocalDate getWarrantyExpiryDate() {
        return warrantyExpiryDate;
    }

    public String getManufacturerSerialNumber() {
        return manufacturerSerialNumber;
    }

    public String getCurrentOwner() {
        return currentOwner;
    }

    // Alias for getCustomerId
    public String getCustomerId() {
        return currentOwner;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerialNumber that = (SerialNumber) o;
        return Objects.equals(serialNumber, that.serialNumber) &&
               Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serialNumber, sku);
    }

    @Override
    public String toString() {
        return "SerialNumber{" +
                "serialNumber='" + serialNumber + '\'' +
                ", sku='" + sku + '\'' +
                ", status=" + status +
                ", lotNumber='" + lotNumber + '\'' +
                ", currentLocation=" + currentLocation +
                ", receivedAt=" + receivedAt +
                ", warrantyExpiryDate=" + warrantyExpiryDate +
                ", currentOwner='" + currentOwner + '\'' +
                ", inWarranty=" + isInWarranty() +
                '}';
    }
}
