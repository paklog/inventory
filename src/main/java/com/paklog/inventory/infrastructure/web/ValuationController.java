package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.InitializeValuationRequest;
import com.paklog.inventory.application.service.ValuationService;
import com.paklog.inventory.domain.model.ValuationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * REST controller for inventory valuation management.
 * Provides endpoints for cost tracking, COGS calculation, and valuation queries.
 */
@RestController
@RequestMapping("/api/v1/inventory/valuation")
public class ValuationController {

    private static final Logger logger = LoggerFactory.getLogger(ValuationController.class);

    private final ValuationService valuationService;

    public ValuationController(ValuationService valuationService) {
        this.valuationService = valuationService;
    }

    /**
     * Initialize valuation for a SKU
     * POST /api/v1/inventory/valuation/initialize
     */
    @PostMapping("/initialize")
    public ResponseEntity<Void> initializeValuation(@RequestBody InitializeValuationRequest request) {
        logger.info("Initializing valuation: SKU={}, method={}, cost={}",
                request.sku(), request.valuationMethod(), request.initialUnitCost());

        valuationService.initializeValuation(
            request.sku(),
            request.valuationMethod(),
            request.initialUnitCost(),
            request.currency()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Update valuation on stock receipt
     * POST /api/v1/inventory/valuation/{sku}/receipt
     */
    @PostMapping("/{sku}/receipt")
    public ResponseEntity<Void> updateValuationOnReceipt(
            @PathVariable String sku,
            @RequestParam int quantityReceived,
            @RequestParam BigDecimal unitCost) {
        logger.info("Updating valuation on receipt: SKU={}, qty={}, cost={}",
                sku, quantityReceived, unitCost);

        valuationService.updateValuationOnReceipt(sku, quantityReceived, unitCost);
        return ResponseEntity.ok().build();
    }

    /**
     * Update valuation on stock issue (returns COGS)
     * POST /api/v1/inventory/valuation/{sku}/issue
     */
    @PostMapping("/{sku}/issue")
    public ResponseEntity<BigDecimal> updateValuationOnIssue(
            @PathVariable String sku,
            @RequestParam int quantityIssued) {
        logger.info("Updating valuation on issue: SKU={}, qty={}", sku, quantityIssued);

        BigDecimal cogs = valuationService.updateValuationOnIssue(sku, quantityIssued);
        return ResponseEntity.ok(cogs);
    }

    /**
     * Get current unit cost
     * GET /api/v1/inventory/valuation/{sku}/unit-cost
     */
    @GetMapping("/{sku}/unit-cost")
    public ResponseEntity<BigDecimal> getUnitCost(@PathVariable String sku) {
        logger.debug("Getting unit cost: SKU={}", sku);

        Optional<BigDecimal> unitCost = valuationService.getUnitCost(sku);
        return unitCost
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get total inventory value
     * GET /api/v1/inventory/valuation/{sku}/total-value
     */
    @GetMapping("/{sku}/total-value")
    public ResponseEntity<BigDecimal> getTotalValue(@PathVariable String sku) {
        logger.debug("Getting total value: SKU={}", sku);

        Optional<BigDecimal> totalValue = valuationService.getTotalValue(sku);
        return totalValue
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Calculate COGS (what-if analysis, no update)
     * GET /api/v1/inventory/valuation/{sku}/cogs
     */
    @GetMapping("/{sku}/cogs")
    public ResponseEntity<BigDecimal> calculateCOGS(
            @PathVariable String sku,
            @RequestParam int quantity) {
        logger.debug("Calculating COGS: SKU={}, qty={}", sku, quantity);

        BigDecimal cogs = valuationService.calculateCOGS(sku, quantity);
        return ResponseEntity.ok(cogs);
    }

    /**
     * Get inventory carrying cost
     * GET /api/v1/inventory/valuation/{sku}/carrying-cost
     */
    @GetMapping("/{sku}/carrying-cost")
    public ResponseEntity<BigDecimal> getCarryingCost(
            @PathVariable String sku,
            @RequestParam double annualCarryingCostPercentage) {
        logger.debug("Getting carrying cost: SKU={}, rate={}%",
                sku, annualCarryingCostPercentage);

        BigDecimal carryingCost = valuationService.getCarryingCost(sku, annualCarryingCostPercentage);
        return ResponseEntity.ok(carryingCost);
    }

    /**
     * Get valuation method
     * GET /api/v1/inventory/valuation/{sku}/method
     */
    @GetMapping("/{sku}/method")
    public ResponseEntity<ValuationMethod> getValuationMethod(@PathVariable String sku) {
        logger.debug("Getting valuation method: SKU={}", sku);

        Optional<ValuationMethod> method = valuationService.getValuationMethod(sku);
        return method
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
