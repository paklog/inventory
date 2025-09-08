package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.InventoryLedgerEntry;

import java.time.LocalDateTime;

public interface InventoryLedgerRepository {
    InventoryLedgerEntry save(InventoryLedgerEntry entry);
    int findTotalQuantityPickedBySkuAndDateRange(String sku, LocalDateTime start, LocalDateTime end);
}