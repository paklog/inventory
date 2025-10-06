package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.infrastructure.persistence.mongodb.ProductStockDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductStockSpringRepository extends MongoRepository<ProductStockDocument, String> {
    // Optimized with index hint on sku field
    @Query("{ 'sku': ?0 }")
    Optional<ProductStockDocument> findBySku(String sku);

    // Optimized query to fetch only SKU field for SKU listing
    @Query(value = "{}", fields = "{ 'sku': 1 }")
    List<ProductStockDocument> findAllSkusProjection();

    void deleteAll(); // For test cleanup
}