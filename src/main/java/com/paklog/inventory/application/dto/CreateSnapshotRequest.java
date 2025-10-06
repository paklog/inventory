package com.paklog.inventory.application.dto;

public record CreateSnapshotRequest(
    String sku,
    String snapshotType,  // DAILY, MONTH_END, QUARTER_END, YEAR_END, AD_HOC
    String reason,        // SCHEDULED, MANUAL, AUDIT, RECONCILIATION, etc.
    String createdBy
) {}
