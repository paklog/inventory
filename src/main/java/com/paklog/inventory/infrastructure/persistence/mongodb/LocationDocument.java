package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.LocationType;

/**
 * Embedded document for Location value object
 */
public class LocationDocument {

    private String warehouseId;
    private String zoneId;
    private String aisle;
    private String shelf;
    private String bin;
    private String locationType; // LocationType enum as string

    public LocationDocument() {
    }

    public static LocationDocument fromDomain(Location location) {
        LocationDocument doc = new LocationDocument();
        doc.warehouseId = location.getWarehouseId();
        doc.zoneId = location.getZoneId();
        doc.aisle = location.getAisle();
        doc.shelf = location.getShelf();
        doc.bin = location.getBin();
        doc.locationType = location.getLocationType().name();
        return doc;
    }

    public Location toDomain() {
        return Location.of(warehouseId, zoneId, aisle, shelf, bin,
                LocationType.valueOf(locationType));
    }

    // Getters and setters
    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getAisle() {
        return aisle;
    }

    public void setAisle(String aisle) {
        this.aisle = aisle;
    }

    public String getShelf() {
        return shelf;
    }

    public void setShelf(String shelf) {
        this.shelf = shelf;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }
}
