package com.paklog.inventory.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing an item inside a container.
 * Enables mixed-SKU containers and lot tracking at container level.
 */
public class ContainerItem {

    private final String sku;
    private final int quantity;
    private final String lotNumber;
    private final Location sourceLocation; // Where item was picked from
    private final LocalDateTime addedAt;

    private ContainerItem(String sku, int quantity, String lotNumber, Location sourceLocation, LocalDateTime addedAt) {
        this.sku = sku;
        this.quantity = quantity;
        this.lotNumber = lotNumber;
        this.sourceLocation = sourceLocation;
        this.addedAt = addedAt;
        validateInvariants();
    }

    /**
     * Factory method to create a container item
     */
    public static ContainerItem create(String sku, int quantity, String lotNumber, Location sourceLocation) {
        return new ContainerItem(sku, quantity, lotNumber, sourceLocation, LocalDateTime.now());
    }

    /**
     * Factory method without lot tracking
     */
    public static ContainerItem create(String sku, int quantity, Location sourceLocation) {
        return new ContainerItem(sku, quantity, null, sourceLocation, LocalDateTime.now());
    }

    /**
     * Create a new instance with adjusted quantity
     */
    public ContainerItem withQuantityChange(int change) {
        return new ContainerItem(this.sku, this.quantity + change, this.lotNumber, this.sourceLocation, this.addedAt);
    }

    private void validateInvariants() {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (sourceLocation == null) {
            throw new IllegalArgumentException("Source location cannot be null");
        }
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public Location getSourceLocation() {
        return sourceLocation;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public boolean isLotTracked() {
        return lotNumber != null && !lotNumber.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContainerItem that = (ContainerItem) o;
        return Objects.equals(sku, that.sku) &&
               Objects.equals(lotNumber, that.lotNumber) &&
               Objects.equals(sourceLocation, that.sourceLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, lotNumber, sourceLocation);
    }

    @Override
    public String toString() {
        return String.format("ContainerItem{sku='%s', qty=%d, lot='%s', from=%s}",
                sku, quantity, lotNumber, sourceLocation.toLocationCode());
    }
}
