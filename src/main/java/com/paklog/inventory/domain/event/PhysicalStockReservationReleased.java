package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.Location;

import java.util.Map;

public class PhysicalStockReservationReleased extends DomainEvent {

    private static final String EVENT_TYPE = "paklog.inventory.physical-stock-reservation-released.v1";

    private final String sku;
    private final Location location;
    private final String reservationId;

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
}
