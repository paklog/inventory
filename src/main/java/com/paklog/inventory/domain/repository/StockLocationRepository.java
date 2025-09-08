package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.StockLocation;

import java.util.List;
import java.util.Optional;

public interface StockLocationRepository {
    Optional<StockLocation> findBySkuAndLocation(String sku, Location location);
    List<StockLocation> findBySku(String sku);
    StockLocation save(StockLocation stockLocation);
}
