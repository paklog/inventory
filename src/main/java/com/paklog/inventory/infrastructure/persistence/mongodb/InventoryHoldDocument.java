package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.HoldType;
import com.paklog.inventory.domain.model.InventoryHold;

import java.time.LocalDateTime;

/**
 * Embedded document for inventory holds within ProductStockDocument
 */
public class InventoryHoldDocument {

    private String holdId;
    private String holdType; // HoldType enum as string
    private int quantity;
    private String reason;
    private String placedBy;
    private LocalDateTime placedAt;
    private LocalDateTime expiresAt;
    private boolean active;

    public InventoryHoldDocument() {
    }

    public static InventoryHoldDocument fromDomain(InventoryHold hold) {
        InventoryHoldDocument doc = new InventoryHoldDocument();
        doc.holdId = hold.getHoldId();
        doc.holdType = hold.getHoldType().name();
        doc.quantity = hold.getQuantity();
        doc.reason = hold.getReason();
        doc.placedBy = hold.getPlacedBy();
        doc.placedAt = hold.getPlacedAt();
        doc.expiresAt = hold.getExpiresAt();
        doc.active = hold.isActive();
        return doc;
    }

    public InventoryHold toDomain() {
        if (expiresAt != null) {
            return InventoryHold.createWithExpiry(
                HoldType.valueOf(holdType),
                quantity,
                reason,
                placedBy,
                expiresAt
            );
        } else {
            return InventoryHold.create(
                HoldType.valueOf(holdType),
                quantity,
                reason,
                placedBy
            );
        }
    }

    // Getters and setters
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
