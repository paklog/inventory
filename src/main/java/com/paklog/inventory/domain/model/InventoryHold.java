package com.paklog.inventory.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a hold placed on inventory.
 * Holds prevent inventory from being allocated or moved.
 *
 * Industry pattern: SAP IM Stock Block, Oracle Inventory Holds
 */
public class InventoryHold {

    private final String holdId;
    private final HoldType holdType;
    private final int quantityOnHold;
    private final String reason;
    private final String placedBy;
    private final LocalDateTime placedAt;
    private final LocalDateTime expiresAt; // Optional, null means no expiration
    private final String lotNumber; // Optional, null means hold applies to all lots
    private final Location location; // Optional, null means hold applies to all locations

    private InventoryHold(String holdId, HoldType holdType, int quantityOnHold,
                          String reason, String placedBy, LocalDateTime placedAt,
                          LocalDateTime expiresAt, String lotNumber, Location location) {
        this.holdId = holdId;
        this.holdType = holdType;
        this.quantityOnHold = quantityOnHold;
        this.reason = reason;
        this.placedBy = placedBy;
        this.placedAt = placedAt;
        this.expiresAt = expiresAt;
        this.lotNumber = lotNumber;
        this.location = location;
        validateInvariants();
    }

    public static InventoryHold create(HoldType holdType, int quantityOnHold,
                                      String reason, String placedBy) {
        return new InventoryHold(
            UUID.randomUUID().toString(),
            holdType,
            quantityOnHold,
            reason,
            placedBy,
            LocalDateTime.now(),
            null,
            null,
            null
        );
    }

    public static InventoryHold create(HoldType holdType, int quantityOnHold,
                                      String reason, String placedBy,
                                      LocalDateTime expiresAt) {
        return new InventoryHold(
            UUID.randomUUID().toString(),
            holdType,
            quantityOnHold,
            reason,
            placedBy,
            LocalDateTime.now(),
            expiresAt,
            null,
            null
        );
    }

    public static InventoryHold createForLot(HoldType holdType, int quantityOnHold,
                                            String reason, String placedBy,
                                            String lotNumber) {
        return new InventoryHold(
            UUID.randomUUID().toString(),
            holdType,
            quantityOnHold,
            reason,
            placedBy,
            LocalDateTime.now(),
            null,
            lotNumber,
            null
        );
    }

    public static InventoryHold createForLocation(HoldType holdType, int quantityOnHold,
                                                 String reason, String placedBy,
                                                 Location location) {
        return new InventoryHold(
            UUID.randomUUID().toString(),
            holdType,
            quantityOnHold,
            reason,
            placedBy,
            LocalDateTime.now(),
            null,
            null,
            location
        );
    }

    public static InventoryHold load(String holdId, HoldType holdType, int quantityOnHold,
                                    String reason, String placedBy, LocalDateTime placedAt,
                                    LocalDateTime expiresAt, String lotNumber, Location location) {
        return new InventoryHold(holdId, holdType, quantityOnHold, reason, placedBy,
                                placedAt, expiresAt, lotNumber, location);
    }

    // Alternative factory method for creating with expiry
    public static InventoryHold createWithExpiry(HoldType holdType, int quantityOnHold,
                                                 String reason, String placedBy,
                                                 LocalDateTime expiresAt) {
        return new InventoryHold(
            java.util.UUID.randomUUID().toString(),
            holdType,
            quantityOnHold,
            reason,
            placedBy,
            LocalDateTime.now(),
            expiresAt,
            null,
            null
        );
    }

    /**
     * Release this hold (mark as inactive by setting expiry to now)
     */
    public InventoryHold release() {
        return new InventoryHold(
            this.holdId,
            this.holdType,
            this.quantityOnHold,
            this.reason,
            this.placedBy,
            this.placedAt,
            LocalDateTime.now(), // Set expiry to now to mark as released
            this.lotNumber,
            this.location
        );
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !isExpired();
    }

    public boolean appliesToLot(String lotNumber) {
        return this.lotNumber == null || this.lotNumber.equals(lotNumber);
    }

    public boolean appliesToLocation(Location location) {
        return this.location == null || this.location.equals(location);
    }

    private void validateInvariants() {
        if (holdId == null || holdId.trim().isEmpty()) {
            throw new IllegalArgumentException("Hold ID cannot be null or empty");
        }
        if (holdType == null) {
            throw new IllegalArgumentException("Hold type cannot be null");
        }
        if (quantityOnHold <= 0) {
            throw new IllegalArgumentException("Quantity on hold must be positive: " + quantityOnHold);
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Hold reason cannot be null or empty");
        }
        if (placedBy == null || placedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("PlacedBy cannot be null or empty");
        }
        if (placedAt == null) {
            throw new IllegalArgumentException("PlacedAt timestamp cannot be null");
        }
        if (expiresAt != null && expiresAt.isBefore(placedAt)) {
            throw new IllegalArgumentException("Expiration date cannot be before placement date");
        }
    }

    // Getters
    public String getHoldId() {
        return holdId;
    }

    public HoldType getHoldType() {
        return holdType;
    }

    public int getQuantityOnHold() {
        return quantityOnHold;
    }

    // Alias for getQuantityOnHold
    public int getQuantity() {
        return quantityOnHold;
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

    public String getLotNumber() {
        return lotNumber;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryHold that = (InventoryHold) o;
        return Objects.equals(holdId, that.holdId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(holdId);
    }

    @Override
    public String toString() {
        return "InventoryHold{" +
                "holdId='" + holdId + '\'' +
                ", holdType=" + holdType +
                ", quantityOnHold=" + quantityOnHold +
                ", reason='" + reason + '\'' +
                ", placedBy='" + placedBy + '\'' +
                ", placedAt=" + placedAt +
                ", expiresAt=" + expiresAt +
                ", lotNumber='" + lotNumber + '\'' +
                ", location=" + location +
                ", isActive=" + isActive() +
                '}';
    }
}
