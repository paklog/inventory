package com.paklog.inventory.domain.model;

import java.util.Objects;

/**
 * Represents a warehouse zone with specific characteristics and rules.
 * Zones are used to organize warehouse locations by function and optimize operations.
 */
public class Zone {

    private final String zoneId;
    private final String zoneName;
    private final ZoneType zoneType;
    private final String warehouseId;
    private final ZoneAttributes attributes;

    private Zone(String zoneId, String zoneName, ZoneType zoneType, String warehouseId, ZoneAttributes attributes) {
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.zoneType = zoneType;
        this.warehouseId = warehouseId;
        this.attributes = attributes;
        validateInvariants();
    }

    public static Zone create(String zoneId, String zoneName, ZoneType zoneType, String warehouseId) {
        return new Zone(zoneId, zoneName, zoneType, warehouseId, ZoneAttributes.defaults());
    }

    public static Zone create(String zoneId, String zoneName, ZoneType zoneType,
                             String warehouseId, ZoneAttributes attributes) {
        return new Zone(zoneId, zoneName, zoneType, warehouseId, attributes);
    }

    public static Zone load(String zoneId, String zoneName, ZoneType zoneType,
                           String warehouseId, ZoneAttributes attributes) {
        return new Zone(zoneId, zoneName, zoneType, warehouseId, attributes);
    }

    public boolean isPickZone() {
        return zoneType == ZoneType.PICK || zoneType == ZoneType.FAST_PICK;
    }

    public boolean isStorageZone() {
        return zoneType == ZoneType.BULK || zoneType == ZoneType.RESERVE;
    }

    public boolean isSpecialHandling() {
        return zoneType == ZoneType.QUARANTINE ||
               zoneType == ZoneType.HAZMAT ||
               zoneType == ZoneType.HIGH_VALUE;
    }

    public boolean requiresTemperatureControl() {
        return attributes.getTemperatureControlled();
    }

    public boolean canStoreProduct(String productCategory) {
        // Business logic for zone-product compatibility
        if (zoneType == ZoneType.HAZMAT && !isHazardousMaterial(productCategory)) {
            return false;
        }
        return true;
    }

    private boolean isHazardousMaterial(String productCategory) {
        // Simplified logic - would integrate with product master in real implementation
        return productCategory != null && productCategory.contains("HAZMAT");
    }

    private void validateInvariants() {
        if (zoneId == null || zoneId.trim().isEmpty()) {
            throw new IllegalArgumentException("Zone ID cannot be null or empty");
        }
        if (zoneName == null || zoneName.trim().isEmpty()) {
            throw new IllegalArgumentException("Zone name cannot be null or empty");
        }
        if (zoneType == null) {
            throw new IllegalArgumentException("Zone type cannot be null");
        }
        if (warehouseId == null || warehouseId.trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse ID cannot be null or empty");
        }
    }

    // Getters
    public String getZoneId() {
        return zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public ZoneType getZoneType() {
        return zoneType;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public ZoneAttributes getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Zone zone = (Zone) o;
        return Objects.equals(zoneId, zone.zoneId) &&
               Objects.equals(warehouseId, zone.warehouseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zoneId, warehouseId);
    }

    @Override
    public String toString() {
        return "Zone{" +
                "zoneId='" + zoneId + '\'' +
                ", zoneName='" + zoneName + '\'' +
                ", zoneType=" + zoneType +
                ", warehouseId='" + warehouseId + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
