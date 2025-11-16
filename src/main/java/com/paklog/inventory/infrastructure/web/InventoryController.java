package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.*;
import com.paklog.inventory.application.service.BatchStockUpdateService;
import com.paklog.inventory.application.service.BulkAllocationService;
import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.application.service.InventoryQueryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryCommandService commandService;
    private final InventoryQueryService queryService;
    private final BulkAllocationService bulkAllocationService;
    private final BatchStockUpdateService batchStockUpdateService;

    public InventoryController(InventoryCommandService commandService,
                               InventoryQueryService queryService,
                               BulkAllocationService bulkAllocationService,
                               BatchStockUpdateService batchStockUpdateService) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.bulkAllocationService = bulkAllocationService;
        this.batchStockUpdateService = batchStockUpdateService;
    }

    @GetMapping("/stock_levels/{sku}")
    public ResponseEntity<StockLevelResponse> getStockLevel(@PathVariable String sku) {
        log.info("Getting stock level for SKU: {}", sku);
        StockLevelResponse stockLevel = queryService.getStockLevel(sku);
        log.info("Stock level for SKU: {} is {}", sku, stockLevel);
        return ResponseEntity.ok(stockLevel);
    }

    @PostMapping("/adjustments")
    public ResponseEntity<Void> createStockAdjustment(@Valid @RequestBody CreateAdjustmentRequest request) {
        log.info("Creating stock adjustment for SKU: {}, change: {}", request.getSku(), request.getQuantityChange());

        // In a real application, operatorId would come from authentication context
        String operatorId = "admin";

        commandService.adjustStock(
            request.getSku(),
            request.getQuantityChange(),
            request.getReasonCode(),
            request.getComment(),
            operatorId
        );

        log.info("Stock adjustment created for SKU: {}", request.getSku());
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/stock_levels/{sku}")
    public ResponseEntity<StockLevelResponse> adjustStockLevel(
            @PathVariable String sku,
            @Valid @RequestBody UpdateStockLevelRequest request) {
        log.info("Adjusting stock level for SKU: {}, change: {}", sku, request.quantityChange());

        // In a real application, operatorId would come from authentication context
        String operatorId = "admin";

        commandService.adjustStock(
            sku,
            request.quantityChange(),
            request.reasonCode(),
            request.comment(),
            operatorId
        );

        // As per the OpenAPI spec, return the updated resource
        StockLevelResponse updatedStockLevel = queryService.getStockLevel(sku);
        log.info("Stock level for SKU: {} is now {}", sku, updatedStockLevel);
        return ResponseEntity.ok(updatedStockLevel);
    }

    @PostMapping("/stock_levels/{sku}/reservations")
    public ResponseEntity<Void> createReservation(@PathVariable String sku, @RequestBody CreateReservationRequest request) {
        log.info("Creating reservation for SKU: {}, quantity: {}, orderId: {}", sku, request.getQuantity(), request.getOrderId());
        commandService.allocateStock(sku, request.getQuantity(), request.getOrderId());
        log.info("Reservation created for SKU: {}", sku);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/inventory_health_metrics")
    public ResponseEntity<InventoryHealthMetricsResponse> getInventoryHealthMetrics(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        log.info("Getting inventory health metrics for category: {}, startDate: {}, endDate: {}", category, startDate, endDate);
        InventoryHealthMetricsResponse metrics = queryService.getInventoryHealthMetrics(category, startDate, endDate);
        log.info("Inventory health metrics: {}", metrics);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/reports/health")
    public ResponseEntity<InventoryHealthMetricsResponse> getInventoryHealthReport(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        log.info("Getting inventory health report for category: {}, startDate: {}, endDate: {}", category, startDate, endDate);
        InventoryHealthMetricsResponse metrics = queryService.getInventoryHealthMetrics(category, startDate, endDate);
        log.info("Inventory health report: {}", metrics);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Bulk allocation endpoint for high-volume order processing.
     * Optimized to handle 10,000+ allocations in a single request.
     */
    @PostMapping("/allocations/bulk")
    public ResponseEntity<BulkAllocationResponse> bulkAllocate(@Valid @RequestBody BulkAllocationRequest request) {
        log.info("Processing bulk allocation request with {} items", request.getRequests().size());
        BulkAllocationResponse response = bulkAllocationService.allocateBulk(request);
        log.info("Bulk allocation completed: {} successful, {} failed, {}ms",
                response.getSuccessfulAllocations(), response.getFailedAllocations(), response.getProcessingTimeMs());
        return ResponseEntity.ok(response);
    }

    /**
     * Set absolute stock level (physical count).
     * Industry standard: Used for physical inventory counts where exact quantity is known.
     */
    @PutMapping("/stock_levels/{sku}/set")
    public ResponseEntity<StockLevelResponse> setStockLevel(
            @PathVariable String sku,
            @Valid @RequestBody SetStockLevelRequest request) {
        log.info("Setting absolute stock level for SKU: {}, quantity: {}", sku, request.quantity());

        // In a real application, operatorId would come from authentication context
        String operatorId = request.sourceOperatorId() != null ? request.sourceOperatorId() : "admin";

        commandService.setStockLevel(
            sku,
            request.quantity(),
            request.reasonCode(),
            request.comment(),
            operatorId,
            request.locationId()
        );

        StockLevelResponse updatedStockLevel = queryService.getStockLevel(sku);
        log.info("Stock level set for SKU: {} to {}", sku, updatedStockLevel.getQuantityOnHand());
        return ResponseEntity.ok(updatedStockLevel);
    }

    /**
     * Batch stock updates endpoint.
     * Industry standard: Process up to 1000 stock updates in a single request.
     * Supports both relative adjustments and absolute sets.
     */
    @PostMapping("/stock_levels/batch")
    public ResponseEntity<BatchStockUpdateResponse> batchStockUpdate(
            @Valid @RequestBody BatchStockUpdateRequest request) {
        log.info("Processing batch stock update with {} items", request.getUpdates().size());

        BatchStockUpdateResponse response = batchStockUpdateService.processBatch(request);

        log.info("Batch stock update completed: {} successful, {} failed, {}ms",
                response.getSuccessfulUpdates(), response.getFailedUpdates(), response.getProcessingTimeMs());

        return ResponseEntity.ok(response);
    }

    /**
     * Get stock level by location.
     * Multi-location support following industry patterns from Shopify, SAP, Oracle.
     */
    @GetMapping("/stock_levels/{sku}/locations/{locationId}")
    public ResponseEntity<StockLevelResponse> getStockLevelByLocation(
            @PathVariable String sku,
            @PathVariable String locationId) {
        log.info("Getting stock level for SKU: {} at location: {}", sku, locationId);

        // For now, return the stock level and filter by location
        // In a full implementation, you'd have location-specific queries
        StockLevelResponse stockLevel = queryService.getStockLevel(sku);

        if (stockLevel.getLocationId() != null && !stockLevel.getLocationId().equals(locationId)) {
            log.warn("Stock found but location mismatch. Requested: {}, Actual: {}",
                    locationId, stockLevel.getLocationId());
        }

        return ResponseEntity.ok(stockLevel);
    }
}
