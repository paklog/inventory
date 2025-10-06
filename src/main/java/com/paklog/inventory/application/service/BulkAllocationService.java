package com.paklog.inventory.application.service;

import com.paklog.inventory.application.dto.AllocationRequestItem;
import com.paklog.inventory.application.dto.AllocationResult;
import com.paklog.inventory.application.dto.BulkAllocationRequest;
import com.paklog.inventory.application.dto.BulkAllocationResponse;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling bulk allocation operations.
 * Optimized for high-volume order allocation (10,000+ orders).
 */
@Service
public class BulkAllocationService {

    private static final Logger log = LoggerFactory.getLogger(BulkAllocationService.class);

    private final ProductStockRepository productStockRepository;
    private final InventoryCommandService inventoryCommandService;

    public BulkAllocationService(ProductStockRepository productStockRepository,
                                InventoryCommandService inventoryCommandService) {
        this.productStockRepository = productStockRepository;
        this.inventoryCommandService = inventoryCommandService;
    }

    /**
     * Process bulk allocation requests in a single transaction.
     * Optimized to minimize database round trips.
     */
    @Transactional
    public BulkAllocationResponse allocateBulk(BulkAllocationRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing bulk allocation request with {} items", request.getRequests().size());

        List<AllocationResult> results = new ArrayList<>();

        // Group requests by SKU to minimize database queries
        Map<String, List<AllocationRequestItem>> requestsBySku = request.getRequests().stream()
                .collect(Collectors.groupingBy(AllocationRequestItem::getSku));

        log.info("Grouped requests into {} unique SKUs", requestsBySku.size());

        // Batch load all ProductStocks
        Set<String> allSkus = requestsBySku.keySet();
        Map<String, ProductStock> stocksBySku = loadProductStocks(allSkus);

        log.info("Loaded {} product stocks from database", stocksBySku.size());

        // Process allocations by SKU
        for (Map.Entry<String, List<AllocationRequestItem>> entry : requestsBySku.entrySet()) {
            String sku = entry.getKey();
            List<AllocationRequestItem> skuRequests = entry.getValue();

            ProductStock stock = stocksBySku.get(sku);

            if (stock == null) {
                // SKU not found - all requests for this SKU fail
                for (AllocationRequestItem item : skuRequests) {
                    results.add(AllocationResult.failure(item.getOrderId(), sku,
                            "Product stock not found for SKU: " + sku));
                }
                continue;
            }

            // Sort by priority if specified (1 = highest priority)
            List<AllocationRequestItem> sortedRequests = skuRequests.stream()
                    .sorted(Comparator.comparing(
                            item -> item.getPriority() != null ? item.getPriority() : 999))
                    .toList();

            // Process allocations for this SKU
            for (AllocationRequestItem item : sortedRequests) {
                try {
                    // Check if using FEFO strategy
                    if (item.getUseFEFO() && stock.isLotTracked()) {
                        stock.allocateWithFEFO(item.getQuantity());
                    } else {
                        stock.allocate(item.getQuantity());
                    }

                    results.add(AllocationResult.success(item.getOrderId(), sku, item.getQuantity()));

                    log.debug("Allocated {} units of {} for order {}",
                            item.getQuantity(), sku, item.getOrderId());

                } catch (Exception e) {
                    results.add(AllocationResult.failure(item.getOrderId(), sku, e.getMessage()));

                    log.warn("Failed to allocate {} units of {} for order {}: {}",
                            item.getQuantity(), sku, item.getOrderId(), e.getMessage());
                }
            }
        }

        // Bulk save all updated stocks
        productStockRepository.saveAll(stocksBySku.values());

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        BulkAllocationResponse response = BulkAllocationResponse.of(results, processingTime);

        log.info("Bulk allocation completed: {} total, {} successful, {} failed, processing time: {}ms",
                response.getTotalRequests(), response.getSuccessfulAllocations(),
                response.getFailedAllocations(), processingTime);

        return response;
    }

    /**
     * Load product stocks in batch to minimize database queries
     */
    private Map<String, ProductStock> loadProductStocks(Set<String> skus) {
        Map<String, ProductStock> result = new HashMap<>();

        for (String sku : skus) {
            productStockRepository.findBySku(sku).ifPresent(stock -> result.put(sku, stock));
        }

        return result;
    }

    /**
     * Optimized version with parallel processing (use with caution)
     */
    @Transactional
    public BulkAllocationResponse allocateBulkParallel(BulkAllocationRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing PARALLEL bulk allocation request with {} items", request.getRequests().size());

        // Note: Parallel processing requires careful handling of transactions
        // This is a simplified example - production use would need more sophisticated locking

        List<AllocationResult> results = request.getRequests().parallelStream()
                .map(item -> {
                    try {
                        inventoryCommandService.allocateStock(item.getSku(), item.getQuantity(), item.getOrderId());
                        return AllocationResult.success(item.getOrderId(), item.getSku(), item.getQuantity());
                    } catch (Exception e) {
                        return AllocationResult.failure(item.getOrderId(), item.getSku(), e.getMessage());
                    }
                })
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        BulkAllocationResponse response = BulkAllocationResponse.of(results, endTime - startTime);

        log.info("PARALLEL bulk allocation completed in {}ms", response.getProcessingTimeMs());
        return response;
    }
}
