package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.ProductStock;

import java.util.List;
import java.util.Optional;

public interface ProductStockRepository {
    Optional<ProductStock> findBySku(String sku);
    ProductStock save(ProductStock productStock);
    List<ProductStock> findAll();
    List<String> findAllSkus();
    void deleteAll();
}