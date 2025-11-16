package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root for License Plate Number (LPN) / Container tracking.
 * Represents a physical container (pallet, case, tote) that holds inventory.
 * Enables mixed-SKU containers and packaging hierarchy.
 *
 * Part of the Inventory bounded context domain model.
 */
public class Container {

    private final String lpn; // License Plate Number (unique identifier)
    private final ContainerType type;
    private final List<ContainerItem> items;
    private final LocalDateTime createdAt;
    private final String createdBy;

    private Location currentLocation;
    private ContainerStatus status;
    private String parentLpn; // For nested containers (e.g., case inside pallet)
    private LocalDateTime lastMovedAt;
    private LocalDateTime closedAt;
    private LocalDateTime shippedAt;

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    private Container(String lpn, ContainerType type, Location currentLocation,
                     String createdBy, LocalDateTime createdAt,
                     ContainerStatus status) {
        this.lpn = lpn;
        this.type = type;
        this.items = new ArrayList<>();
        this.currentLocation = currentLocation;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.status = status;
        validateInvariants();
    }

    /**
     * Factory method to create a new container
     */
    public static Container create(ContainerType type, Location location, String createdBy) {
        String lpn = generateLPN(type);
        return new Container(lpn, type, location, createdBy, LocalDateTime.now(),
            ContainerStatus.ACTIVE);
    }

    /**
     * Factory method with specific LPN (for barcode integration)
     */
    public static Container createWithLPN(String lpn, ContainerType type,
                                         Location location, String createdBy) {
        return new Container(lpn, type, location, createdBy, LocalDateTime.now(),
            ContainerStatus.ACTIVE);
    }

    /**
     * Factory method for loading from persistence
     */
    public static Container load(String lpn, ContainerType type, Location currentLocation,
                                 String createdBy, LocalDateTime createdAt,
                                 ContainerStatus status, LocalDateTime lastMovedAt) {
        Container container = new Container(lpn, type, currentLocation, createdBy,
            createdAt, status);
        container.lastMovedAt = lastMovedAt;
        return container;
    }

    /**
     * Add an item to the container
     */
    public void addItem(String sku, int quantity, String lotNumber, Location sourceLocation) {
        if (status != ContainerStatus.ACTIVE) {
            throw new IllegalStateException(
                String.format("Cannot add items to container in %s status", status));
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Check if SKU already exists in container
        ContainerItem existingItem = items.stream()
            .filter(item -> item.getSku().equals(sku) &&
                          Objects.equals(item.getLotNumber(), lotNumber))
            .findFirst()
            .orElse(null);

        if (existingItem != null) {
            // Increase quantity
            items.remove(existingItem);
            items.add(existingItem.withQuantityChange(quantity));
        } else {
            // Add new item
            items.add(ContainerItem.create(sku, quantity, lotNumber, sourceLocation));
        }
    }

    /**
     * Remove an item from the container
     */
    public void removeItem(String sku, int quantity, String lotNumber) {
        ContainerItem item = items.stream()
            .filter(i -> i.getSku().equals(sku) &&
                        Objects.equals(i.getLotNumber(), lotNumber))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Item not found in container: SKU=%s, Lot=%s", sku, lotNumber)));

        if (quantity > item.getQuantity()) {
            throw new IllegalArgumentException(
                String.format("Cannot remove %d units. Only %d available", quantity, item.getQuantity()));
        }

        items.remove(item);

        if (item.getQuantity() > quantity) {
            // Partial removal - add back remaining quantity
            items.add(item.withQuantityChange(-quantity));
        }
    }

    /**
     * Move container to a new location
     */
    public void moveTo(Location newLocation) {
        if (status != ContainerStatus.ACTIVE) {
            throw new IllegalStateException(
                String.format("Cannot move container in %s status", status));
        }

        this.currentLocation = newLocation;
        this.lastMovedAt = LocalDateTime.now();
    }

    /**
     * Close the container (no more items can be added)
     */
    public void close() {
        if (status != ContainerStatus.ACTIVE) {
            throw new IllegalStateException("Container is not active");
        }

        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot close empty container");
        }

        this.status = ContainerStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    /**
     * Mark container as shipped
     */
    public void ship() {
        if (status != ContainerStatus.CLOSED) {
            throw new IllegalStateException("Container must be closed before shipping");
        }

        this.status = ContainerStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
    }

    /**
     * Empty the container (remove all items)
     */
    public void empty() {
        items.clear();
        this.status = ContainerStatus.EMPTY;
    }

    /**
     * Nest this container inside a parent container
     */
    public void nestInside(String parentLpn) {
        if (type == ContainerType.PALLET) {
            throw new IllegalStateException("Pallets cannot be nested inside other containers");
        }

        this.parentLpn = parentLpn;
    }

    /**
     * Check if container is empty
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Get total quantity across all items
     */
    public int getTotalQuantity() {
        return items.stream().mapToInt(ContainerItem::getQuantity).sum();
    }

    /**
     * Get total number of unique SKUs
     */
    public int getUniqueSKUCount() {
        return (int) items.stream().map(ContainerItem::getSku).distinct().count();
    }

    /**
     * Check if container is mixed-SKU
     */
    public boolean isMixedSKU() {
        return getUniqueSKUCount() > 1;
    }

    /**
     * Get maximum capacity based on container type
     */
    public int getMaxCapacity() {
        return type.getMaxCapacity();
    }

    /**
     * Check if container is at capacity
     */
    public boolean isAtCapacity() {
        return getTotalQuantity() >= getMaxCapacity();
    }

    private static String generateLPN(ContainerType type) {
        return String.format("%s-%s", type.getPrefix(), UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    private void validateInvariants() {
        if (lpn == null || lpn.isBlank()) {
            throw new IllegalArgumentException("LPN cannot be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Container type cannot be null");
        }
        if (currentLocation == null) {
            throw new IllegalArgumentException("Current location cannot be null");
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw new IllegalArgumentException("CreatedBy cannot be blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    // Getters
    public String getLpn() {
        return lpn;
    }

    public ContainerType getType() {
        return type;
    }

    public List<ContainerItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public ContainerStatus getStatus() {
        return status;
    }

    public String getParentLpn() {
        return parentLpn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getLastMovedAt() {
        return lastMovedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Container container = (Container) o;
        return Objects.equals(lpn, container.lpn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lpn);
    }

    @Override
    public String toString() {
        return String.format("Container{lpn='%s', type=%s, status=%s, items=%d, location=%s}",
                lpn, type, status, items.size(), currentLocation.toLocationCode());
    }
}
