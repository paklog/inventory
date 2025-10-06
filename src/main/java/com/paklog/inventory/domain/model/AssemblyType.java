package com.paklog.inventory.domain.model;

/**
 * Type of assembly operation.
 */
public enum AssemblyType {
    /**
     * Assembly - combine components into a kit
     * Components → Kit SKU
     */
    ASSEMBLE,

    /**
     * Disassembly - break kit into components
     * Kit SKU → Components
     */
    DISASSEMBLE
}
