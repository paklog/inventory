package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.*;
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

    public InventoryController(InventoryCommandService commandService,
                               InventoryQueryService queryService,
                               BulkAllocationService bulkAllocationService) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.bulkAllocationService = bulkAllocationService;
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
}
