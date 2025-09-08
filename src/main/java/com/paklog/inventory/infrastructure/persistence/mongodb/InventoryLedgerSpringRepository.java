package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.infrastructure.persistence.mongodb.InventoryLedgerEntryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryLedgerSpringRepository extends MongoRepository<InventoryLedgerEntryDocument, String> {
    @Query("{ 'sku': ?0, 'changeType': 'PICK', 'timestamp': { '$gte': ?1, '$lte': ?2 } }")
    List<InventoryLedgerEntryDocument> findPicksBySkuAndDateRange(String sku, LocalDateTime start, LocalDateTime end);
}