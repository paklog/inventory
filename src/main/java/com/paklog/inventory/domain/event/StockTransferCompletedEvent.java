package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.Location;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Domain event published when a stock transfer is completed.
 * Important for inventory reconciliation and location updates.
 */
public class StockTransferCompletedEvent extends DomainEvent {

    private String transferId;
    private String sku;
    private Location sourceLocation;
    private Location destinationLocation;
    private int quantityTransferred;
    private int actualQuantityReceived;
    private boolean hasShrinkage;

    public StockTransferCompletedEvent(String transferId, String sku,
                                      Location sourceLocation, Location destinationLocation,
                                      int quantityTransferred, int actualQuantityReceived) {
        super(transferId);
        this.transferId = transferId;
        this.sku = sku;
        this.sourceLocation = sourceLocation;
        this.destinationLocation = destinationLocation;
        this.quantityTransferred = quantityTransferred;
        this.actualQuantityReceived = actualQuantityReceived;
        this.hasShrinkage = actualQuantityReceived < quantityTransferred;
    }

    @Override
    public String getEventType() {
        return CloudEventType.STOCK_TRANSFER_COMPLETED.getType();
    }

    @Override
    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("transferId", transferId);
        data.put("sku", sku);
        data.put("sourceLocation", sourceLocation.toLocationCode());
        data.put("destinationLocation", destinationLocation.toLocationCode());
        data.put("quantityTransferred", quantityTransferred);
        data.put("actualQuantityReceived", actualQuantityReceived);
        data.put("hasShrinkage", hasShrinkage);
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

    public int getQuantityTransferred() {
        return quantityTransferred;
    }

    public int getActualQuantityReceived() {
        return actualQuantityReceived;
    }

    public boolean hasShrinkage() {
        return hasShrinkage;
    }

    public int getShrinkageQuantity() {
        return quantityTransferred - actualQuantityReceived;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockTransferCompletedEvent that = (StockTransferCompletedEvent) o;
        return Objects.equals(transferId, that.transferId) &&
               Objects.equals(occurredOn, that.occurredOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transferId, occurredOn);
    }

    @Override
    public String toString() {
        return String.format("StockTransferCompletedEvent{id='%s', sku='%s', %s->%s, sent=%d, received=%d, shrinkage=%b}",
                transferId, sku, sourceLocation.toLocationCode(),
                destinationLocation.toLocationCode(), quantityTransferred,
                actualQuantityReceived, hasShrinkage);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String transferId;
        private String sku;
        private Location sourceLocation;
        private Location destinationLocation;
        private int quantityTransferred;
        private int actualQuantityReceived;
        private boolean hasShrinkage;

        public Builder transferId(final String transferId) { this.transferId = transferId; return this; }
        public Builder sku(final String sku) { this.sku = sku; return this; }
        public Builder sourceLocation(final Location sourceLocation) { this.sourceLocation = sourceLocation; return this; }
        public Builder destinationLocation(final Location destinationLocation) { this.destinationLocation = destinationLocation; return this; }
        public Builder quantityTransferred(final int quantityTransferred) { this.quantityTransferred = quantityTransferred; return this; }
        public Builder actualQuantityReceived(final int actualQuantityReceived) { this.actualQuantityReceived = actualQuantityReceived; return this; }
        public Builder hasShrinkage(final boolean hasShrinkage) { this.hasShrinkage = hasShrinkage; return this; }

        public StockTransferCompletedEvent build() {
            return new StockTransferCompletedEvent(transferId, sku, sourceLocation, destinationLocation, quantityTransferred, actualQuantityReceived);
        }
    }
}