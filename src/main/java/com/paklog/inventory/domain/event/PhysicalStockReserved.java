package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.Location;

import java.util.Map;

public class PhysicalStockReserved extends DomainEvent {

    private static final String EVENT_TYPE = "paklog.inventory.physical-stock-reserved.v1";

    private final String sku;
    private final Location location;
    private final int quantity;
    private final String reservationId;

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
}
