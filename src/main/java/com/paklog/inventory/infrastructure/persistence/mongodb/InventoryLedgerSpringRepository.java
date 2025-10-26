package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.infrastructure.persistence.mongodb.InventoryLedgerEntryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryLedgerSpringRepository extends MongoRepository<InventoryLedgerEntryDocument, String> {
    List<InventoryLedgerEntryDocument> findBySku(String sku);

    List<InventoryLedgerEntryDocument> findBySkuAndTimestampBetween(String sku, LocalDateTime start, LocalDateTime end);

    List<InventoryLedgerEntryDocument> findByChangeType(String changeType);

    // Optimized with index hint for sku + changeType + timestamp compound index
    @Query(value = "{ 'sku': ?0, 'changeType': 'PICK', 'timestamp': { '$gte': ?1, '$lte': ?2 } }",
           fields = "{ 'sku': 1, 'quantityChange': 1, 'timestamp': 1 }")
    List<InventoryLedgerEntryDocument> findPicksBySkuAndDateRange(String sku, LocalDateTime start, LocalDateTime end);

    // Optimized with index hint and projection for bulk operations
    @Query(value = "{ 'sku': { '$in': ?0 }, 'changeType': 'PICK', 'timestamp': { '$gte': ?1, '$lte': ?2 } }",
           fields = "{ 'sku': 1, 'quantityChange': 1, 'timestamp': 1 }")
    List<InventoryLedgerEntryDocument> findPicksBySkusAndDateRange(List<String> skus, LocalDateTime start, LocalDateTime end);
}