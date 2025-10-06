package com.paklog.inventory.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object representing allocation of a component SKU for kit assembly.
 */
public class ComponentAllocation {

    private final String componentSku;
    private final int quantity;
    private final Location sourceLocation;
    private final String lotNumber;
    private final LocalDateTime allocatedAt;

    private ComponentAllocation(String componentSku, int quantity, Location sourceLocation,
                               String lotNumber) {
        if (componentSku == null || componentSku.isBlank()) {
            throw new IllegalArgumentException("Component SKU cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (sourceLocation == null) {
            throw new IllegalArgumentException("Source location cannot be null");
        }

        this.componentSku = componentSku;
        this.quantity = quantity;
        this.sourceLocation = sourceLocation;
        this.lotNumber = lotNumber;
        this.allocatedAt = LocalDateTime.now();
    }

    /**
     * Create component allocation
     */
    public static ComponentAllocation of(String componentSku, int quantity,
                                        Location sourceLocation, String lotNumber) {
        return new ComponentAllocation(componentSku, quantity, sourceLocation, lotNumber);
    }

    /**
     * Create component allocation without lot number
     */
    public static ComponentAllocation of(String componentSku, int quantity, Location sourceLocation) {
        return new ComponentAllocation(componentSku, quantity, sourceLocation, null);
    }

    public String getComponentSku() {
        return componentSku;
    }

    public int getQuantity() {
        return quantity;
    }

    public Location getSourceLocation() {
        return sourceLocation;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentAllocation that = (ComponentAllocation) o;
        return quantity == that.quantity &&
               Objects.equals(componentSku, that.componentSku) &&
               Objects.equals(sourceLocation, that.sourceLocation) &&
               Objects.equals(lotNumber, that.lotNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentSku, quantity, sourceLocation, lotNumber);
    }

    @Override
    public String toString() {
        return quantity + "x " + componentSku +
               " from " + sourceLocation.toLocationCode() +
               (lotNumber != null ? " (lot: " + lotNumber + ")" : "");
    }
}
