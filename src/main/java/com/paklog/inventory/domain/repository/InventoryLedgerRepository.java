package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.InventoryLedgerEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface InventoryLedgerRepository {
    InventoryLedgerEntry save(InventoryLedgerEntry entry);
    int findTotalQuantityPickedBySkuAndDateRange(String sku, LocalDateTime start, LocalDateTime end);
    Map<String, Integer> findTotalQuantityPickedBySkusAndDateRange(List<String> skus, LocalDateTime start, LocalDateTime end);
}