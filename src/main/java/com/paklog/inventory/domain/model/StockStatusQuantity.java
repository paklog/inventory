package com.paklog.inventory.domain.model;

import java.util.*;

/**
 * Value object representing inventory quantities segmented by stock status.
 * Enables proper ATP calculation and inventory visibility by quality status.
 *
 * Industry pattern: SAP IM Stock Type, Oracle Subinventory Quantities
 */
public class StockStatusQuantity {

    private final Map<StockStatus, Integer> quantitiesByStatus;

    private StockStatusQuantity(Map<StockStatus, Integer> quantitiesByStatus) {
        this.quantitiesByStatus = new HashMap<>(quantitiesByStatus);
        validateInvariants();
    }

    public static StockStatusQuantity empty() {
        return new StockStatusQuantity(new HashMap<>());
    }

    public static StockStatusQuantity of(StockStatus status, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + quantity);
        }
        Map<StockStatus, Integer> quantities = new HashMap<>();
        quantities.put(status, quantity);
        return new StockStatusQuantity(quantities);
    }

    public static StockStatusQuantity fromMap(Map<StockStatus, Integer> quantities) {
        return new StockStatusQuantity(quantities);
    }

    /**
     * Get quantity for specific status
     */
    public int getQuantityByStatus(StockStatus status) {
        return quantitiesByStatus.getOrDefault(status, 0);
    }

    /**
     * Get total quantity across all statuses
     */
    public int getTotalQuantity() {
        return quantitiesByStatus.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Alias for getTotalQuantity()
     */
    public int getQuantity() {
        return getTotalQuantity();
    }

    /**
     * Get total available quantity (only AVAILABLE status)
     */
    public int getAvailableQuantity() {
        return quantitiesByStatus.entrySet().stream()
                .filter(entry -> entry.getKey().isAvailableToPromise())
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    /**
     * Get total on-hand quantity (excludes IN_TRANSIT)
     */
    public int getQuantityOnHand() {
        return quantitiesByStatus.entrySet().stream()
                .filter(entry -> entry.getKey().countsAsOnHand())
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    /**
     * Get total sellable quantity (AVAILABLE + ALLOCATED)
     */
    public int getSellableQuantity() {
        return quantitiesByStatus.entrySet().stream()
                .filter(entry -> entry.getKey().isSellable())
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    /**
     * Add quantity to a specific status
     */
    public StockStatusQuantity addQuantity(StockStatus status, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity to add must be non-negative: " + quantity);
        }
        Map<StockStatus, Integer> newQuantities = new HashMap<>(this.quantitiesByStatus);
        newQuantities.merge(status, quantity, Integer::sum);
        return new StockStatusQuantity(newQuantities);
    }

    /**
     * Remove quantity from a specific status
     */
    public StockStatusQuantity removeQuantity(StockStatus status, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity to remove must be non-negative: " + quantity);
        }
        int currentQuantity = getQuantityByStatus(status);
        if (currentQuantity < quantity) {
            throw new IllegalStateException(
                String.format("Insufficient quantity in status %s. Current: %d, Requested: %d",
                    status, currentQuantity, quantity));
        }
        Map<StockStatus, Integer> newQuantities = new HashMap<>(this.quantitiesByStatus);
        int newQuantity = currentQuantity - quantity;
        if (newQuantity == 0) {
            newQuantities.remove(status);
        } else {
            newQuantities.put(status, newQuantity);
        }
        return new StockStatusQuantity(newQuantities);
    }

    /**
     * Move quantity from one status to another (e.g., QUARANTINE → AVAILABLE)
     */
    public StockStatusQuantity moveQuantity(StockStatus fromStatus, StockStatus toStatus, int quantity) {
        return this.removeQuantity(fromStatus, quantity).addQuantity(toStatus, quantity);
    }

    /**
     * Change quantity by a delta (can be positive or negative)
     * This works for the AVAILABLE status or any single status in this quantity object
     */
    public StockStatusQuantity withQuantityChange(int quantityChange) {
        if (quantitiesByStatus.isEmpty()) {
            // If empty, add to AVAILABLE status
            if (quantityChange < 0) {
                throw new IllegalStateException("Cannot decrease quantity on empty status quantity");
            }
            return addQuantity(StockStatus.AVAILABLE, quantityChange);
        }

        // If there's only one status, apply change to that status
        if (quantitiesByStatus.size() == 1) {
            Map.Entry<StockStatus, Integer> entry = quantitiesByStatus.entrySet().iterator().next();
            StockStatus status = entry.getKey();
            int currentQty = entry.getValue();
            int newQty = currentQty + quantityChange;

            if (newQty < 0) {
                throw new IllegalStateException(
                    String.format("Quantity change would result in negative quantity: %d + %d < 0",
                        currentQty, quantityChange));
            }

            if (quantityChange >= 0) {
                return addQuantity(status, quantityChange);
            } else {
                return removeQuantity(status, -quantityChange);
            }
        }

        // For multiple statuses, apply to AVAILABLE
        if (quantityChange >= 0) {
            return addQuantity(StockStatus.AVAILABLE, quantityChange);
        } else {
            return removeQuantity(StockStatus.AVAILABLE, -quantityChange);
        }
    }

    /**
     * Get all statuses with non-zero quantities
     */
    public Set<StockStatus> getActiveStatuses() {
        return new HashSet<>(quantitiesByStatus.keySet());
    }

    /**
     * Get breakdown as unmodifiable map
     */
    public Map<StockStatus, Integer> getBreakdown() {
        return Collections.unmodifiableMap(quantitiesByStatus);
    }

    private void validateInvariants() {
        for (Map.Entry<StockStatus, Integer> entry : quantitiesByStatus.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IllegalStateException(
                    String.format("Quantity for status %s cannot be negative: %d",
                        entry.getKey(), entry.getValue()));
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockStatusQuantity that = (StockStatusQuantity) o;
        return Objects.equals(quantitiesByStatus, that.quantitiesByStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantitiesByStatus);
    }

    @Override
    public String toString() {
        return "StockStatusQuantity{" +
                "totalOnHand=" + getQuantityOnHand() +
                ", available=" + getAvailableQuantity() +
                ", breakdown=" + quantitiesByStatus +
                '}';
    }
}
