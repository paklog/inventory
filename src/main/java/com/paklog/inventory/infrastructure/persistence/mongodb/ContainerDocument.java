package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.Container;
import com.paklog.inventory.domain.model.ContainerStatus;
import com.paklog.inventory.domain.model.ContainerType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MongoDB document for containers/LPN tracking (separate collection)
 */
@Document(collection = "containers")
@CompoundIndexes({
    @CompoundIndex(name = "lpn_idx", def = "{'lpn': 1}", unique = true),
    @CompoundIndex(name = "status_type_idx", def = "{'status': 1, 'type': 1}"),
    @CompoundIndex(name = "location_idx", def = "{'currentLocation.warehouseId': 1, 'currentLocation.zoneId': 1}"),
    @CompoundIndex(name = "parent_lpn_idx", def = "{'parentLpn': 1}"),
    @CompoundIndex(name = "created_at_idx", def = "{'createdAt': -1}")
})
public class ContainerDocument {

    @Id
    private String lpn; // License Plate Number (unique identifier)
    private String type; // ContainerType enum as string
    private List<ContainerItemDocument> items;
    private LocalDateTime createdAt;
    private String createdBy;

    private LocationDocument currentLocation;

    @Indexed
    private String status; // ContainerStatus enum as string

    private String parentLpn; // For nested containers
    private LocalDateTime lastMovedAt;

    public ContainerDocument() {
        this.items = new ArrayList<>();
    }

    public static ContainerDocument fromDomain(Container container) {
        ContainerDocument doc = new ContainerDocument();
        doc.lpn = container.getLpn();
        doc.type = container.getType().name();
        doc.items = container.getItems().stream()
                .map(ContainerItemDocument::fromDomain)
                .collect(Collectors.toList());
        doc.createdAt = container.getCreatedAt();
        doc.createdBy = container.getCreatedBy();
        doc.currentLocation = LocationDocument.fromDomain(container.getCurrentLocation());
        doc.status = container.getStatus().name();
        doc.parentLpn = container.getParentLpn();
        doc.lastMovedAt = container.getLastMovedAt();
        return doc;
    }

    public Container toDomain() {
        return Container.load(
            lpn,
            ContainerType.valueOf(type),
            currentLocation.toDomain(),
            createdBy,
            createdAt,
            ContainerStatus.valueOf(status),
            lastMovedAt
        );
    }

    // Getters and setters
    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ContainerItemDocument> getItems() {
        return items;
    }

    public void setItems(List<ContainerItemDocument> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocationDocument getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(LocationDocument currentLocation) {
        this.currentLocation = currentLocation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getParentLpn() {
        return parentLpn;
    }

    public void setParentLpn(String parentLpn) {
        this.parentLpn = parentLpn;
    }

    public LocalDateTime getLastMovedAt() {
        return lastMovedAt;
    }

    public void setLastMovedAt(LocalDateTime lastMovedAt) {
        this.lastMovedAt = lastMovedAt;
    }
}
