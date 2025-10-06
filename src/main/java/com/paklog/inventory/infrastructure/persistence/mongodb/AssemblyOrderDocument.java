package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.AssemblyOrder;
import com.paklog.inventory.domain.model.AssemblyStatus;
import com.paklog.inventory.domain.model.AssemblyType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MongoDB document for AssemblyOrder aggregate.
 */
@Document(collection = "assembly_orders")
@CompoundIndexes({
    @CompoundIndex(name = "orderId_idx", def = "{'assemblyOrderId': 1}", unique = true),
    @CompoundIndex(name = "kit_status_idx", def = "{'kitSku': 1, 'status': 1}"),
    @CompoundIndex(name = "type_status_idx", def = "{'assemblyType': 1, 'status': 1}"),
    @CompoundIndex(name = "created_idx", def = "{'createdAt': -1}")
})
public class AssemblyOrderDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String assemblyOrderId;
    private String assemblyType;  // AssemblyType as string
    private String kitSku;
    private int kitQuantity;
    private List<ComponentAllocationDocument> componentAllocations;
    private LocationDocument assemblyLocation;
    private String status;  // AssemblyStatus as string
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String completedBy;
    private int actualQuantityProduced;

    public AssemblyOrderDocument() {
    }

    public static AssemblyOrderDocument fromDomain(AssemblyOrder order) {
        AssemblyOrderDocument doc = new AssemblyOrderDocument();
        doc.assemblyOrderId = order.getAssemblyOrderId();
        doc.assemblyType = order.getAssemblyType().name();
        doc.kitSku = order.getKitSku();
        doc.kitQuantity = order.getKitQuantity();
        doc.componentAllocations = order.getComponentAllocations().stream()
            .map(ComponentAllocationDocument::fromDomain)
            .collect(Collectors.toList());
        doc.assemblyLocation = LocationDocument.fromDomain(order.getAssemblyLocation());
        doc.status = order.getStatus().name();
        doc.createdBy = order.getCreatedBy();
        doc.createdAt = order.getCreatedAt();
        doc.startedAt = order.getStartedAt().orElse(null);
        doc.completedAt = order.getCompletedAt().orElse(null);
        doc.completedBy = order.getCompletedBy().orElse(null);
        doc.actualQuantityProduced = order.getActualQuantityProduced();
        return doc;
    }

    public AssemblyOrder toDomain() {
        AssemblyOrder order;

        if (AssemblyType.valueOf(assemblyType) == AssemblyType.ASSEMBLE) {
            order = AssemblyOrder.createAssembly(
                assemblyOrderId,
                kitSku,
                kitQuantity,
                componentAllocations.stream()
                    .map(ComponentAllocationDocument::toDomain)
                    .collect(Collectors.toList()),
                assemblyLocation.toDomain(),
                createdBy
            );
        } else {
            order = AssemblyOrder.createDisassembly(
                assemblyOrderId,
                kitSku,
                kitQuantity,
                assemblyLocation.toDomain(),
                createdBy
            );
        }

        // Restore state based on status
        AssemblyStatus currentStatus = AssemblyStatus.valueOf(status);
        if (currentStatus == AssemblyStatus.IN_PROGRESS) {
            order.start();
        } else if (currentStatus == AssemblyStatus.COMPLETED && completedBy != null) {
            order.start();
            order.complete(actualQuantityProduced, completedBy);
        } else if (currentStatus == AssemblyStatus.CANCELLED) {
            order.cancel("Restored from persistence");
        }

        return order;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAssemblyOrderId() {
        return assemblyOrderId;
    }

    public void setAssemblyOrderId(String assemblyOrderId) {
        this.assemblyOrderId = assemblyOrderId;
    }

    public String getAssemblyType() {
        return assemblyType;
    }

    public void setAssemblyType(String assemblyType) {
        this.assemblyType = assemblyType;
    }

    public String getKitSku() {
        return kitSku;
    }

    public void setKitSku(String kitSku) {
        this.kitSku = kitSku;
    }

    public int getKitQuantity() {
        return kitQuantity;
    }

    public void setKitQuantity(int kitQuantity) {
        this.kitQuantity = kitQuantity;
    }

    public List<ComponentAllocationDocument> getComponentAllocations() {
        return componentAllocations;
    }

    public void setComponentAllocations(List<ComponentAllocationDocument> componentAllocations) {
        this.componentAllocations = componentAllocations;
    }

    public LocationDocument getAssemblyLocation() {
        return assemblyLocation;
    }

    public void setAssemblyLocation(LocationDocument assemblyLocation) {
        this.assemblyLocation = assemblyLocation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(String completedBy) {
        this.completedBy = completedBy;
    }

    public int getActualQuantityProduced() {
        return actualQuantityProduced;
    }

    public void setActualQuantityProduced(int actualQuantityProduced) {
        this.actualQuantityProduced = actualQuantityProduced;
    }
}
