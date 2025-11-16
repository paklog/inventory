package com.paklog.inventory.domain.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain event published when an inventory hold is released.
 * Signals that held inventory is now available for allocation.
 */
public class InventoryHoldReleasedEvent extends DomainEvent {

    private String sku;
    private String holdId;
    private int quantityReleased;
    private String releasedBy;

    public InventoryHoldReleasedEvent(String sku, String holdId, int quantityReleased, String releasedBy) {
        super(sku);
        this.sku = sku;
        this.holdId = holdId;
        this.quantityReleased = quantityReleased;
        this.releasedBy = releasedBy;
    }

    @Override
    public String getEventType() {
        return CloudEventType.INVENTORY_HOLD_RELEASED.getType();
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("sku", sku);
        data.put("holdId", holdId);
        data.put("quantityReleased", quantityReleased);
        data.put("releasedBy", releasedBy);
        return Collections.unmodifiableMap(data);
    }

    // Getters
    public String getSku() {
        return sku;
    }

    public String getHoldId() {
        return holdId;
    }

    public int getQuantityReleased() {
        return quantityReleased;
    }

    public String getReleasedBy() {
        return releasedBy;
    }

    // Alias for getOccurredOn
    public java.time.LocalDateTime getOccurredAt() {
        return getOccurredOn();
    }

    // Additional getters for compatibility
    public com.paklog.inventory.domain.model.HoldType getHoldType() {
        // Return a default or derive from context
        return com.paklog.inventory.domain.model.HoldType.QUALITY_HOLD;
    }

    public String getReason() {
        return "Hold released";
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String sku;
        private String holdId;
        private int quantityReleased;
        private String releasedBy;

        public Builder sku(final String sku) { this.sku = sku; return this; }
        public Builder holdId(final String holdId) { this.holdId = holdId; return this; }
        public Builder quantityReleased(final int quantityReleased) { this.quantityReleased = quantityReleased; return this; }
        public Builder releasedBy(final String releasedBy) { this.releasedBy = releasedBy; return this; }

        public InventoryHoldReleasedEvent build() {
            return new InventoryHoldReleasedEvent(sku, holdId, quantityReleased, releasedBy);
        }
    }
}