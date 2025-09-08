package com.paklog.inventory.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockLocationSpringRepository extends MongoRepository<StockLocationDocument, String> {
    List<StockLocationDocument> findBySku(String sku);
}
