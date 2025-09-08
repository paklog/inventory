package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.infrastructure.persistence.mongodb.ProductStockDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductStockSpringRepository extends MongoRepository<ProductStockDocument, String> {
    Optional<ProductStockDocument> findBySku(String sku);
    void deleteAll(); // For test cleanup
}