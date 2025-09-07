package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.ProductStock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductStockSpringRepository extends MongoRepository<ProductStock, String> {
    Optional<ProductStock> findBySku(String sku);
    void deleteAll(); // For test cleanup
}