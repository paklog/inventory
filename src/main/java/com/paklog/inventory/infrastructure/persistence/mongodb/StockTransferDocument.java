package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.StockTransfer;
import com.paklog.inventory.domain.model.TransferStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document for stock transfers (separate collection)
 */
@Document(collection = "stock_transfers")
@CompoundIndexes({
    @CompoundIndex(name = "transfer_id_idx", def = "{'transferId': 1}", unique = true),
    @CompoundIndex(name = "sku_status_idx", def = "{'sku': 1, 'status': 1}"),
    @CompoundIndex(name = "source_location_idx", def = "{'sourceLocation.warehouseId': 1, 'sourceLocation.zoneId': 1}"),
    @CompoundIndex(name = "dest_location_idx", def = "{'destinationLocation.warehouseId': 1, 'destinationLocation.zoneId': 1}"),
    @CompoundIndex(name = "initiated_at_idx", def = "{'initiatedAt': -1}")
})
public class StockTransferDocument {

    @Id
    private String transferId;
    private String sku;
    private LocationDocument sourceLocation;
    private LocationDocument destinationLocation;
    private int quantity;
    private String initiatedBy;
    private LocalDateTime initiatedAt;
    private String reason;

    @Indexed
    private String status; // TransferStatus enum as string

    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String cancellationReason;
    private int actualQuantityReceived;

    public StockTransferDocument() {
    }

    public static StockTransferDocument fromDomain(StockTransfer transfer) {
        StockTransferDocument doc = new StockTransferDocument();
        doc.transferId = transfer.getTransferId();
        doc.sku = transfer.getSku();
        doc.sourceLocation = LocationDocument.fromDomain(transfer.getSourceLocation());
        doc.destinationLocation = LocationDocument.fromDomain(transfer.getDestinationLocation());
        doc.quantity = transfer.getQuantity();
        doc.initiatedBy = transfer.getInitiatedBy();
        doc.initiatedAt = transfer.getInitiatedAt();
        doc.reason = transfer.getReason();
        doc.status = transfer.getStatus().name();
        doc.completedAt = transfer.getCompletedAt();
        doc.cancelledAt = transfer.getCancelledAt();
        doc.cancelledBy = transfer.getCancelledBy();
        doc.cancellationReason = transfer.getCancellationReason();
        doc.actualQuantityReceived = transfer.getActualQuantityReceived();
        return doc;
    }

    public StockTransfer toDomain() {
        return StockTransfer.load(
            transferId,
            sku,
            sourceLocation.toDomain(),
            destinationLocation.toDomain(),
            quantity,
            initiatedBy,
            initiatedAt,
            reason,
            TransferStatus.valueOf(status),
            completedAt,
            actualQuantityReceived
        );
    }

    // Getters and setters
    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public LocationDocument getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(LocationDocument sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public LocationDocument getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(LocationDocument destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public LocalDateTime getInitiatedAt() {
        return initiatedAt;
    }

    public void setInitiatedAt(LocalDateTime initiatedAt) {
        this.initiatedAt = initiatedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public int getActualQuantityReceived() {
        return actualQuantityReceived;
    }

    public void setActualQuantityReceived(int actualQuantityReceived) {
        this.actualQuantityReceived = actualQuantityReceived;
    }
}
