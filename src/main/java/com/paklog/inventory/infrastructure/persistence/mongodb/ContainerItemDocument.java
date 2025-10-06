package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.ContainerItem;

/**
 * Embedded document for container items within ContainerDocument
 */
public class ContainerItemDocument {

    private String sku;
    private int quantity;
    private String lotNumber;
    private LocationDocument sourceLocation;

    public ContainerItemDocument() {
    }

    public static ContainerItemDocument fromDomain(ContainerItem item) {
        ContainerItemDocument doc = new ContainerItemDocument();
        doc.sku = item.getSku();
        doc.quantity = item.getQuantity();
        doc.lotNumber = item.getLotNumber();
        doc.sourceLocation = LocationDocument.fromDomain(item.getSourceLocation());
        return doc;
    }

    public ContainerItem toDomain() {
        return ContainerItem.create(
            sku,
            quantity,
            lotNumber,
            sourceLocation.toDomain()
        );
    }

    // Getters and setters
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public LocationDocument getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(LocationDocument sourceLocation) {
        this.sourceLocation = sourceLocation;
    }
}
