package com.paklog.inventory.domain.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Aggregate root for Kit/Assembly management.
 * A kit is a parent SKU composed of multiple component SKUs.
 * Supports both physical assembly (actual bundling) and virtual kitting (logical grouping).
 */
public class Kit {

    private final String kitSku;                    // Parent SKU code
    private final String kitDescription;
    private final List<KitComponent> components;    // Required components
    private final KitType kitType;                  // PHYSICAL, VIRTUAL
    private final boolean allowPartialKit;          // Can ship with missing components
    private final LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private boolean active;

    // Domain events
    private final List<Object> uncommittedEvents = new ArrayList<>();

    private Kit(String kitSku, String kitDescription, List<KitComponent> components,
               KitType kitType, boolean allowPartialKit) {
        if (kitSku == null || kitSku.isBlank()) {
            throw new IllegalArgumentException("Kit SKU cannot be null or blank");
        }
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("Kit must have at least one component");
        }
        if (kitType == null) {
            throw new IllegalArgumentException("Kit type cannot be null");
        }

        this.kitSku = kitSku;
        this.kitDescription = kitDescription;
        this.components = new ArrayList<>(components);
        this.kitType = kitType;
        this.allowPartialKit = allowPartialKit;
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public Kit(
        String kitSku,
        String kitDescription,
        List<KitComponent> components,
        KitType kitType,
        boolean allowPartialKit,
        LocalDateTime createdAt
    ) {
        this.kitSku = kitSku;
        this.kitDescription = kitDescription;
        this.components = components;
        this.kitType = kitType;
        this.allowPartialKit = allowPartialKit;
        this.createdAt = createdAt;
    }


    /**
     * Create a new kit definition
     */
    public static Kit create(String kitSku, String kitDescription, List<KitComponent> components,
                            KitType kitType, boolean allowPartialKit) {
        return new Kit(kitSku, kitDescription, components, kitType, allowPartialKit);
    }

    /**
     * Create physical kit (requires actual assembly)
     */
    public static Kit physical(String kitSku, String kitDescription, List<KitComponent> components) {
        return new Kit(kitSku, kitDescription, components, KitType.PHYSICAL, false);
    }

    /**
     * Create virtual kit (logical grouping, no assembly)
     */
    public static Kit virtual(String kitSku, String kitDescription, List<KitComponent> components,
                             boolean allowPartialKit) {
        return new Kit(kitSku, kitDescription, components, KitType.VIRTUAL, allowPartialKit);
    }

    /**
     * Add a component to the kit
     */
    public void addComponent(KitComponent component) {
        if (component == null) {
            throw new IllegalArgumentException("Component cannot be null");
        }

        // Check if component already exists
        Optional<KitComponent> existing = components.stream()
            .filter(c -> c.getComponentSku().equals(component.getComponentSku()))
            .findFirst();

        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                "Component " + component.getComponentSku() + " already exists in kit");
        }

        components.add(component);
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Remove a component from the kit
     */
    public void removeComponent(String componentSku) {
        boolean removed = components.removeIf(c -> c.getComponentSku().equals(componentSku));

        if (!removed) {
            throw new IllegalArgumentException("Component " + componentSku + " not found in kit");
        }

        if (components.isEmpty()) {
            throw new IllegalStateException("Cannot remove last component from kit");
        }

        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Update component quantity
     */
    public void updateComponentQuantity(String componentSku, int newQuantity) {
        KitComponent component = components.stream()
            .filter(c -> c.getComponentSku().equals(componentSku))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Component " + componentSku + " not found in kit"));

        components.remove(component);
        components.add(component.withQuantity(newQuantity));

        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Calculate total number of kits that can be assembled from available inventory
     */
    public int calculateAvailableKits(Map<String, Integer> componentInventory) {
        if (componentInventory == null || componentInventory.isEmpty()) {
            return 0;
        }

        int minKits = Integer.MAX_VALUE;

        for (KitComponent component : components) {
            if (!component.isOptional()) {
                int available = componentInventory.getOrDefault(component.getComponentSku(), 0);
                int possibleKits = available / component.getQuantity();
                minKits = Math.min(minKits, possibleKits);
            }
        }

        return minKits == Integer.MAX_VALUE ? 0 : minKits;
    }

    /**
     * Check if kit can be assembled with available components
     */
    public boolean canAssemble(Map<String, Integer> componentInventory, int kitQuantity) {
        for (KitComponent component : components) {
            if (!component.isOptional()) {
                int required = component.getQuantity() * kitQuantity;
                int available = componentInventory.getOrDefault(component.getComponentSku(), 0);

                if (available < required) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Get shortage details for kit assembly
     */
    public Map<String, Integer> getShortages(Map<String, Integer> componentInventory, int kitQuantity) {
        Map<String, Integer> shortages = new HashMap<>();

        for (KitComponent component : components) {
            if (!component.isOptional()) {
                int required = component.getQuantity() * kitQuantity;
                int available = componentInventory.getOrDefault(component.getComponentSku(), 0);

                if (available < required) {
                    shortages.put(component.getComponentSku(), required - available);
                }
            }
        }

        return shortages;
    }

    /**
     * Deactivate kit
     */
    public void deactivate() {
        this.active = false;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Activate kit
     */
    public void activate() {
        this.active = true;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * Check if kit is physical (requires assembly)
     */
    public boolean isPhysical() {
        return kitType == KitType.PHYSICAL;
    }

    /**
     * Check if kit is virtual (logical grouping only)
     */
    public boolean isVirtual() {
        return kitType == KitType.VIRTUAL;
    }

    /**
     * Get total number of unique components
     */
    public int getComponentCount() {
        return components.size();
    }

    /**
     * Get component by SKU
     */
    public Optional<KitComponent> getComponent(String componentSku) {
        return components.stream()
            .filter(c -> c.getComponentSku().equals(componentSku))
            .findFirst();
    }

    public String getKitSku() {
        return kitSku;
    }

    public String getKitDescription() {
        return kitDescription;
    }

    public List<KitComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public KitType getKitType() {
        return kitType;
    }

    public boolean isAllowPartialKit() {
        return allowPartialKit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public boolean isActive() {
        return active;
    }

    public List<Object> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
}
