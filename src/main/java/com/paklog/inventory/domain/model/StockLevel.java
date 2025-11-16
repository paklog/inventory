package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.exception.InvalidQuantityException;
import com.paklog.inventory.domain.exception.StockLevelInvariantViolationException;
import java.util.Objects;

public class StockLevel {
    private final int quantityOnHand;
    private final int quantityAllocated;
    private final int quantityReserved;

    private StockLevel(int quantityOnHand, int quantityAllocated, int quantityReserved) {
        if (quantityOnHand < 0) {
            throw new InvalidQuantityException("create stock level", quantityOnHand, "Quantity on hand cannot be negative");
        }
        if (quantityAllocated < 0) {
            throw new InvalidQuantityException("create stock level", quantityAllocated, "Quantity allocated cannot be negative");
        }
        if (quantityReserved < 0) {
            throw new InvalidQuantityException("create stock level", quantityReserved, "Quantity reserved cannot be negative");
        }
        if (quantityAllocated > quantityOnHand) {
            throw new StockLevelInvariantViolationException(
                "Allocated quantity cannot exceed quantity on hand",
                quantityOnHand,
                quantityAllocated);
        }

        this.quantityOnHand = quantityOnHand;
        this.quantityAllocated = quantityAllocated;
        this.quantityReserved = quantityReserved;
    }


    public static StockLevel of(int quantityOnHand, int quantityAllocated) {
        return new StockLevel(quantityOnHand, quantityAllocated, 0);
    }

    public static StockLevel of(int quantityOnHand, int quantityAllocated, int quantityReserved) {
        return new StockLevel(quantityOnHand, quantityAllocated, quantityReserved);
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityAllocated() {
        return quantityAllocated;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public int getAvailableToPromise() {
        return quantityOnHand - quantityAllocated;
    }

    public StockLevel withAllocation(int quantity) {
        return new StockLevel(this.quantityOnHand, this.quantityAllocated + quantity, this.quantityReserved);
    }

    public StockLevel withDeallocation(int quantity) {
        return new StockLevel(this.quantityOnHand, this.quantityAllocated - quantity, this.quantityReserved);
    }

    public StockLevel withQuantityChange(int change) {
        return new StockLevel(this.quantityOnHand + change, this.quantityAllocated, this.quantityReserved);
    }

    public StockLevel withReservation(int quantity) {
        return new StockLevel(this.quantityOnHand, this.quantityAllocated, this.quantityReserved + quantity);
    }

    public StockLevel withReservationRelease(int quantity) {
        return new StockLevel(this.quantityOnHand, this.quantityAllocated, this.quantityReserved - quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockLevel that = (StockLevel) o;
        return quantityOnHand == that.quantityOnHand &&
               quantityAllocated == that.quantityAllocated &&
               quantityReserved == that.quantityReserved;
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantityOnHand, quantityAllocated, quantityReserved);
    }

    @Override
    public String toString() {
        return "StockLevel{" +
               "quantityOnHand=" + quantityOnHand +
               ", quantityAllocated=" + quantityAllocated +
               ", quantityReserved=" + quantityReserved +
               '}';
    }
}
