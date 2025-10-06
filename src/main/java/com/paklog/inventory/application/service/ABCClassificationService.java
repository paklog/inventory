package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.ABCClass;
import com.paklog.inventory.domain.model.ABCClassification;
import com.paklog.inventory.domain.model.ABCCriteria;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Application service for ABC classification management.
 * Handles classification calculation, assignment, and re-classification.
 */
@Service
public class ABCClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(ABCClassificationService.class);

    private final ProductStockRepository productStockRepository;
    private final OutboxRepository outboxRepository;

    public ABCClassificationService(ProductStockRepository productStockRepository,
                                   OutboxRepository outboxRepository) {
        this.productStockRepository = productStockRepository;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Classify a SKU based on metrics
     */
    @Transactional
    public void classifySKU(String sku, BigDecimal annualUsageValue, int annualUsageQuantity,
                           BigDecimal unitCost, double velocityScore, double criticalityScore,
                           ABCCriteria criteria) {
        logger.info("Classifying SKU: sku={}, criteria={}, annualValue={}",
                sku, criteria, annualUsageValue);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        ABCClassification classification = ABCClassification.classify(
                sku, annualUsageValue, annualUsageQuantity, unitCost,
                velocityScore, criticalityScore, criteria);

        productStock.setAbcClassification(classification);

        productStockRepository.save(productStock);
        outboxRepository.saveAll(productStock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        productStock.markEventsAsCommitted();

        logger.info("SKU classified: sku={}, class={}, criteria={}, serviceLevel={}%",
                sku, classification.getAbcClass(), criteria,
                classification.getRecommendedServiceLevel());
    }

    /**
     * Classify using combined criteria (value + velocity + criticality)
     */
    @Transactional
    public void classifyWithCombinedCriteria(String sku, BigDecimal annualUsageValue,
                                            int annualUsageQuantity, BigDecimal unitCost,
                                            double velocityScore, double criticalityScore) {
        classifySKU(sku, annualUsageValue, annualUsageQuantity, unitCost,
                velocityScore, criticalityScore, ABCCriteria.COMBINED);
    }

    /**
     * Classify using value-based criteria only (traditional Pareto)
     */
    @Transactional
    public void classifyByValue(String sku, BigDecimal annualUsageValue,
                               int annualUsageQuantity, BigDecimal unitCost) {
        classifySKU(sku, annualUsageValue, annualUsageQuantity, unitCost,
                0.0, 0.0, ABCCriteria.VALUE_BASED);
    }

    /**
     * Classify using velocity-based criteria (movement frequency)
     */
    @Transactional
    public void classifyByVelocity(String sku, BigDecimal annualUsageValue,
                                  int annualUsageQuantity, BigDecimal unitCost,
                                  double velocityScore) {
        classifySKU(sku, annualUsageValue, annualUsageQuantity, unitCost,
                velocityScore, 0.0, ABCCriteria.VELOCITY_BASED);
    }

    /**
     * Get current ABC class for a SKU
     */
    @Transactional(readOnly = true)
    public Optional<ABCClass> getABCClass(String sku) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getAbcClass();
    }

    /**
     * Get recommended cycle count frequency for a SKU
     */
    @Transactional(readOnly = true)
    public int getRecommendedCountFrequency(String sku) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getRecommendedCycleCountFrequencyDays();
    }

    /**
     * Check if SKU requires re-classification
     */
    @Transactional(readOnly = true)
    public boolean requiresReclassification(String sku) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.requiresAbcReclassification();
    }

    /**
     * Get all SKUs requiring re-classification
     */
    @Transactional(readOnly = true)
    public List<String> getSKUsRequiringReclassification() {
        return productStockRepository.findAll().stream()
                .filter(ProductStock::requiresAbcReclassification)
                .map(ProductStock::getSku)
                .toList();
    }

    /**
     * Batch re-classify all SKUs (scheduled job)
     */
    @Transactional
    public int reclassifyAll(ABCCriteria criteria) {
        logger.info("Starting batch re-classification with criteria={}", criteria);

        List<ProductStock> allStock = productStockRepository.findAll();
        int count = 0;

        for (ProductStock productStock : allStock) {
            if (productStock.requiresAbcReclassification()) {
                // In production, you'd calculate these metrics from actual data
                // For now, using placeholder values
                BigDecimal annualUsageValue = productStock.getTotalValue().orElse(BigDecimal.ZERO);
                int annualUsageQuantity = productStock.getQuantityOnHand();
                BigDecimal unitCost = productStock.getUnitCost().orElse(BigDecimal.ONE);

                ABCClassification classification = ABCClassification.classify(
                        productStock.getSku(), annualUsageValue, annualUsageQuantity,
                        unitCost, 50.0, 50.0, criteria);

                productStock.setAbcClassification(classification);
                productStockRepository.save(productStock);
                outboxRepository.saveAll(productStock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
                productStock.markEventsAsCommitted();

                count++;
            }
        }

        logger.info("Batch re-classification completed: {} SKUs re-classified", count);
        return count;
    }

    /**
     * Get ABC classification details
     */
    @Transactional(readOnly = true)
    public Optional<ABCClassification> getClassification(String sku) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getAbcClassification();
    }
}
