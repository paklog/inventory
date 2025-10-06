package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.exception.InsufficientStockException;
import com.paklog.inventory.domain.exception.InvalidQuantityException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Value object representing a lot/batch of inventory with expiry tracking.
 * Used for FEFO (First Expired First Out) picking strategies and lot traceability.
 */
public class LotBatch {

    private final String lotNumber;
    private final LocalDate manufactureDate;
    private final LocalDate expiryDate;
    private final String supplierId;
    private final BatchStatus status;
    private int quantityOnHand;
    private int quantityAllocated;

    private LotBatch(String lotNumber, LocalDate manufactureDate, LocalDate expiryDate,
                     String supplierId, BatchStatus status, int quantityOnHand, int quantityAllocated) {
        this.lotNumber = lotNumber;
        this.manufactureDate = manufactureDate;
        this.expiryDate = expiryDate;
        this.supplierId = supplierId;
        this.status = status;
        this.quantityOnHand = quantityOnHand;
        this.quantityAllocated = quantityAllocated;
        validateInvariants();
    }

    public static LotBatch create(String lotNumber, LocalDate manufactureDate, LocalDate expiryDate,
                                  String supplierId, int initialQuantity) {
        if (lotNumber == null || lotNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Lot number cannot be null or empty");
        }
        if (manufactureDate == null) {
            throw new IllegalArgumentException("Manufacture date cannot be null");
        }
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date cannot be null");
        }
        if (expiryDate.isBefore(manufactureDate)) {
            throw new IllegalArgumentException("Expiry date cannot be before manufacture date");
        }
        if (initialQuantity < 0) {
            throw new InvalidQuantityException("create", initialQuantity, "Initial quantity cannot be negative");
        }

        return new LotBatch(lotNumber, manufactureDate, expiryDate, supplierId,
                           BatchStatus.AVAILABLE, initialQuantity, 0);
    }

    public static LotBatch load(String lotNumber, LocalDate manufactureDate, LocalDate expiryDate,
                                String supplierId, BatchStatus status, int quantityOnHand, int quantityAllocated) {
        return new LotBatch(lotNumber, manufactureDate, expiryDate, supplierId,
                           status, quantityOnHand, quantityAllocated);
    }

    public void allocate(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("allocate", quantity, "Allocation quantity must be positive");
        }
        if (status != BatchStatus.AVAILABLE) {
            throw new IllegalStateException("Cannot allocate from batch with status: " + status);
        }
        if (isExpired()) {
            throw new IllegalStateException("Cannot allocate from expired batch: " + lotNumber);
        }
        if (getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(lotNumber, quantity, getAvailableQuantity());
        }

        this.quantityAllocated += quantity;
        validateInvariants();
    }

    public void deallocate(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("deallocate", quantity, "Deallocation quantity must be positive");
        }
        if (quantityAllocated < quantity) {
            throw new InsufficientStockException(lotNumber, quantity, quantityAllocated);
        }

        this.quantityAllocated -= quantity;
        validateInvariants();
    }

    public void pick(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("pick", quantity, "Pick quantity must be positive");
        }
        if (quantityAllocated < quantity) {
            throw new IllegalStateException("Cannot pick more than allocated quantity");
        }
        if (quantityOnHand < quantity) {
            throw new InsufficientStockException(lotNumber, quantity, quantityOnHand);
        }

        this.quantityOnHand -= quantity;
        this.quantityAllocated -= quantity;
        validateInvariants();
    }

    public void adjustQuantity(int quantityChange) {
        if (quantityOnHand + quantityChange < 0) {
            throw new InvalidQuantityException("adjust", quantityChange,
                String.format("Would result in negative quantity: %d + %d < 0", quantityOnHand, quantityChange));
        }

        this.quantityOnHand += quantityChange;
        validateInvariants();
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public boolean isNearExpiry(int daysThreshold) {
        return getDaysUntilExpiry() <= daysThreshold && getDaysUntilExpiry() >= 0;
    }

    public int getDaysUntilExpiry() {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public int getAvailableQuantity() {
        return quantityOnHand - quantityAllocated;
    }

    public boolean canBePicked() {
        return status == BatchStatus.AVAILABLE &&
               !isExpired() &&
               getAvailableQuantity() > 0;
    }

    private void validateInvariants() {
        if (quantityOnHand < 0) {
            throw new IllegalStateException("Quantity on hand cannot be negative");
        }
        if (quantityAllocated < 0) {
            throw new IllegalStateException("Quantity allocated cannot be negative");
        }
        if (quantityAllocated > quantityOnHand) {
            throw new IllegalStateException("Allocated quantity cannot exceed quantity on hand");
        }
    }

    // Getters
    public String getLotNumber() {
        return lotNumber;
    }

    // Alias for getLotNumber
    public String getBatchNumber() {
        return lotNumber;
    }

    // Alias for getExpiryDate
    public LocalDate getExpirationDate() {
        return expiryDate;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityAllocated() {
        return quantityAllocated;
    }

    // Alias for getQuantityOnHand
    public int getQuantity() {
        return quantityOnHand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LotBatch lotBatch = (LotBatch) o;
        return Objects.equals(lotNumber, lotBatch.lotNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lotNumber);
    }

    @Override
    public String toString() {
        return "LotBatch{" +
                "lotNumber='" + lotNumber + '\'' +
                ", manufactureDate=" + manufactureDate +
                ", expiryDate=" + expiryDate +
                ", supplierId='" + supplierId + '\'' +
                ", status=" + status +
                ", quantityOnHand=" + quantityOnHand +
                ", quantityAllocated=" + quantityAllocated +
                ", daysUntilExpiry=" + getDaysUntilExpiry() +
                ", availableQuantity=" + getAvailableQuantity() +
                '}';
    }
}
