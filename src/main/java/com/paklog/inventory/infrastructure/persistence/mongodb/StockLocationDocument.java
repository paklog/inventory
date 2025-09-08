package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.PhysicalReservation;
import com.paklog.inventory.domain.model.StockLocation;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.stream.Collectors;

@Document(collection = "stock_locations")
public class StockLocationDocument {

    @Id
    private String id;
    private String sku;
    private LocationDocument location;
    private int quantity;
    private List<PhysicalReservationDocument> physicalReservations;

    public StockLocationDocument() {
    }

    public static StockLocationDocument fromDomain(StockLocation stockLocation) {
        StockLocationDocument doc = new StockLocationDocument();
        doc.setId(stockLocation.getSku() + ":" + stockLocation.getLocation().getAisle() + ":" + stockLocation.getLocation().getShelf() + ":" + stockLocation.getLocation().getBin());
        doc.setSku(stockLocation.getSku());
        doc.setLocation(LocationDocument.fromDomain(stockLocation.getLocation()));
        doc.setQuantity(stockLocation.getQuantity());
        doc.setPhysicalReservations(stockLocation.getPhysicalReservations().stream().map(PhysicalReservationDocument::fromDomain).collect(Collectors.toList()));
        return doc;
    }

    public StockLocation toDomain() {
        StockLocation stockLocation = new StockLocation(this.sku, this.location.toDomain(), this.quantity);
        this.physicalReservations.forEach(pr -> stockLocation.addPhysicalReservation(pr.getReservationId(), pr.getQuantity()));
        return stockLocation;
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

    public LocationDocument getLocation() {
        return location;
    }

    public void setLocation(LocationDocument location) {
        this.location = location;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public List<PhysicalReservationDocument> getPhysicalReservations() {
        return physicalReservations;
    }

    public void setPhysicalReservations(List<PhysicalReservationDocument> physicalReservations) {
        this.physicalReservations = physicalReservations;
    }
}

class LocationDocument {
    private String aisle;
    private String shelf;
    private String bin;

    public static LocationDocument fromDomain(Location location) {
        LocationDocument doc = new LocationDocument();
        doc.setAisle(location.getAisle());
        doc.setShelf(location.getShelf());
        doc.setBin(location.getBin());
        return doc;
    }

    public Location toDomain() {
        return new Location(this.aisle, this.shelf, this.bin);
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
}

class PhysicalReservationDocument {
    private String reservationId;
    private int quantity;

    public static PhysicalReservationDocument fromDomain(PhysicalReservation physicalReservation) {
        PhysicalReservationDocument doc = new PhysicalReservationDocument();
        doc.setReservationId(physicalReservation.getReservationId());
        doc.setQuantity(physicalReservation.getQuantity());
        return doc;
    }

    public PhysicalReservation toDomain() {
        return new PhysicalReservation(this.reservationId, this.quantity);
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
