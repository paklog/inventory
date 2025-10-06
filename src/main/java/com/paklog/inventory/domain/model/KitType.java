package com.paklog.inventory.domain.model;

/**
 * Types of kits supported in the system.
 */
public enum KitType {
    /**
     * Physical kit - components are physically assembled into a new unit.
     * Requires actual bundling/assembly work.
     * Inventory is consumed from components and added to kit SKU.
     */
    PHYSICAL,

    /**
     * Virtual kit - logical grouping of components without physical assembly.
     * Used for promotions, bundles sold as-is.
     * Inventory remains in component SKUs, allocated together.
     */
    VIRTUAL
}
