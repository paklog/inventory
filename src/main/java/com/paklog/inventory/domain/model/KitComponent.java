package com.paklog.inventory.domain.model;

import java.util.Objects;

/**
 * Value object representing a component in a kit.
 * Defines what SKU and how many are needed for one kit.
 */
public class KitComponent {

    private final String componentSku;
    private final int quantity;           // Quantity needed per kit
    private final boolean optional;       // Is this component optional?
    private final boolean substitutable;  // Can be substituted with alternative SKU?
    private final String substituteGroup; // Group ID for substitutable components

    private KitComponent(String componentSku, int quantity, boolean optional,
                        boolean substitutable, String substituteGroup) {
        if (componentSku == null || componentSku.isBlank()) {
            throw new IllegalArgumentException("Component SKU cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        this.componentSku = componentSku;
        this.quantity = quantity;
        this.optional = optional;
        this.substitutable = substitutable;
        this.substituteGroup = substituteGroup;
    }

    /**
     * Create a required component
     */
    public static KitComponent required(String componentSku, int quantity) {
        return new KitComponent(componentSku, quantity, false, false, null);
    }

    /**
     * Create an optional component
     */
    public static KitComponent optional(String componentSku, int quantity) {
        return new KitComponent(componentSku, quantity, true, false, null);
    }

    /**
     * Create a substitutable component
     */
    public static KitComponent substitutable(String componentSku, int quantity, String substituteGroup) {
        if (substituteGroup == null || substituteGroup.isBlank()) {
            throw new IllegalArgumentException("Substitute group cannot be null or blank for substitutable components");
        }
        return new KitComponent(componentSku, quantity, false, true, substituteGroup);
    }

    /**
     * Create component with all attributes
     */
    public static KitComponent of(String componentSku, int quantity, boolean optional,
                                  boolean substitutable, String substituteGroup) {
        return new KitComponent(componentSku, quantity, optional, substitutable, substituteGroup);
    }

    /**
     * Update quantity (returns new instance - immutable)
     */
    public KitComponent withQuantity(int newQuantity) {
        return new KitComponent(this.componentSku, newQuantity, this.optional,
            this.substitutable, this.substituteGroup);
    }

    /**
     * Mark as optional (returns new instance - immutable)
     */
    public KitComponent asOptional() {
        return new KitComponent(this.componentSku, this.quantity, true,
            this.substitutable, this.substituteGroup);
    }

    /**
     * Mark as required (returns new instance - immutable)
     */
    public KitComponent asRequired() {
        return new KitComponent(this.componentSku, this.quantity, false,
            this.substitutable, this.substituteGroup);
    }

    /**
     * Calculate total quantity needed for multiple kits
     */
    public int getTotalQuantityFor(int kitCount) {
        return quantity * kitCount;
    }

    public String getComponentSku() {
        return componentSku;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isSubstitutable() {
        return substitutable;
    }

    public String getSubstituteGroup() {
        return substituteGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KitComponent that = (KitComponent) o;
        return Objects.equals(componentSku, that.componentSku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentSku);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(quantity).append("x ").append(componentSku);

        if (optional) sb.append(" (optional)");
        if (substitutable) sb.append(" (substitutable: ").append(substituteGroup).append(")");

        return sb.toString();
    }
}
