package com.paklog.inventory.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.paklog.inventory.domain.event.DomainEvent;
import com.paklog.inventory.domain.event.StockLevelChangedEvent;
import com.paklog.inventory.domain.exception.InsufficientStockException;
import com.paklog.inventory.domain.exception.InvalidQuantityException;
import com.paklog.inventory.domain.exception.StockLevelInvariantViolationException;

public class ProductStock {

    private String sku;
    private StockLevel stockLevel;
    private LocalDateTime lastUpdated;

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    // Private constructor for internal use and reconstruction from persistence
    private ProductStock(String sku, StockLevel stockLevel, LocalDateTime lastUpdated) {
        this.sku = sku;
        this.stockLevel = stockLevel;
        this.lastUpdated = lastUpdated;
        validateInvariants();
    }

    // Factory method for creating new ProductStock instances
    public static ProductStock create(String sku, int initialQuantity) {
        if (initialQuantity < 0) {
            throw new InvalidQuantityException("create", initialQuantity, "Initial quantity cannot be negative");
        }
        ProductStock productStock = new ProductStock(sku, StockLevel.of(initialQuantity, 0), LocalDateTime.now());
        productStock.addEvent(new StockLevelChangedEvent(productStock.getSku(), StockLevel.of(0, 0), productStock.getStockLevel(), "INITIAL_STOCK"));
        return productStock;
    }

    // Factory method for loading from persistence
    public static ProductStock load(String sku, int quantityOnHand, int quantityAllocated, LocalDateTime lastUpdated) {
        return new ProductStock(sku, StockLevel.of(quantityOnHand, quantityAllocated), lastUpdated);
    }

    public String getSku() {
        return sku;
    }

    public int getQuantityOnHand() {
        return stockLevel.getQuantityOnHand();
    }

    public int getQuantityAllocated() {
        return stockLevel.getQuantityAllocated();
    }

    public int getAvailableToPromise() {
        return stockLevel.getAvailableToPromise();
    }

    public StockLevel getStockLevel() { // Added getter for StockLevel
        return stockLevel;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Allocates a given quantity of stock.
     *
     * @param quantity The quantity to allocate.
     * @throws IllegalArgumentException if quantity is negative or if allocation exceeds available to promise.
     */
    public void allocate(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("allocate", quantity, "Allocation quantity must be positive");
        }
        if (stockLevel.getAvailableToPromise() < quantity) {
            throw new InsufficientStockException(sku, quantity, stockLevel.getAvailableToPromise());
        }

        StockLevel previousStockLevel = this.stockLevel;
        this.stockLevel = stockLevel.withAllocation(quantity);
        this.lastUpdated = LocalDateTime.now();
        validateInvariants();
        addEvent(new StockLevelChangedEvent(sku, previousStockLevel, this.stockLevel, "ALLOCATION"));
    }

    /**
     * Deallocates a given quantity of stock.
     *
     * @param quantity The quantity to deallocate.
     * @throws IllegalArgumentException if quantity is negative or if deallocation exceeds allocated quantity.
     */
    public void deallocate(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("deallocate", quantity, "Deallocation quantity must be positive");
        }
        if (stockLevel.getQuantityAllocated() < quantity) {
            throw new InsufficientStockException(sku, quantity, stockLevel.getQuantityAllocated());
        }

        StockLevel previousStockLevel = this.stockLevel;
        this.stockLevel = stockLevel.withDeallocation(quantity);
        this.lastUpdated = LocalDateTime.now();
        validateInvariants();
        addEvent(new StockLevelChangedEvent(sku, previousStockLevel, this.stockLevel, "DEALLOCATION"));
    }

    /**
     * Adjusts the quantity on hand. Can be positive (stock intake) or negative (damage, loss, pick).
     *
     * @param change The quantity change (positive or negative).
     * @param reason The reason for the adjustment.
     * @throws IllegalArgumentException if the adjustment results in negative quantity on hand.
     */
    public void adjustQuantityOnHand(int change, String reason) {
        if (stockLevel.getQuantityOnHand() + change < 0) {
            throw new InvalidQuantityException("adjust", change, 
                String.format("Would result in negative quantity on hand: %d + %d < 0", 
                             stockLevel.getQuantityOnHand(), change));
        }

        StockLevel previousStockLevel = this.stockLevel;
        this.stockLevel = stockLevel.withQuantityChange(change);
        this.lastUpdated = LocalDateTime.now();
        validateInvariants();
        addEvent(new StockLevelChangedEvent(sku, previousStockLevel, this.stockLevel, reason));
    }

    /**
     * Receives new stock, increasing quantity on hand.
     *
     * @param quantity The quantity of stock received.
     * @throws IllegalArgumentException if quantity is negative.
     */
    public void receiveStock(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("receive", quantity, "Received quantity must be positive");
        }
        adjustQuantityOnHand(quantity, "STOCK_RECEIPT");
    }

    public void increaseQuantityOnHand(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("increase", quantity, "Increased quantity must be positive");
        }
        adjustQuantityOnHand(quantity, "PHYSICAL_STOCK_ADDED");
    }

    public void decreaseQuantityOnHand(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("decrease", quantity, "Decreased quantity must be positive");
        }
        adjustQuantityOnHand(-quantity, "PHYSICAL_STOCK_REMOVED");
    }

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        this.uncommittedEvents.clear();
    }

    private void addEvent(DomainEvent event) {
        this.uncommittedEvents.add(event);
    }

    /* package-private */ void validateInvariants() {
        if (stockLevel.getQuantityOnHand() < 0) {
            throw new StockLevelInvariantViolationException(
                "Quantity on hand cannot be negative",
                stockLevel.getQuantityOnHand(),
                stockLevel.getQuantityAllocated());
        }
        if (stockLevel.getQuantityAllocated() < 0) {
            throw new StockLevelInvariantViolationException(
                "Quantity allocated cannot be negative",
                stockLevel.getQuantityOnHand(),
                stockLevel.getQuantityAllocated());
        }
        if (stockLevel.getQuantityAllocated() > stockLevel.getQuantityOnHand()) {
            throw new StockLevelInvariantViolationException(
                "Allocated quantity cannot exceed quantity on hand",
                stockLevel.getQuantityOnHand(),
                stockLevel.getQuantityAllocated());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductStock that = (ProductStock) o;
        return Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }
}