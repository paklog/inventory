package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.StockStatusChangeRequest;
import com.paklog.inventory.application.service.StockStatusService;
import com.paklog.inventory.domain.model.StockStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for stock status management.
 * Provides endpoints for changing stock status and querying by status.
 */
@RestController
@RequestMapping("/api/v1/inventory/stock-status")
public class StockStatusController {

    private static final Logger logger = LoggerFactory.getLogger(StockStatusController.class);

    private final StockStatusService stockStatusService;

    public StockStatusController(StockStatusService stockStatusService) {
        this.stockStatusService = stockStatusService;
    }

    /**
     * Change stock status for a SKU
     * POST /api/v1/inventory/stock-status/change
     */
    @PostMapping("/change")
    public ResponseEntity<Void> changeStockStatus(@RequestBody StockStatusChangeRequest request) {
        logger.info("Changing stock status: SKU={}, qty={}, from={}, to={}",
                request.sku(), request.quantity(), request.fromStatus(), request.toStatus());

        stockStatusService.changeStockStatus(
            request.sku(),
            request.quantity(),
            request.fromStatus(),
            request.toStatus(),
            request.reason()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Move stock to QUARANTINE
     * POST /api/v1/inventory/stock-status/{sku}/quarantine
     */
    @PostMapping("/{sku}/quarantine")
    public ResponseEntity<Void> moveToQuarantine(
            @PathVariable String sku,
            @RequestParam int quantity,
            @RequestParam String reason) {
        logger.info("Moving to quarantine: SKU={}, qty={}", sku, quantity);

        stockStatusService.moveToQuarantine(sku, quantity, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark stock as DAMAGED
     * POST /api/v1/inventory/stock-status/{sku}/damaged
     */
    @PostMapping("/{sku}/damaged")
    public ResponseEntity<Void> markAsDamaged(
            @PathVariable String sku,
            @RequestParam int quantity,
            @RequestParam String reason) {
        logger.info("Marking as damaged: SKU={}, qty={}", sku, quantity);

        stockStatusService.markAsDamaged(sku, quantity, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark stock as EXPIRED
     * POST /api/v1/inventory/stock-status/{sku}/expired
     */
    @PostMapping("/{sku}/expired")
    public ResponseEntity<Void> markAsExpired(
            @PathVariable String sku,
            @RequestParam int quantity) {
        logger.info("Marking as expired: SKU={}, qty={}", sku, quantity);

        stockStatusService.markAsExpired(sku, quantity);
        return ResponseEntity.ok().build();
    }

    /**
     * Release stock from QUARANTINE back to AVAILABLE
     * POST /api/v1/inventory/stock-status/{sku}/release-quarantine
     */
    @PostMapping("/{sku}/release-quarantine")
    public ResponseEntity<Void> releaseFromQuarantine(
            @PathVariable String sku,
            @RequestParam int quantity) {
        logger.info("Releasing from quarantine: SKU={}, qty={}", sku, quantity);

        stockStatusService.releaseFromQuarantine(sku, quantity);
        return ResponseEntity.ok().build();
    }

    /**
     * Get stock quantities by status
     * GET /api/v1/inventory/stock-status/{sku}
     */
    @GetMapping("/{sku}")
    public ResponseEntity<Map<StockStatus, Integer>> getStockByStatus(@PathVariable String sku) {
        logger.debug("Getting stock by status: SKU={}", sku);

        Map<StockStatus, Integer> stockByStatus = stockStatusService.getStockByStatus(sku);
        return ResponseEntity.ok(stockByStatus);
    }

    /**
     * Get quantity in specific status
     * GET /api/v1/inventory/stock-status/{sku}/{status}
     */
    @GetMapping("/{sku}/{status}")
    public ResponseEntity<Integer> getQuantityInStatus(
            @PathVariable String sku,
            @PathVariable StockStatus status) {
        logger.debug("Getting quantity in status: SKU={}, status={}", sku, status);

        int quantity = stockStatusService.getQuantityInStatus(sku, status);
        return ResponseEntity.ok(quantity);
    }
}
