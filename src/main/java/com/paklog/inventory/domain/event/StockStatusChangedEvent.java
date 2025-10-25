package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.StockStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain event published when inventory stock status changes.
 * Example: QUARANTINE → AVAILABLE, AVAILABLE → DAMAGED
 */
public class StockStatusChangedEvent extends DomainEvent {

    private String sku;
    private StockStatus previousStatus;
    private StockStatus newStatus;
    private int quantity;
    private String reason;
    private String lotNumber; // Optional

    public StockStatusChangedEvent(String sku, StockStatus previousStatus, StockStatus newStatus,
                                   int quantity, String reason, String lotNumber) {
        super(sku);
        this.sku = sku;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.quantity = quantity;
        this.reason = reason;
        this.lotNumber = lotNumber;
    }

    @Override
    public String getEventType() {
        return CloudEventType.STOCK_STATUS_CHANGED.getType();
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("sku", sku);
        data.put("previousStatus", previousStatus.name());
        data.put("newStatus", newStatus.name());
        data.put("quantity", quantity);
        data.put("reason", reason);
        if (lotNumber != null) {
            data.put("lotNumber", lotNumber);
        }
        return Collections.unmodifiableMap(data);
    }

    // Getters
    public String getSku() {
        return sku;
    }

    public StockStatus getPreviousStatus() {
        return previousStatus;
    }

    public StockStatus getNewStatus() {
        return newStatus;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    // Alias for getOccurredOn
    public java.time.LocalDateTime getOccurredAt() {
        return getOccurredOn();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String sku;
        private StockStatus previousStatus;
        private StockStatus newStatus;
        private int quantity;
        private String reason;
        private String lotNumber;

        public Builder sku(final String sku) { this.sku = sku; return this; }
        public Builder previousStatus(final StockStatus previousStatus) { this.previousStatus = previousStatus; return this; }
        public Builder newStatus(final StockStatus newStatus) { this.newStatus = newStatus; return this; }
        public Builder quantity(final int quantity) { this.quantity = quantity; return this; }
        public Builder reason(final String reason) { this.reason = reason; return this; }
        public Builder lotNumber(final String lotNumber) { this.lotNumber = lotNumber; return this; }

        public StockStatusChangedEvent build() {
            return new StockStatusChangedEvent(sku, previousStatus, newStatus, quantity, reason, lotNumber);
        }
    }
}