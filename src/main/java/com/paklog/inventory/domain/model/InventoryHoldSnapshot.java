package com.paklog.inventory.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object capturing inventory hold state at snapshot time.
 */
public class InventoryHoldSnapshot {

    private final String holdId;
    private final String holdType;
    private final int quantity;
    private final String reason;
    private final String placedBy;
    private final LocalDateTime placedAt;
    private final LocalDateTime expiresAt;
    private final boolean active;

    private InventoryHoldSnapshot(String holdId, String holdType, int quantity, String reason,
                                 String placedBy, LocalDateTime placedAt, LocalDateTime expiresAt,
                                 boolean active) {
        this.holdId = holdId;
        this.holdType = holdType;
        this.quantity = quantity;
        this.reason = reason;
        this.placedBy = placedBy;
        this.placedAt = placedAt;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public static InventoryHoldSnapshot of(String holdId, String holdType, int quantity,
                                          String reason, String placedBy, LocalDateTime placedAt,
                                          LocalDateTime expiresAt, boolean active) {
        return new InventoryHoldSnapshot(holdId, holdType, quantity, reason, placedBy,
            placedAt, expiresAt, active);
    }

    public static InventoryHoldSnapshot fromInventoryHold(InventoryHold hold) {
        return new InventoryHoldSnapshot(
            hold.getHoldId(),
            hold.getHoldType().name(),
            hold.getQuantity(),
            hold.getReason(),
            hold.getPlacedBy(),
            hold.getPlacedAt(),
            hold.getExpiresAt(), // Already nullable LocalDateTime
            hold.isActive()
        );
    }

    public String getHoldId() {
        return holdId;
    }

    public String getHoldType() {
        return holdType;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    public String getPlacedBy() {
        return placedBy;
    }

    public LocalDateTime getPlacedAt() {
        return placedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryHoldSnapshot that = (InventoryHoldSnapshot) o;
        return Objects.equals(holdId, that.holdId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(holdId);
    }
}
