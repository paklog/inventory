package com.paklog.inventory.domain.model;

/**
 * Status of a cycle count.
 */
public enum CountStatus {
    /**
     * Count is scheduled but not started
     */
    SCHEDULED,

    /**
     * Count is in progress
     */
    IN_PROGRESS,

    /**
     * Count completed with variance, pending approval
     */
    PENDING_APPROVAL,

    /**
     * Count approved and system adjusted
     */
    APPROVED,

    /**
     * Count rejected, needs recount
     */
    REJECTED,

    /**
     * Count completed with no variance
     */
    COMPLETED,

    /**
     * Count cancelled
     */
    CANCELLED
}
