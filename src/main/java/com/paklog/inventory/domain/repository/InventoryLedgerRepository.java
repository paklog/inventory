package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.InventoryLedgerEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface InventoryLedgerRepository {
    InventoryLedgerEntry save(InventoryLedgerEntry entry);
    List<InventoryLedgerEntry> saveAll(Iterable<InventoryLedgerEntry> entries);
    List<InventoryLedgerEntry> findAll();
    List<InventoryLedgerEntry> findBySku(String sku);
    List<InventoryLedgerEntry> findBySkuAndTimestampBetween(String sku, LocalDateTime start, LocalDateTime end);
    List<InventoryLedgerEntry> findByChangeType(com.paklog.inventory.domain.model.ChangeType changeType);
    int findTotalQuantityPickedBySkuAndDateRange(String sku, LocalDateTime start, LocalDateTime end);
    Map<String, Integer> findTotalQuantityPickedBySkusAndDateRange(List<String> skus, LocalDateTime start, LocalDateTime end);
    void deleteAll();
}