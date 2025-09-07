package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.StockLevel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StockLevelChangedEvent extends DomainEvent {

    private static final String EVENT_TYPE = "com.example.fulfillment.inventory.stock_level.changed";

    private final String sku;
    private final StockLevel previousStockLevel;
    private final StockLevel newStockLevel;
    private final String changeReason;

    public StockLevelChangedEvent(String sku, StockLevel previousStockLevel, StockLevel newStockLevel, String changeReason) {
        super(sku); // Aggregate ID is the SKU
        this.sku = sku;
        this.previousStockLevel = previousStockLevel;
        this.newStockLevel = newStockLevel;
        this.changeReason = changeReason;
    }

    public String getSku() {
        return sku;
    }

    public StockLevel getPreviousStockLevel() {
        return previousStockLevel;
    }

    public StockLevel getNewStockLevel() {
        return newStockLevel;
    }

    public String getChangeReason() {
        return changeReason;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("sku", sku);
        data.put("previousQuantityOnHand", previousStockLevel.getQuantityOnHand());
        data.put("previousQuantityAllocated", previousStockLevel.getQuantityAllocated());
        data.put("previousAvailableToPromise", previousStockLevel.getAvailableToPromise());
        data.put("newQuantityOnHand", newStockLevel.getQuantityOnHand());
        data.put("newQuantityAllocated", newStockLevel.getQuantityAllocated());
        data.put("newAvailableToPromise", newStockLevel.getAvailableToPromise());
        data.put("changeReason", changeReason);
        return Collections.unmodifiableMap(data);
    }
}