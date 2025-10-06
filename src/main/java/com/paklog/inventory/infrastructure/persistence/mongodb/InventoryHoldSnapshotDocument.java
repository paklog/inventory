package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.InventoryHoldSnapshot;

import java.time.LocalDateTime;

/**
 * Embedded document for InventoryHoldSnapshot.
 */
public class InventoryHoldSnapshotDocument {

    private String holdId;
    private String holdType;
    private int quantity;
    private String reason;
    private String placedBy;
    private LocalDateTime placedAt;
    private LocalDateTime expiresAt;
    private boolean active;

    public InventoryHoldSnapshotDocument() {
    }

    public static InventoryHoldSnapshotDocument fromDomain(InventoryHoldSnapshot hold) {
        InventoryHoldSnapshotDocument doc = new InventoryHoldSnapshotDocument();
        doc.holdId = hold.getHoldId();
        doc.holdType = hold.getHoldType();
        doc.quantity = hold.getQuantity();
        doc.reason = hold.getReason();
        doc.placedBy = hold.getPlacedBy();
        doc.placedAt = hold.getPlacedAt();
        doc.expiresAt = hold.getExpiresAt();
        doc.active = hold.isActive();
        return doc;
    }

    public InventoryHoldSnapshot toDomain() {
        return InventoryHoldSnapshot.of(holdId, holdType, quantity, reason,
            placedBy, placedAt, expiresAt, active);
    }

    public String getHoldId() {
        return holdId;
    }

    public void setHoldId(String holdId) {
        this.holdId = holdId;
    }

    public String getHoldType() {
        return holdType;
    }

    public void setHoldType(String holdType) {
        this.holdType = holdType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPlacedBy() {
        return placedBy;
    }

    public void setPlacedBy(String placedBy) {
        this.placedBy = placedBy;
    }

    public LocalDateTime getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(LocalDateTime placedAt) {
        this.placedAt = placedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
