package com.paklog.inventory.infrastructure.async;

import com.paklog.inventory.infrastructure.metrics.InventoryMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Async wrapper for metrics operations that don't need to block main request flow.
 * Metrics collection happens in background threads to improve response times.
 */
@Service
public class AsyncMetricsService {

    private static final Logger log = LoggerFactory.getLogger(AsyncMetricsService.class);

    private final InventoryMetricsService metricsService;

    public AsyncMetricsService(InventoryMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    /**
     * Asynchronously update inventory metrics after stock operations.
     * Runs in background to avoid blocking the main transaction.
     */
    @Async("metricsExecutor")
    public void updateInventoryMetricsAsync(String sku,
                                           int previousOnHand,
                                           int previousAllocated,
                                           int currentOnHand,
                                           int currentAllocated) {
        try {
            log.debug("Async updating inventory metrics for sku: {}", sku);
            metricsService.updateInventoryMetrics(sku, previousOnHand, previousAllocated,
                    currentOnHand, currentAllocated);
        } catch (Exception e) {
            log.error("Failed to update inventory metrics asynchronously for sku: {}", sku, e);
            // Don't propagate - metrics failures shouldn't affect business operations
        }
    }

    /**
     * Asynchronously record stock adjustment metrics.
     */
    @Async("metricsExecutor")
    public void incrementStockAdjustmentAsync(String sku, int quantityChange, String reasonCode) {
        try {
            log.debug("Async recording stock adjustment for sku: {}", sku);
            metricsService.incrementStockAdjustment(sku, quantityChange, reasonCode);
        } catch (Exception e) {
            log.error("Failed to record stock adjustment metrics for sku: {}", sku, e);
        }
    }

    /**
     * Asynchronously record stock allocation metrics.
     */
    @Async("metricsExecutor")
    public void incrementStockAllocationAsync(String sku, int quantity) {
        try {
            log.debug("Async recording stock allocation for sku: {}", sku);
            metricsService.incrementStockAllocation(sku, quantity);
        } catch (Exception e) {
            log.error("Failed to record stock allocation metrics for sku: {}", sku, e);
        }
    }

    /**
     * Asynchronously record stock deallocation metrics.
     */
    @Async("metricsExecutor")
    public void incrementStockDeallocationAsync(String sku, int quantity) {
        try {
            log.debug("Async recording stock deallocation for sku: {}", sku);
            metricsService.incrementStockDeallocation(sku, quantity);
        } catch (Exception e) {
            log.error("Failed to record stock deallocation metrics for sku: {}", sku, e);
        }
    }

    /**
     * Asynchronously record stock receipt metrics.
     */
    @Async("metricsExecutor")
    public void incrementStockReceiptAsync(String sku, int quantity) {
        try {
            log.debug("Async recording stock receipt for sku: {}", sku);
            metricsService.incrementStockReceipt(sku, quantity);
        } catch (Exception e) {
            log.error("Failed to record stock receipt metrics for sku: {}", sku, e);
        }
    }

    /**
     * Asynchronously record item picked metrics.
     */
    @Async("metricsExecutor")
    public void incrementItemPickedAsync(String sku, int quantity) {
        try {
            log.debug("Async recording item picked for sku: {}", sku);
            metricsService.incrementItemPicked(sku, quantity);
        } catch (Exception e) {
            log.error("Failed to record item picked metrics for sku: {}", sku, e);
        }
    }

    /**
     * Asynchronously record stock level query metrics.
     */
    @Async("metricsExecutor")
    public void incrementStockLevelQueryAsync(String sku) {
        try {
            log.debug("Async recording stock level query for sku: {}", sku);
            metricsService.incrementStockLevelQuery(sku);
        } catch (Exception e) {
            log.error("Failed to record stock level query metrics for sku: {}", sku, e);
        

}
}
}
