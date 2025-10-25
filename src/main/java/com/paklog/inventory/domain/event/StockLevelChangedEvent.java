package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.StockLevel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StockLevelChangedEvent extends DomainEvent {

    private String sku;
    private StockLevel previousStockLevel;
    private StockLevel newStockLevel;
    private String changeReason;

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

    // Alias for getOccurredOn
    public java.time.LocalDateTime getOccurredAt() {
        return getOccurredOn();
    }

    @Override
    public String getEventType() {
        return CloudEventType.STOCK_LEVEL_CHANGED.getType();
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

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String sku;
        private StockLevel previousStockLevel;
        private StockLevel newStockLevel;
        private String changeReason;

        public Builder sku(final String sku) { this.sku = sku; return this; }
        public Builder previousStockLevel(final StockLevel previousStockLevel) { this.previousStockLevel = previousStockLevel; return this; }
        public Builder newStockLevel(final StockLevel newStockLevel) { this.newStockLevel = newStockLevel; return this; }
        public Builder changeReason(final String changeReason) { this.changeReason = changeReason; return this; }

        public StockLevelChangedEvent build() {
            return new StockLevelChangedEvent(sku, previousStockLevel, newStockLevel, changeReason);
        }
    }
}