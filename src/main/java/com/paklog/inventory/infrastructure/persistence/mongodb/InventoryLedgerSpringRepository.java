package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.InventoryLedgerEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryLedgerSpringRepository extends MongoRepository<InventoryLedgerEntry, String> {
}