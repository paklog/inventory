package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.Location;

import java.util.Map;

public class StockRemovedFromLocation extends DomainEvent {

    private static final String EVENT_TYPE = "paklog.inventory.stock-removed-from-location.v1";

    private final String sku;
    private final Location location;
    private final int quantity;

    public StockRemovedFromLocation() {
        super();
        this.sku = null;
        this.location = null;
        this.quantity = 0;
    }

    public StockRemovedFromLocation(String sku, Location location, int quantity) {
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
