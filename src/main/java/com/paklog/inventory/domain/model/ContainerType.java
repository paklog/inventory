package com.paklog.inventory.domain.model;

/**
 * Types of containers/packaging used in warehouse operations.
 */
public enum ContainerType {

    /**
     * Pallet - largest unit, typically 40-48 cases
     */
    PALLET("PLT", 2000),

    /**
     * Case - intermediate unit, typically 6-24 eaches
     */
    CASE("CASE", 100),

    /**
     * Tote/Bin - small container for picking
     */
    TOTE("TOTE", 50),

    /**
     * Each - individual unit (not really a container, but used for tracking)
     */
    EACH("EACH", 1),

    /**
     * Carton - shipping carton
     */
    CARTON("CTN", 50),

    /**
     * Bag - flexible container
     */
    BAG("BAG", 20),

    /**
     * Drum - for liquids/chemicals
     */
    DRUM("DRM", 200),

    /**
     * Rack - for long items (lumber, pipes)
     */
    RACK("RACK", 500);

    private final String prefix;
    private final int maxCapacity;

    ContainerType(String prefix, int maxCapacity) {
        this.prefix = prefix;
        this.maxCapacity = maxCapacity;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Check if this container type can be nested inside another
     */
    public boolean canBeNestedIn(ContainerType parent) {
        return this.ordinal() < parent.ordinal();
    }
}
