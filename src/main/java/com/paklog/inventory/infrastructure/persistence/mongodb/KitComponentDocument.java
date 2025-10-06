package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.KitComponent;

/**
 * Embedded document for KitComponent value object.
 */
public class KitComponentDocument {

    private String componentSku;
    private int quantity;
    private boolean optional;
    private boolean substitutable;
    private String substituteGroup;

    public KitComponentDocument() {
    }

    public KitComponentDocument(String componentSku, int quantity, boolean optional,
                               boolean substitutable, String substituteGroup) {
        this.componentSku = componentSku;
        this.quantity = quantity;
        this.optional = optional;
        this.substitutable = substitutable;
        this.substituteGroup = substituteGroup;
    }

    public static KitComponentDocument fromDomain(KitComponent component) {
        return new KitComponentDocument(
            component.getComponentSku(),
            component.getQuantity(),
            component.isOptional(),
            component.isSubstitutable(),
            component.getSubstituteGroup()
        );
    }

    public KitComponent toDomain() {
        return KitComponent.of(componentSku, quantity, optional, substitutable, substituteGroup);
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

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isSubstitutable() {
        return substitutable;
    }

    public void setSubstitutable(boolean substitutable) {
        this.substitutable = substitutable;
    }

    public String getSubstituteGroup() {
        return substituteGroup;
    }

    public void setSubstituteGroup(String substituteGroup) {
        this.substituteGroup = substituteGroup;
    }
}
