package com.paklog.inventory.domain.model;

public enum ChangeType {
    ALLOCATION,
    DEALLOCATION,
    PICK,
    RECEIPT,
    ADJUSTMENT,
    ADJUSTMENT_POSITIVE,
    ADJUSTMENT_NEGATIVE,
    CYCLE_COUNT
}