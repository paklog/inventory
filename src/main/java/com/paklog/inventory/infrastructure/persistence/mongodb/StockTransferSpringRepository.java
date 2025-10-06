package com.paklog.inventory.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data MongoDB repository for StockTransferDocument
 */
@Repository
public interface StockTransferSpringRepository extends MongoRepository<StockTransferDocument, String> {

    List<StockTransferDocument> findBySku(String sku);

    List<StockTransferDocument> findByStatus(String status);

    List<StockTransferDocument> findBySkuAndStatus(String sku, String status);

    List<StockTransferDocument> findByInitiatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("{ 'status': 'COMPLETED', 'actualQuantityReceived': { $lt: '$quantity' } }")
    List<StockTransferDocument> findTransfersWithShrinkage();

    long countByStatus(String status);
}
