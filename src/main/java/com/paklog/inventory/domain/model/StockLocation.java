package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.event.DomainEvent;

import com.paklog.inventory.domain.event.PhysicalStockReserved;
import com.paklog.inventory.domain.event.PhysicalStockReservationReleased;
import com.paklog.inventory.domain.event.StockAddedToLocation;
import com.paklog.inventory.domain.event.StockRemovedFromLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StockLocation {

    private String sku;
    private Location location;
    private int quantity;
    private List<PhysicalReservation> physicalReservations = new ArrayList<>();
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    public StockLocation(String sku, Location location, int quantity) {
        this.sku = sku;
        this.location = location;
        this.quantity = quantity;
    }

    public void addStock(int quantityToAdd) {
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive.");
        }
        this.quantity += quantityToAdd;
        addEvent(new StockAddedToLocation(this.sku, this.location, quantityToAdd));
    }

    public void removeStock(int quantityToRemove) {
        if (quantityToRemove <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be positive.");
        }
        if (this.quantity < quantityToRemove) {
            throw new IllegalStateException("Not enough stock to remove.");
        }
        this.quantity -= quantityToRemove;
        addEvent(new StockRemovedFromLocation(this.sku, this.location, quantityToRemove));
    }

    public void addPhysicalReservation(String reservationId, int quantityToReserve) {
        if (quantityToReserve <= 0) {
            throw new IllegalArgumentException("Quantity to reserve must be positive.");
        }
        if (getAvailableToPick() < quantityToReserve) {
            throw new IllegalStateException("Not enough stock to reserve.");
        }
        this.physicalReservations.add(new PhysicalReservation(reservationId, quantityToReserve));
        addEvent(new PhysicalStockReserved(this.sku, this.location, quantityToReserve, reservationId));
    }

    public void removePhysicalReservation(String reservationId) {
        boolean removed = this.physicalReservations.removeIf(reservation -> reservation.getReservationId().equals(reservationId));
        if (removed) {
            addEvent(new PhysicalStockReservationReleased(this.sku, this.location, reservationId));
        }
    }

    public int getAvailableToPick() {
        return this.quantity - this.physicalReservations.stream().mapToInt(PhysicalReservation::getQuantity).sum();
    }

    public String getSku() {
        return sku;
    }

    public Location getLocation() {
        return location;
    }

    public int getQuantity() {
        return quantity;
    }

    public List<PhysicalReservation> getPhysicalReservations() {
        return physicalReservations;
    }

    public List<DomainEvent> getUncommittedEvents() {
        return uncommittedEvents;
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
        StockLocation that = (StockLocation) o;
        return Objects.equals(sku, that.sku) &&
                Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, location);
    }
}
