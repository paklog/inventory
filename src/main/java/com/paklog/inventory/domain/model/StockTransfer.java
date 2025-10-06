package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root for inter-location stock transfers.
 * Manages the lifecycle of inventory moving between locations.
 *
 * Part of the Inventory bounded context domain model.
 */
public class StockTransfer {

    private final String transferId;
    private final String sku;
    private final Location sourceLocation;
    private final Location destinationLocation;
    private final int quantity;
    private final String initiatedBy;
    private final LocalDateTime initiatedAt;
    private final String reason;

    private TransferStatus status;
    private LocalDateTime completedAt;
    private LocalDateTime inTransitAt;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String cancellationReason;
    private int actualQuantityReceived; // May differ from quantity due to damage/loss in transit

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    private StockTransfer(String transferId, String sku,
                         Location sourceLocation, Location destinationLocation,
                         int quantity, String initiatedBy, LocalDateTime initiatedAt,
                         String reason, TransferStatus status) {
        this.transferId = transferId;
        this.sku = sku;
        this.sourceLocation = sourceLocation;
        this.destinationLocation = destinationLocation;
        this.quantity = quantity;
        this.initiatedBy = initiatedBy;
        this.initiatedAt = initiatedAt;
        this.reason = reason;
        this.status = status;
        this.actualQuantityReceived = 0;
        validateInvariants();
    }

    /**
     * Factory method to create a new stock transfer
     */
    public static StockTransfer create(String sku,
                                      Location sourceLocation,
                                      Location destinationLocation,
                                      int quantity,
                                      String initiatedBy,
                                      String reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be positive");
        }

        if (sourceLocation.equals(destinationLocation)) {
            throw new IllegalArgumentException("Source and destination locations must be different");
        }

        String transferId = UUID.randomUUID().toString();
        return new StockTransfer(
            transferId, sku, sourceLocation, destinationLocation,
            quantity, initiatedBy, LocalDateTime.now(), reason,
            TransferStatus.INITIATED
        );
    }

    /**
     * Factory method for loading from persistence
     */
    public static StockTransfer load(String transferId, String sku,
                                    Location sourceLocation, Location destinationLocation,
                                    int quantity, String initiatedBy, LocalDateTime initiatedAt,
                                    String reason, TransferStatus status,
                                    LocalDateTime completedAt, int actualQuantityReceived) {
        StockTransfer transfer = new StockTransfer(
            transferId, sku, sourceLocation, destinationLocation,
            quantity, initiatedBy, initiatedAt, reason, status
        );
        transfer.completedAt = completedAt;
        transfer.actualQuantityReceived = actualQuantityReceived;
        return transfer;
    }

    /**
     * Mark transfer as in-transit (stock removed from source location)
     */
    public void markInTransit() {
        if (status != TransferStatus.INITIATED) {
            throw new IllegalStateException(
                String.format("Cannot mark transfer as in-transit. Current status: %s", status));
        }

        this.status = TransferStatus.IN_TRANSIT;
        this.inTransitAt = LocalDateTime.now();
    }

    /**
     * Complete the transfer (stock received at destination)
     */
    public void complete(int actualQuantityReceived) {
        if (status != TransferStatus.IN_TRANSIT) {
            throw new IllegalStateException(
                String.format("Cannot complete transfer. Current status: %s", status));
        }

        if (actualQuantityReceived <= 0 || actualQuantityReceived > quantity) {
            throw new IllegalArgumentException(
                String.format("Actual quantity received (%d) must be between 1 and %d",
                    actualQuantityReceived, quantity));
        }

        this.actualQuantityReceived = actualQuantityReceived;
        this.completedAt = LocalDateTime.now();
        this.status = TransferStatus.COMPLETED;
    }

    /**
     * Cancel the transfer
     */
    public void cancel(String cancelledBy, String cancellationReason) {
        if (status == TransferStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed transfer");
        }

        if (status == TransferStatus.CANCELLED) {
            throw new IllegalStateException("Transfer is already cancelled");
        }

        this.status = TransferStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = cancellationReason;
    }

    /**
     * Check if there was shrinkage/damage during transfer
     */
    public boolean hasShrinkage() {
        return status == TransferStatus.COMPLETED && actualQuantityReceived < quantity;
    }

    /**
     * Get shrinkage quantity
     */
    public int getShrinkageQuantity() {
        if (!hasShrinkage()) {
            return 0;
        }
        return quantity - actualQuantityReceived;
    }

    /**
     * Get shrinkage percentage
     */
    public double getShrinkagePercentage() {
        if (!hasShrinkage() || quantity == 0) {
            return 0.0;
        }
        return ((double) getShrinkageQuantity() / quantity) * 100.0;
    }

    /**
     * Get transfer duration in hours
     */
    public long getTransferDurationHours() {
        if (completedAt == null) {
            return 0;
        }
        return java.time.Duration.between(initiatedAt, completedAt).toHours();
    }

    private void validateInvariants() {
        if (transferId == null || transferId.isBlank()) {
            throw new IllegalArgumentException("Transfer ID cannot be blank");
        }
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be blank");
        }
        if (sourceLocation == null) {
            throw new IllegalArgumentException("Source location cannot be null");
        }
        if (destinationLocation == null) {
            throw new IllegalArgumentException("Destination location cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (initiatedBy == null || initiatedBy.isBlank()) {
            throw new IllegalArgumentException("InitiatedBy cannot be blank");
        }
        if (initiatedAt == null) {
            throw new IllegalArgumentException("InitiatedAt cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    // Getters
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

    // Alias for getQuantity
    public int getQuantityToTransfer() {
        return quantity;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public LocalDateTime getInitiatedAt() {
        return initiatedAt;
    }

    public String getReason() {
        return reason;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getInTransitAt() {
        return inTransitAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public int getActualQuantityReceived() {
        return actualQuantityReceived;
    }

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        this.uncommittedEvents.clear();
    }

    private void addEvent(DomainEvent event) {
        this.uncommittedEvents.add(event);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockTransfer that = (StockTransfer) o;
        return Objects.equals(transferId, that.transferId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transferId);
    }

    @Override
    public String toString() {
        return String.format("StockTransfer{id='%s', sku='%s', %s->%s, qty=%d, status=%s}",
                transferId, sku, sourceLocation.toLocationCode(),
                destinationLocation.toLocationCode(), quantity, status);
    }
}
