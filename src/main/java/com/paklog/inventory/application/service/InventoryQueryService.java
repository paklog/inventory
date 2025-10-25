package com.paklog.inventory.application.service;

import com.paklog.inventory.application.dto.InventoryHealthMetricsResponse;
import com.paklog.inventory.application.dto.StockLevelResponse;
import com.paklog.inventory.domain.exception.ProductStockNotFoundException;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.repository.InventoryLedgerRepository;
import com.paklog.inventory.infrastructure.cache.CacheConfiguration;
import com.paklog.inventory.infrastructure.metrics.InventoryMetricsService;
import com.paklog.inventory.infrastructure.persistence.mongodb.OptimizedInventoryLedgerRepository;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryQueryService {

    private final ProductStockRepository productStockRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final OptimizedInventoryLedgerRepository optimizedInventoryLedgerRepository;
    private final InventoryMetricsService metricsService;

    public InventoryQueryService(ProductStockRepository productStockRepository,
                                 InventoryLedgerRepository inventoryLedgerRepository,
                                 OptimizedInventoryLedgerRepository optimizedInventoryLedgerRepository,
                                 InventoryMetricsService metricsService) {
        this.productStockRepository = productStockRepository;
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.optimizedInventoryLedgerRepository = optimizedInventoryLedgerRepository;
        this.metricsService = metricsService;
    }

    @Cacheable(value = CacheConfiguration.PRODUCT_STOCK_CACHE, key = "#sku", unless = "#result == null")
    public StockLevelResponse getStockLevel(String sku) {
        Timer.Sample sample = metricsService.startQueryOperation();

        try {
            ProductStock productStock = productStockRepository.findBySku(sku)
                    .orElseThrow(() -> new ProductStockNotFoundException(sku));

            metricsService.incrementStockLevelQuery(sku);
            metricsService.stopQueryOperation(sample, "stock_level");

            return StockLevelResponse.fromDomain(productStock);
        } catch (Exception e) {
            metricsService.stopQueryOperation(sample, "stock_level_error");
            throw e;
        }
    }

    public InventoryHealthMetricsResponse getInventoryHealthMetrics(String category, LocalDate startDate, LocalDate endDate) {
        Timer.Sample sample = metricsService.startQueryOperation();
        try {
            // Handle null dates with defaults (e.g., last 30 days if not specified)
            LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusDays(30);
            LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();

            List<String> allSkus = productStockRepository.findAllSkus();
            long totalSkus = allSkus.size();

            List<ProductStock> allProductStocks = productStockRepository.findAll();

            long outOfStockSkus = allProductStocks.stream()
                    .filter(ps -> ps.getQuantityOnHand() == 0)
                    .count();

            // Optimized: Use aggregation pipeline for batch query to avoid N+1 problem
            // Server-side aggregation significantly reduces network transfer and client processing
            java.util.Map<String, Integer> pickedQuantitiesBySku = optimizedInventoryLedgerRepository
                    .findTotalQuantityPickedBySkusUsingAggregation(allSkus, effectiveStartDate.atStartOfDay(), effectiveEndDate.atTime(LocalTime.MAX));

            List<String> deadStockSkus = allSkus.stream()
                    .filter(sku -> pickedQuantitiesBySku.getOrDefault(sku, 0) == 0)
                    .collect(Collectors.toList());

            int totalPicked = pickedQuantitiesBySku.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            long totalOnhand = allProductStocks.stream()
                    .mapToLong(ProductStock::getQuantityOnHand)
                    .sum();

            double inventoryTurnover = totalOnhand > 0 ? (double) totalPicked / totalOnhand : 0.0;

            metricsService.stopQueryOperation(sample, "health_metrics");
            return InventoryHealthMetricsResponse.of(inventoryTurnover, deadStockSkus, totalSkus, outOfStockSkus);
        } catch (Exception e) {
            metricsService.stopQueryOperation(sample, "health_metrics_error");
            throw e;
        

}
}
}
