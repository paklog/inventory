package com.paklog.inventory.domain.model;

/**
 * Status of an assembly/disassembly order.
 */
public enum AssemblyStatus {
    /**
     * Order created, not yet started
     */
    CREATED,

    /**
     * Assembly/disassembly work in progress
     */
    IN_PROGRESS,

    /**
     * Completed successfully
     */
    COMPLETED,

    /**
     * Cancelled before completion
     */
    CANCELLED
}
