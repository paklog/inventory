package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.ChangeType;
import com.paklog.inventory.domain.model.InventoryLedgerEntry;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "inventory_ledger")
@CompoundIndex(name = "sku_timestamp_idx", def = "{ 'sku': 1, 'timestamp': -1 }")
public class InventoryLedgerEntryDocument {

    @Id
    private String id;
    private String sku;
    private LocalDateTime timestamp;
    private int quantityChange;
    private ChangeType changeType;
    private String sourceReference;
    private String reason;
    private String operatorId;

    public InventoryLedgerEntryDocument() {
    }

    public InventoryLedgerEntryDocument(String id, String sku, LocalDateTime timestamp, int quantityChange, ChangeType changeType, String sourceReference, String reason, String operatorId) {
        this.id = id;
        this.sku = sku;
        this.timestamp = timestamp;
        this.quantityChange = quantityChange;
        this.changeType = changeType;
        this.sourceReference = sourceReference;
        this.reason = reason;
        this.operatorId = operatorId;
    }

    public static InventoryLedgerEntryDocument fromDomain(InventoryLedgerEntry entry) {
        return new InventoryLedgerEntryDocument(
                entry.getId(),
                entry.getSku(),
                entry.getTimestamp(),
                entry.getQuantityChange(),
                entry.getChangeType(),
                entry.getSourceReference(),
                entry.getReason(),
                entry.getOperatorId()
        );
    }

    public InventoryLedgerEntry toDomain() {
        return InventoryLedgerEntry.load(
                this.id,
                this.sku,
                this.timestamp,
                this.quantityChange,
                this.changeType,
                this.sourceReference,
                this.reason,
                this.operatorId
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(int quantityChange) {
        this.quantityChange = quantityChange;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }
}
