package com.paklog.inventory.domain.model;

import java.util.Objects;

public class Location {
    private final String warehouseId;
    private final String aisle;
    private final String shelf;
    private final String bin;
    private final String zoneId; // Reference to zone
    private final LocationType locationType;
    private final LocationCapacity capacity;

    // Default constructor for frameworks (MongoDB)
    protected Location() {
        this.warehouseId = null;
        this.aisle = null;
        this.shelf = null;
        this.bin = null;
        this.zoneId = null;
        this.locationType = null;
        this.capacity = null;
    }

    public Location(String aisle, String shelf, String bin) {
        this(null, null, aisle, shelf, bin, LocationType.GENERAL, LocationCapacity.unlimited());
    }

    public Location(String warehouseId, String zoneId, String aisle, String shelf, String bin,
                   LocationType locationType, LocationCapacity capacity) {
        if (aisle == null || aisle.trim().isEmpty()) {
            throw new IllegalArgumentException("Location aisle cannot be null or empty");
        }
        if (shelf == null || shelf.trim().isEmpty()) {
            throw new IllegalArgumentException("Location shelf cannot be null or empty");
        }
        if (bin == null || bin.trim().isEmpty()) {
            throw new IllegalArgumentException("Location bin cannot be null or empty");
        }
        this.warehouseId = warehouseId;
        this.aisle = aisle.trim();
        this.shelf = shelf.trim();
        this.bin = bin.trim();
        this.zoneId = zoneId;
        this.locationType = locationType != null ? locationType : LocationType.GENERAL;
        this.capacity = capacity != null ? capacity : LocationCapacity.unlimited();
    }

    public static Location of(String warehouseId, String zoneId, String aisle, String shelf, String bin,
                             LocationType locationType) {
        return new Location(warehouseId, zoneId, aisle, shelf, bin, locationType, LocationCapacity.unlimited());
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getAisle() {
        return aisle;
    }

    public String getShelf() {
        return shelf;
    }

    public String getBin() {
        return bin;
    }

    public String getZoneId() {
        return zoneId;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public LocationCapacity getCapacity() {
        return capacity;
    }

    public String toLocationCode() {
        return aisle + "-" + shelf + "-" + bin;
    }

    public boolean isInZone(String zoneId) {
        return this.zoneId != null && this.zoneId.equals(zoneId);
    }

    public boolean canAccommodate(int pallets, double weightKg, double cubicMeters, int units) {
        return capacity.canAccommodate(pallets, weightKg, cubicMeters, units);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(aisle, location.aisle) &&
                Objects.equals(shelf, location.shelf) &&
                Objects.equals(bin, location.bin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aisle, shelf, bin);
    }

    @Override
    public String toString() {
        return "Location{"
                + "aisle='" + aisle + "'"
                + ", shelf='" + shelf + "'"
                + ", bin='" + bin + "'"
                + ", zoneId='" + zoneId + "'"
                + ", locationType=" + locationType
                + ", capacity=" + capacity
                + '}';
    }
}