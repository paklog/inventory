package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.ComponentAllocation;
import com.paklog.inventory.domain.model.Location;

import java.time.LocalDateTime;

/**
 * Embedded document for ComponentAllocation value object.
 */
public class ComponentAllocationDocument {

    private String componentSku;
    private int quantity;
    private LocationDocument sourceLocation;
    private String lotNumber;
    private LocalDateTime allocatedAt;

    public ComponentAllocationDocument() {
    }

    public static ComponentAllocationDocument fromDomain(ComponentAllocation allocation) {
        ComponentAllocationDocument doc = new ComponentAllocationDocument();
        doc.componentSku = allocation.getComponentSku();
        doc.quantity = allocation.getQuantity();
        doc.sourceLocation = LocationDocument.fromDomain(allocation.getSourceLocation());
        doc.lotNumber = allocation.getLotNumber();
        doc.allocatedAt = allocation.getAllocatedAt();
        return doc;
    }

    public ComponentAllocation toDomain() {
        Location location = sourceLocation.toDomain();
        return ComponentAllocation.of(componentSku, quantity, location, lotNumber);
    }

    public String getComponentSku() {
        return componentSku;
    }

    public void setComponentSku(String componentSku) {
        this.componentSku = componentSku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocationDocument getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(LocationDocument sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public void setAllocatedAt(LocalDateTime allocatedAt) {
        this.allocatedAt = allocatedAt;
    }
}
