package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.ClassifySKURequest;
import com.paklog.inventory.application.service.ABCClassificationService;
import com.paklog.inventory.domain.model.ABCClass;
import com.paklog.inventory.domain.model.ABCClassification;
import com.paklog.inventory.domain.model.ABCCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for ABC classification management.
 * Provides endpoints for classification, re-classification, and queries.
 */
@RestController
@RequestMapping("/api/v1/inventory/abc-classification")
public class ABCClassificationController {

    private static final Logger logger = LoggerFactory.getLogger(ABCClassificationController.class);

    private final ABCClassificationService abcClassificationService;

    public ABCClassificationController(ABCClassificationService abcClassificationService) {
        this.abcClassificationService = abcClassificationService;
    }

    /**
     * Classify a SKU
     * POST /api/v1/inventory/abc-classification/classify
     */
    @PostMapping("/classify")
    public ResponseEntity<Void> classifySKU(@RequestBody ClassifySKURequest request) {
        logger.info("Classifying SKU: sku={}, criteria={}", request.sku(), request.criteria());

        abcClassificationService.classifySKU(
            request.sku(),
            request.annualUsageValue(),
            request.annualUsageQuantity(),
            request.unitCost(),
            request.velocityScore(),
            request.criticalityScore(),
            request.criteria()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Classify using combined criteria
     * POST /api/v1/inventory/abc-classification/{sku}/classify-combined
     */
    @PostMapping("/{sku}/classify-combined")
    public ResponseEntity<Void> classifyWithCombinedCriteria(
            @PathVariable String sku,
            @RequestParam java.math.BigDecimal annualUsageValue,
            @RequestParam int annualUsageQuantity,
            @RequestParam java.math.BigDecimal unitCost,
            @RequestParam double velocityScore,
            @RequestParam double criticalityScore) {
        logger.info("Classifying SKU with combined criteria: {}", sku);

        abcClassificationService.classifyWithCombinedCriteria(
            sku, annualUsageValue, annualUsageQuantity, unitCost,
            velocityScore, criticalityScore
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Classify using value-based criteria (traditional Pareto)
     * POST /api/v1/inventory/abc-classification/{sku}/classify-by-value
     */
    @PostMapping("/{sku}/classify-by-value")
    public ResponseEntity<Void> classifyByValue(
            @PathVariable String sku,
            @RequestParam java.math.BigDecimal annualUsageValue,
            @RequestParam int annualUsageQuantity,
            @RequestParam java.math.BigDecimal unitCost) {
        logger.info("Classifying SKU by value: {}", sku);

        abcClassificationService.classifyByValue(sku, annualUsageValue, annualUsageQuantity, unitCost);
        return ResponseEntity.ok().build();
    }

    /**
     * Classify using velocity-based criteria
     * POST /api/v1/inventory/abc-classification/{sku}/classify-by-velocity
     */
    @PostMapping("/{sku}/classify-by-velocity")
    public ResponseEntity<Void> classifyByVelocity(
            @PathVariable String sku,
            @RequestParam java.math.BigDecimal annualUsageValue,
            @RequestParam int annualUsageQuantity,
            @RequestParam java.math.BigDecimal unitCost,
            @RequestParam double velocityScore) {
        logger.info("Classifying SKU by velocity: {}", sku);

        abcClassificationService.classifyByVelocity(
            sku, annualUsageValue, annualUsageQuantity, unitCost, velocityScore
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Batch re-classify all SKUs
     * POST /api/v1/inventory/abc-classification/reclassify-all
     */
    @PostMapping("/reclassify-all")
    public ResponseEntity<Integer> reclassifyAll(@RequestParam ABCCriteria criteria) {
        logger.info("Starting batch re-classification with criteria={}", criteria);

        int count = abcClassificationService.reclassifyAll(criteria);
        return ResponseEntity.ok(count);
    }

    /**
     * Get ABC class for a SKU
     * GET /api/v1/inventory/abc-classification/{sku}/class
     */
    @GetMapping("/{sku}/class")
    public ResponseEntity<ABCClass> getABCClass(@PathVariable String sku) {
        logger.debug("Getting ABC class: SKU={}", sku);

        Optional<ABCClass> abcClass = abcClassificationService.getABCClass(sku);
        return abcClass
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get recommended cycle count frequency
     * GET /api/v1/inventory/abc-classification/{sku}/count-frequency
     */
    @GetMapping("/{sku}/count-frequency")
    public ResponseEntity<Integer> getRecommendedCountFrequency(@PathVariable String sku) {
        logger.debug("Getting recommended count frequency: SKU={}", sku);

        int frequency = abcClassificationService.getRecommendedCountFrequency(sku);
        return ResponseEntity.ok(frequency);
    }

    /**
     * Check if SKU requires re-classification
     * GET /api/v1/inventory/abc-classification/{sku}/requires-reclassification
     */
    @GetMapping("/{sku}/requires-reclassification")
    public ResponseEntity<Boolean> requiresReclassification(@PathVariable String sku) {
        logger.debug("Checking if SKU requires re-classification: {}", sku);

        boolean requires = abcClassificationService.requiresReclassification(sku);
        return ResponseEntity.ok(requires);
    }

    /**
     * Get all SKUs requiring re-classification
     * GET /api/v1/inventory/abc-classification/requiring-reclassification
     */
    @GetMapping("/requiring-reclassification")
    public ResponseEntity<List<String>> getSKUsRequiringReclassification() {
        logger.debug("Getting SKUs requiring re-classification");

        List<String> skus = abcClassificationService.getSKUsRequiringReclassification();
        return ResponseEntity.ok(skus);
    }

    /**
     * Get classification details
     * GET /api/v1/inventory/abc-classification/{sku}
     */
    @GetMapping("/{sku}")
    public ResponseEntity<ABCClassification> getClassification(@PathVariable String sku) {
        logger.debug("Getting classification details: SKU={}", sku);

        Optional<ABCClassification> classification = abcClassificationService.getClassification(sku);
        return classification
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
