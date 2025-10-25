package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.Location;

import java.util.Map;

public class PhysicalStockReservationReleased extends DomainEvent {

    private static final String EVENT_TYPE = "paklog.inventory.physical-stock-reservation-released.v1";

    private String sku;
    private Location location;
    private String reservationId;

    public PhysicalStockReservationReleased(String sku, Location location, String reservationId) {
        super(sku);
        this.sku = sku;
        this.location = location;
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
                "reservationId", reservationId
        );
    }

    public String getSku() {
        return sku;
    }

    public Location getLocation() {
        return location;
    }

    public String getReservationId() {
        return reservationId;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String sku;
        private Location location;
        private String reservationId;

        public Builder sku(final String sku) { this.sku = sku; return this; }
        public Builder location(final Location location) { this.location = location; return this; }
        public Builder reservationId(final String reservationId) { this.reservationId = reservationId; return this; }

        public PhysicalStockReservationReleased build() {
            return new PhysicalStockReservationReleased(sku, location, reservationId);
        }
    }
}