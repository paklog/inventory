package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.PlaceHoldRequest;
import com.paklog.inventory.application.service.InventoryHoldService;
import com.paklog.inventory.domain.model.HoldType;
import com.paklog.inventory.domain.model.InventoryHold;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for inventory hold management.
 * Provides endpoints for placing, releasing, and querying inventory holds.
 */
@RestController
@RequestMapping("/api/v1/inventory/holds")
public class InventoryHoldController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryHoldController.class);

    private final InventoryHoldService inventoryHoldService;

    public InventoryHoldController(InventoryHoldService inventoryHoldService) {
        this.inventoryHoldService = inventoryHoldService;
    }

    /**
     * Place an inventory hold
     * POST /api/v1/inventory/holds
     */
    @PostMapping
    public ResponseEntity<String> placeHold(@RequestBody PlaceHoldRequest request) {
        logger.info("Placing hold: SKU={}, type={}, qty={}",
                request.sku(), request.holdType(), request.quantity());

        String holdId = inventoryHoldService.placeHold(
            request.sku(),
            request.holdType(),
            request.quantity(),
            request.reason(),
            request.placedBy()
        );

        return ResponseEntity.ok(holdId);
    }

    /**
     * Place a quality hold
     * POST /api/v1/inventory/holds/{sku}/quality
     */
    @PostMapping("/{sku}/quality")
    public ResponseEntity<String> placeQualityHold(
            @PathVariable String sku,
            @RequestParam int quantity,
            @RequestParam String reason,
            @RequestParam String placedBy) {
        logger.info("Placing quality hold: SKU={}, qty={}", sku, quantity);

        String holdId = inventoryHoldService.placeQualityHold(sku, quantity, reason, placedBy);
        return ResponseEntity.ok(holdId);
    }

    /**
     * Place a credit hold
     * POST /api/v1/inventory/holds/{sku}/credit
     */
    @PostMapping("/{sku}/credit")
    public ResponseEntity<String> placeCreditHold(
            @PathVariable String sku,
            @RequestParam int quantity,
            @RequestParam String customerId,
            @RequestParam String placedBy) {
        logger.info("Placing credit hold: SKU={}, qty={}, customer={}",
                sku, quantity, customerId);

        String holdId = inventoryHoldService.placeCreditHold(sku, quantity, customerId, placedBy);
        return ResponseEntity.ok(holdId);
    }

    /**
     * Release a specific hold
     * DELETE /api/v1/inventory/holds/{holdId}
     */
    @DeleteMapping("/{holdId}")
    public ResponseEntity<Void> releaseHold(@PathVariable String holdId) {
        logger.info("Releasing hold: holdId={}", holdId);

        inventoryHoldService.releaseHold(holdId);
        return ResponseEntity.ok().build();
    }

    /**
     * Release all expired holds (scheduled job endpoint)
     * POST /api/v1/inventory/holds/release-expired
     */
    @PostMapping("/release-expired")
    public ResponseEntity<Integer> releaseExpiredHolds() {
        logger.info("Releasing expired holds");

        int count = inventoryHoldService.releaseExpiredHolds();
        return ResponseEntity.ok(count);
    }

    /**
     * Get active holds for a SKU
     * GET /api/v1/inventory/holds/{sku}
     */
    @GetMapping("/{sku}")
    public ResponseEntity<List<InventoryHold>> getActiveHolds(@PathVariable String sku) {
        logger.debug("Getting active holds: SKU={}", sku);

        List<InventoryHold> holds = inventoryHoldService.getActiveHolds(sku);
        return ResponseEntity.ok(holds);
    }

    /**
     * Get total held quantity for a SKU
     * GET /api/v1/inventory/holds/{sku}/total
     */
    @GetMapping("/{sku}/total")
    public ResponseEntity<Integer> getTotalHeldQuantity(@PathVariable String sku) {
        logger.debug("Getting total held quantity: SKU={}", sku);

        int total = inventoryHoldService.getTotalHeldQuantity(sku);
        return ResponseEntity.ok(total);
    }

    /**
     * Get holds by type
     * GET /api/v1/inventory/holds/{sku}/type/{holdType}
     */
    @GetMapping("/{sku}/type/{holdType}")
    public ResponseEntity<List<InventoryHold>> getHoldsByType(
            @PathVariable String sku,
            @PathVariable HoldType holdType) {
        logger.debug("Getting holds by type: SKU={}, type={}", sku, holdType);

        List<InventoryHold> holds = inventoryHoldService.getHoldsByType(sku, holdType);
        return ResponseEntity.ok(holds);
    }

    /**
     * Check if SKU has active holds
     * GET /api/v1/inventory/holds/{sku}/has-active
     */
    @GetMapping("/{sku}/has-active")
    public ResponseEntity<Boolean> hasActiveHolds(@PathVariable String sku) {
        logger.debug("Checking for active holds: SKU={}", sku);

        boolean hasHolds = inventoryHoldService.hasActiveHolds(sku);
        return ResponseEntity.ok(hasHolds);
    }
}
