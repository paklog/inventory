package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.InventoryLedgerEntry;
import com.paklog.inventory.domain.repository.InventoryLedgerRepository;
import org.springframework.stereotype.Component;

@Component
public class InventoryLedgerRepositoryImpl implements InventoryLedgerRepository {

    private final InventoryLedgerSpringRepository springRepository;

    public InventoryLedgerRepositoryImpl(InventoryLedgerSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public InventoryLedgerEntry save(InventoryLedgerEntry entry) {
        return springRepository.save(entry);
    }
}