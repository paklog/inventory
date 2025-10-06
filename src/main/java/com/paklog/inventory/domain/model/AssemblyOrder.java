package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Aggregate root for Assembly/Disassembly orders.
 * Represents the physical process of creating kits from components (assembly)
 * or breaking kits into components (disassembly).
 */
public class AssemblyOrder {

    private final String assemblyOrderId;
    private final AssemblyType assemblyType;        // ASSEMBLE, DISASSEMBLE
    private final String kitSku;
    private final int kitQuantity;
    private final List<ComponentAllocation> componentAllocations;
    private final Location assemblyLocation;
    private AssemblyStatus status;
    private final String createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String completedBy;
    private int actualQuantityProduced;

    // Domain events
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    private AssemblyOrder(String assemblyOrderId, AssemblyType assemblyType, String kitSku,
                         int kitQuantity, List<ComponentAllocation> componentAllocations,
                         Location assemblyLocation, String createdBy) {
        if (assemblyOrderId == null || assemblyOrderId.isBlank()) {
            throw new IllegalArgumentException("Assembly order ID cannot be null or blank");
        }
        if (assemblyType == null) {
            throw new IllegalArgumentException("Assembly type cannot be null");
        }
        if (kitSku == null || kitSku.isBlank()) {
            throw new IllegalArgumentException("Kit SKU cannot be null or blank");
        }
        if (kitQuantity <= 0) {
            throw new IllegalArgumentException("Kit quantity must be positive");
        }
        if (assemblyLocation == null) {
            throw new IllegalArgumentException("Assembly location cannot be null");
        }

        this.assemblyOrderId = assemblyOrderId;
        this.assemblyType = assemblyType;
        this.kitSku = kitSku;
        this.kitQuantity = kitQuantity;
        this.componentAllocations = componentAllocations == null ?
            new ArrayList<>() : new ArrayList<>(componentAllocations);
        this.assemblyLocation = assemblyLocation;
        this.status = AssemblyStatus.CREATED;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.actualQuantityProduced = 0;
    }

    /**
     * Create assembly order (components → kit)
     */
    public static AssemblyOrder createAssembly(String assemblyOrderId, String kitSku, int kitQuantity,
                                              List<ComponentAllocation> componentAllocations,
                                              Location assemblyLocation, String createdBy) {
        return new AssemblyOrder(assemblyOrderId, AssemblyType.ASSEMBLE, kitSku, kitQuantity,
            componentAllocations, assemblyLocation, createdBy);
    }

    /**
     * Create disassembly order (kit → components)
     */
    public static AssemblyOrder createDisassembly(String assemblyOrderId, String kitSku, int kitQuantity,
                                                 Location assemblyLocation, String createdBy) {
        return new AssemblyOrder(assemblyOrderId, AssemblyType.DISASSEMBLE, kitSku, kitQuantity,
            null, assemblyLocation, createdBy);
    }

    /**
     * Start assembly/disassembly work
     */
    public void start() {
        if (status != AssemblyStatus.CREATED) {
            throw new IllegalStateException("Cannot start assembly order in status " + status);
        }

        if (assemblyType == AssemblyType.ASSEMBLE && componentAllocations.isEmpty()) {
            throw new IllegalStateException("Cannot start assembly without component allocations");
        }

        this.status = AssemblyStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * Complete assembly/disassembly
     */
    public void complete(int actualQuantityProduced, String completedBy) {
        if (status != AssemblyStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete assembly order in status " + status);
        }

        if (actualQuantityProduced < 0 || actualQuantityProduced > kitQuantity) {
            throw new IllegalArgumentException(
                "Actual quantity produced must be between 0 and " + kitQuantity);
        }

        this.status = AssemblyStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.completedBy = completedBy;
        this.actualQuantityProduced = actualQuantityProduced;
    }

    /**
     * Cancel assembly/disassembly
     */
    public void cancel(String reason) {
        if (status == AssemblyStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed assembly order");
        }

        this.status = AssemblyStatus.CANCELLED;
    }

    /**
     * Add component allocation
     */
    public void addComponentAllocation(ComponentAllocation allocation) {
        if (assemblyType != AssemblyType.ASSEMBLE) {
            throw new IllegalStateException("Cannot add component allocations to disassembly order");
        }

        if (status != AssemblyStatus.CREATED) {
            throw new IllegalStateException("Cannot add allocations in status " + status);
        }

        componentAllocations.add(allocation);
    }

    /**
     * Check if all components are allocated
     */
    public boolean hasAllComponents(Kit kit) {
        if (assemblyType != AssemblyType.ASSEMBLE) {
            return true; // Disassembly doesn't need component allocations
        }

        for (KitComponent component : kit.getComponents()) {
            if (!component.isOptional()) {
                int required = component.getTotalQuantityFor(kitQuantity);
                int allocated = componentAllocations.stream()
                    .filter(a -> a.getComponentSku().equals(component.getComponentSku()))
                    .mapToInt(ComponentAllocation::getQuantity)
                    .sum();

                if (allocated < required) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Calculate yield percentage
     */
    public double getYieldPercentage() {
        if (status != AssemblyStatus.COMPLETED || kitQuantity == 0) {
            return 0.0;
        }

        return (actualQuantityProduced * 100.0) / kitQuantity;
    }

    /**
     * Check if assembly
     */
    public boolean isAssembly() {
        return assemblyType == AssemblyType.ASSEMBLE;
    }

    /**
     * Check if disassembly
     */
    public boolean isDisassembly() {
        return assemblyType == AssemblyType.DISASSEMBLE;
    }

    public String getAssemblyOrderId() {
        return assemblyOrderId;
    }

    public AssemblyType getAssemblyType() {
        return assemblyType;
    }

    public String getKitSku() {
        return kitSku;
    }

    public int getKitQuantity() {
        return kitQuantity;
    }

    public List<ComponentAllocation> getComponentAllocations() {
        return Collections.unmodifiableList(componentAllocations);
    }

    public Location getAssemblyLocation() {
        return assemblyLocation;
    }

    public AssemblyStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Optional<LocalDateTime> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<LocalDateTime> getCompletedAt() {
        return Optional.ofNullable(completedAt);
    }

    public Optional<String> getCompletedBy() {
        return Optional.ofNullable(completedBy);
    }

    public int getActualQuantityProduced() {
        return actualQuantityProduced;
    }

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
}
