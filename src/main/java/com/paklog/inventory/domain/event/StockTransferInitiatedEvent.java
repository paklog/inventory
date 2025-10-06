package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.Location;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Domain event published when a stock transfer is initiated.
 * Important for inventory visibility and location-based ATP calculations.
 */
public class StockTransferInitiatedEvent extends DomainEvent {

    private final String transferId;
    private final String sku;
    private final Location sourceLocation;
    private final Location destinationLocation;
    private final int quantity;
    private final String initiatedBy;
    private final String reason;

    public StockTransferInitiatedEvent(String transferId, String sku,
                                      Location sourceLocation, Location destinationLocation,
                                      int quantity, String initiatedBy, String reason) {
        super(transferId);
        this.transferId = transferId;
        this.sku = sku;
        this.sourceLocation = sourceLocation;
        this.destinationLocation = destinationLocation;
        this.quantity = quantity;
        this.initiatedBy = initiatedBy;
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return CloudEventType.STOCK_TRANSFER_INITIATED.getType();
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("transferId", transferId);
        data.put("sku", sku);
        data.put("sourceLocation", sourceLocation.toLocationCode());
        data.put("destinationLocation", destinationLocation.toLocationCode());
        data.put("quantity", quantity);
        data.put("initiatedBy", initiatedBy);
        data.put("reason", reason);
        return Collections.unmodifiableMap(data);
    }

    // Alias for getOccurredOn
    public LocalDateTime occurredOn() {
        return getOccurredOn();
    }

    public String getTransferId() {
        return transferId;
    }

    public String getSku() {
        return sku;
    }

    public Location getSourceLocation() {
        return sourceLocation;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockTransferInitiatedEvent that = (StockTransferInitiatedEvent) o;
        return Objects.equals(transferId, that.transferId) &&
               Objects.equals(occurredOn, that.occurredOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transferId, occurredOn);
    }

    @Override
    public String toString() {
        return String.format("StockTransferInitiatedEvent{id='%s', sku='%s', %s->%s, qty=%d}",
                transferId, sku, sourceLocation.toLocationCode(),
                destinationLocation.toLocationCode(), quantity);
    }
}
