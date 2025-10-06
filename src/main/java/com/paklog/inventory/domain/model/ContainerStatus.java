package com.paklog.inventory.domain.model;

/**
 * Status lifecycle for containers/LPNs.
 */
public enum ContainerStatus {

    /**
     * Container is active and can receive items
     */
    ACTIVE,

    /**
     * Container is closed, no more items can be added
     */
    CLOSED,

    /**
     * Container is empty (all items removed)
     */
    EMPTY,

    /**
     * Container has been shipped
     */
    SHIPPED,

    /**
     * Container is damaged and unusable
     */
    DAMAGED,

    /**
     * Container is on hold (quality, inspection)
     */
    ON_HOLD;

    /**
     * Can items be added to this container?
     */
    public boolean canAddItems() {
        return this == ACTIVE;
    }

    /**
     * Can this container be moved?
     */
    public boolean canBeMoved() {
        return this == ACTIVE || this == CLOSED;
    }

    /**
     * Is this a final status?
     */
    public boolean isFinalStatus() {
        return this == SHIPPED || this == DAMAGED;
    }
}
