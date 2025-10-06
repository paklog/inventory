package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.HoldType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain event published when a hold is placed on inventory.
 * Other contexts may need to react (e.g., Order Management canceling allocations).
 */
public class InventoryHoldPlacedEvent extends DomainEvent {

    private final String sku;
    private final String holdId;
    private final HoldType holdType;
    private final int quantityOnHold;
    private final String reason;
    private final String lotNumber; // Optional

    public InventoryHoldPlacedEvent(String sku, String holdId, HoldType holdType,
                                   int quantityOnHold, String reason, String lotNumber) {
        super(sku);
        this.sku = sku;
        this.holdId = holdId;
        this.holdType = holdType;
        this.quantityOnHold = quantityOnHold;
        this.reason = reason;
        this.lotNumber = lotNumber;
    }

    @Override
    public String getEventType() {
        return CloudEventType.INVENTORY_HOLD_PLACED.getType();
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("sku", sku);
        data.put("holdId", holdId);
        data.put("holdType", holdType.name());
        data.put("quantityOnHold", quantityOnHold);
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

    public String getHoldId() {
        return holdId;
    }

    public HoldType getHoldType() {
        return holdType;
    }

    public int getQuantityOnHold() {
        return quantityOnHold;
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
}
