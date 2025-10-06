package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.ValuationMethod;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Application service for inventory valuation management.
 * Handles cost tracking, COGS calculation, and valuation updates.
 */
@Service
public class ValuationService {

    private static final Logger logger = LoggerFactory.getLogger(ValuationService.class);

    private final ProductStockRepository productStockRepository;
    private final OutboxRepository outboxRepository;

    public ValuationService(ProductStockRepository productStockRepository,
                           OutboxRepository outboxRepository) {
        this.productStockRepository = productStockRepository;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Initialize valuation for a SKU
     */
    @Transactional
    public void initializeValuation(String sku, ValuationMethod method,
                                   BigDecimal initialUnitCost, String currency) {
        logger.info("Initializing valuation: SKU={}, method={}, unitCost={}, currency={}",
                sku, method, initialUnitCost, currency);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        productStock.initializeValuation(method, initialUnitCost, currency);

        productStockRepository.save(productStock);
        // Note: initializeValuation doesn't publish events currently
        // Could add InventoryValuationInitializedEvent if needed

        logger.info("Valuation initialized: SKU={}, method={}, totalValue={}",
                sku, method, productStock.getTotalValue().orElse(BigDecimal.ZERO));
    }

    /**
     * Update valuation on stock receipt
     */
    @Transactional
    public void updateValuationOnReceipt(String sku, int quantityReceived, BigDecimal unitCostReceived) {
        logger.info("Updating valuation on receipt: SKU={}, qty={}, unitCost={}",
                sku, quantityReceived, unitCostReceived);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        productStock.updateValuationOnReceipt(quantityReceived, unitCostReceived);

        productStockRepository.save(productStock);
        outboxRepository.saveAll(productStock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        productStock.markEventsAsCommitted();

        logger.info("Valuation updated on receipt: SKU={}, newUnitCost={}, newTotalValue={}",
                sku, productStock.getUnitCost().orElse(BigDecimal.ZERO),
                productStock.getTotalValue().orElse(BigDecimal.ZERO));
    }

    /**
     * Update valuation on stock issue (calculates COGS)
     */
    @Transactional
    public BigDecimal updateValuationOnIssue(String sku, int quantityIssued) {
        logger.info("Updating valuation on issue: SKU={}, qty={}", sku, quantityIssued);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        // Calculate COGS before updating
        BigDecimal cogs = productStock.getValuation()
                .map(v -> v.getCostOfGoods(quantityIssued))
                .orElse(BigDecimal.ZERO);

        productStock.updateValuationOnIssue(quantityIssued);

        productStockRepository.save(productStock);
        outboxRepository.saveAll(productStock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        productStock.markEventsAsCommitted();

        logger.info("Valuation updated on issue: SKU={}, qty={}, COGS={}, remainingValue={}",
                sku, quantityIssued, cogs, productStock.getTotalValue().orElse(BigDecimal.ZERO));

        return cogs;
    }

    /**
     * Get current unit cost
     */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getUnitCost(String sku) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getUnitCost();
    }

    /**
     * Get total inventory value
     */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getTotalValue(String sku) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getTotalValue();
    }

    /**
     * Calculate COGS without updating valuation (for what-if analysis)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCOGS(String sku, int quantity) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getValuation()
                .map(v -> v.getCostOfGoods(quantity))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get inventory carrying cost
     */
    @Transactional(readOnly = true)
    public BigDecimal getCarryingCost(String sku, double annualCarryingCostPercentage) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getValuation()
                .map(v -> v.getTotalCarryingCost(annualCarryingCostPercentage))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get valuation method for a SKU
     */
    @Transactional(readOnly = true)
    public Optional<ValuationMethod> getValuationMethod(String sku) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getValuation().map(v -> v.getValuationMethod());
    }
}
