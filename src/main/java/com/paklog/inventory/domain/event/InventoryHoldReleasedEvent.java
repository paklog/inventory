package com.paklog.inventory.domain.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain event published when an inventory hold is released.
 * Signals that held inventory is now available for allocation.
 */
public class InventoryHoldReleasedEvent extends DomainEvent {

    private final String sku;
    private final String holdId;
    private final int quantityReleased;
    private final String releasedBy;

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
}
