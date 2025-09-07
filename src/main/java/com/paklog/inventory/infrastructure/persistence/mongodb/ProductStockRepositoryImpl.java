package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProductStockRepositoryImpl implements ProductStockRepository {

    private final ProductStockSpringRepository springRepository;

    public ProductStockRepositoryImpl(ProductStockSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Optional<ProductStock> findBySku(String sku) {
        return springRepository.findBySku(sku);
    }

    @Override
    public ProductStock save(ProductStock productStock) {
        return springRepository.save(productStock);
    }

    @Override
    public java.util.List<ProductStock> findAll() {
        return springRepository.findAll();
    }

    @Override
    public void deleteAll() {
        springRepository.deleteAll();
    }
}