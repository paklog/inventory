package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ProductStockRepositoryImpl implements ProductStockRepository {

    private final ProductStockSpringRepository springRepository;

    public ProductStockRepositoryImpl(ProductStockSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Optional<ProductStock> findBySku(String sku) {
        return springRepository.findBySku(sku).map(ProductStockDocument::toDomain);
    }

    @Override
    public ProductStock save(ProductStock productStock) {
        ProductStockDocument doc = ProductStockDocument.fromDomain(productStock);
        springRepository.save(doc);
        return productStock;
    }

    @Override
    public List<ProductStock> saveAll(Iterable<ProductStock> productStocks) {
        List<ProductStockDocument> docs = new java.util.ArrayList<>();
        for (ProductStock ps : productStocks) {
            docs.add(ProductStockDocument.fromDomain(ps));
        }
        springRepository.saveAll(docs);

        // Return the original list
        List<ProductStock> result = new java.util.ArrayList<>();
        productStocks.forEach(result::add);
        return result;
    }

    @Override
    public List<ProductStock> findAll() {
        return springRepository.findAll().stream()
                .map(ProductStockDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findAllSkus() {
        // Use optimized projection query to fetch only SKU field
        return springRepository.findAllSkusProjection().stream()
                .map(ProductStockDocument::getSku)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAll() {
        springRepository.deleteAll();
    }
}