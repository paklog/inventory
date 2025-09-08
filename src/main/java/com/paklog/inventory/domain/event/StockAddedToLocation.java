package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.Location;

import java.util.Map;

public class StockAddedToLocation extends DomainEvent {

    private static final String EVENT_TYPE = "paklog.inventory.stock-added-to-location.v1";

    private final String sku;
    private final Location location;
    private final int quantity;

    public StockAddedToLocation() {
        super();
        this.sku = null;
        this.location = null;
        this.quantity = 0;
    }

    public StockAddedToLocation(String sku, Location location, int quantity) {
        super(sku);
        this.sku = sku;
        this.location = location;
        this.quantity = quantity;
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
                "quantity", quantity
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
}
