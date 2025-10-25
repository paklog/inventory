package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.Location;

import java.util.Map;

public class PhysicalStockReserved extends DomainEvent {

    private static final String EVENT_TYPE = "paklog.inventory.physical-stock-reserved.v1";

    private String sku;
    private Location location;
    private int quantity;
    private String reservationId;

    public PhysicalStockReserved(String sku, Location location, int quantity, String reservationId) {
        super(sku);
        this.sku = sku;
        this.location = location;
        this.quantity = quantity;
        this.reservationId = reservationId;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of(
                "sku", sku,
                "location", location,
                "quantity", quantity,
                "reservationId", reservationId
        );
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

    public String getReservationId() {
        return reservationId;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String sku;
        private Location location;
        private int quantity;
        private String reservationId;

        public Builder sku(final String sku) { this.sku = sku; return this; }
        public Builder location(final Location location) { this.location = location; return this; }
        public Builder quantity(final int quantity) { this.quantity = quantity; return this; }
        public Builder reservationId(final String reservationId) { this.reservationId = reservationId; return this; }

        public PhysicalStockReserved build() {
            return new PhysicalStockReserved(sku, location, quantity, reservationId);
        }
    }
}