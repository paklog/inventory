package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.InventoryLedgerEntry;

public interface InventoryLedgerRepository {
    InventoryLedgerEntry save(InventoryLedgerEntry entry);
}