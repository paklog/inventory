package com.paklog.inventory.application.service;

import com.paklog.inventory.application.dto.InventoryHealthMetricsResponse;
import com.paklog.inventory.application.dto.StockLevelResponse;
import com.paklog.inventory.domain.exception.ProductStockNotFoundException;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.infrastructure.metrics.InventoryMetricsService;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryQueryService {

    private final ProductStockRepository productStockRepository;
    private final InventoryMetricsService metricsService;

    public InventoryQueryService(ProductStockRepository productStockRepository, InventoryMetricsService metricsService) {
        this.productStockRepository = productStockRepository;
        this.metricsService = metricsService;
    }

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
            // This is a simplified implementation.
            // A real implementation would involve more complex queries, potentially
            // aggregating data from InventoryLedgerEntry and other sources.
            // For now, we'll return some dummy data or basic calculations.

            List<ProductStock> allProductStocks = productStockRepository.findAll(); // Assuming findAll exists or can be added

            long totalSkus = allProductStocks.size();
            long outOfStockSkus = allProductStocks.stream()
                    .filter(ps -> ps.getQuantityOnHand() == 0)
                    .count();

            // Dummy inventory turnover and dead stock for demonstration
            double inventoryTurnover = totalSkus > 0 ? (double) (totalSkus * 4) / totalSkus : 0.0; // Example: 4 turns per year
            List<String> deadStockSkus = allProductStocks.stream()
                    .filter(ps -> ps.getQuantityOnHand() > 0 && ps.getQuantityAllocated() == 0 && ps.getLastUpdated().isBefore(java.time.LocalDateTime.now().minusMonths(6)))
                    .map(ProductStock::getSku)
                    .collect(Collectors.toList());

            metricsService.stopQueryOperation(sample, "health_metrics");
            return InventoryHealthMetricsResponse.of(inventoryTurnover, deadStockSkus, totalSkus, outOfStockSkus);
        } catch (Exception e) {
            metricsService.stopQueryOperation(sample, "health_metrics_error");
            throw e;
        }
    }
}