package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.StockLocation;
import com.paklog.inventory.domain.repository.StockLocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PhysicalStockQueryService {

    private final StockLocationRepository stockLocationRepository;

    public PhysicalStockQueryService(StockLocationRepository stockLocationRepository) {
        this.stockLocationRepository = stockLocationRepository;
    }

    public List<StockLocation> findBySku(String sku) {
        return stockLocationRepository.findBySku(sku);
    }
}
