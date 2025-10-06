package com.paklog.inventory.domain.model;

/**
 * Reason for creating an inventory snapshot.
 */
public enum SnapshotReason {
    /**
     * Scheduled automated snapshot
     */
    SCHEDULED,

    /**
     * Manual snapshot requested by user
     */
    MANUAL,

    /**
     * Snapshot for audit purposes
     */
    AUDIT,

    /**
     * Snapshot for reconciliation
     */
    RECONCILIATION,

    /**
     * Snapshot for financial reporting
     */
    FINANCIAL_REPORTING,

    /**
     * Snapshot before major system change
     */
    SYSTEM_CHANGE,

    /**
     * Snapshot for debugging/investigation
     */
    INVESTIGATION
}
