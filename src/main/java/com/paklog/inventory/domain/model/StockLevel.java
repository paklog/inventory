package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.exception.InvalidQuantityException;
import com.paklog.inventory.domain.exception.StockLevelInvariantViolationException;
import java.util.Objects;

public class StockLevel {
    private final int quantityOnHand;
    private final int quantityAllocated;

    private StockLevel(int quantityOnHand, int quantityAllocated) {
        if (quantityOnHand < 0) {
            throw new InvalidQuantityException("create stock level", quantityOnHand, "Quantity on hand cannot be negative");
        }
        if (quantityAllocated < 0) {
            throw new InvalidQuantityException("create stock level", quantityAllocated, "Quantity allocated cannot be negative");
        }
        if (quantityAllocated > quantityOnHand) {
            throw new StockLevelInvariantViolationException(
                "Allocated quantity cannot exceed quantity on hand", 
                quantityOnHand, 
                quantityAllocated);
        }
        this.quantityOnHand = quantityOnHand;
        this.quantityAllocated = quantityAllocated;
    }

    public static StockLevel of(int quantityOnHand, int quantityAllocated) {
        return new StockLevel(quantityOnHand, quantityAllocated);
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityAllocated() {
        return quantityAllocated;
    }

    public int getAvailableToPromise() {
        return quantityOnHand - quantityAllocated;
    }

    public StockLevel withAllocation(int quantity) {
        return new StockLevel(this.quantityOnHand, this.quantityAllocated + quantity);
    }

    public StockLevel withDeallocation(int quantity) {
        return new StockLevel(this.quantityOnHand, this.quantityAllocated - quantity);
    }

    public StockLevel withQuantityChange(int change) {
        return new StockLevel(this.quantityOnHand + change, this.quantityAllocated);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockLevel that = (StockLevel) o;
        return quantityOnHand == that.quantityOnHand &&
               quantityAllocated == that.quantityAllocated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantityOnHand, quantityAllocated);
    }

    @Override
    public String toString() {
        return "StockLevel{" +
               "quantityOnHand=" + quantityOnHand +
               ", quantityAllocated=" + quantityAllocated +
               '}';
    }
}