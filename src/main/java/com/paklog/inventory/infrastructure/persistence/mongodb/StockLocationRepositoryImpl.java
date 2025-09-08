package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.StockLocation;
import com.paklog.inventory.domain.repository.StockLocationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class StockLocationRepositoryImpl implements StockLocationRepository {

    private final StockLocationSpringRepository springRepository;

    public StockLocationRepositoryImpl(StockLocationSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Optional<StockLocation> findBySkuAndLocation(String sku, Location location) {
        String id = sku + ":" + location.getAisle() + ":" + location.getShelf() + ":" + location.getBin();
        return springRepository.findById(id).map(StockLocationDocument::toDomain);
    }

    @Override
    public List<StockLocation> findBySku(String sku) {
        return springRepository.findBySku(sku).stream()
                .map(StockLocationDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public StockLocation save(StockLocation stockLocation) {
        StockLocationDocument doc = StockLocationDocument.fromDomain(stockLocation);
        springRepository.save(doc);
        return stockLocation;
    }
}
