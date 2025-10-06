package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.HoldType;
import com.paklog.inventory.domain.model.InventoryHold;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for inventory hold management.
 * Handles placing, releasing, and querying holds.
 */
@Service
public class InventoryHoldService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryHoldService.class);

    private final ProductStockRepository productStockRepository;
    private final OutboxRepository outboxRepository;

    public InventoryHoldService(ProductStockRepository productStockRepository,
                               OutboxRepository outboxRepository) {
        this.productStockRepository = productStockRepository;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Place a hold on inventory
     */
    @Transactional
    public String placeHold(String sku, HoldType holdType, int quantity,
                          String reason, String placedBy) {
        logger.info("Placing hold: SKU={}, type={}, qty={}, placedBy={}, reason={}",
                sku, holdType, quantity, placedBy, reason);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        productStock.placeHold(holdType, quantity, reason, placedBy);

        productStockRepository.save(productStock);
        outboxRepository.saveAll(productStock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        productStock.markEventsAsCommitted();

        // Get the hold ID from the last added hold
        String holdId = productStock.getHolds().stream()
                .filter(InventoryHold::isActive)
                .reduce((first, second) -> second) // Get last
                .map(InventoryHold::getHoldId)
                .orElse(null);

        logger.info("Hold placed successfully: SKU={}, holdId={}, newATP={}",
                sku, holdId, productStock.getAvailableToPromise());

        return holdId;
    }

    /**
     * Release a hold
     */
    @Transactional
    public void releaseHold(String sku, String holdId, String releasedBy, String releaseReason) {
        logger.info("Releasing hold: SKU={}, holdId={}, releasedBy={}, reason={}",
                sku, holdId, releasedBy, releaseReason);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        productStock.releaseHold(holdId, releasedBy, releaseReason);

        productStockRepository.save(productStock);
        outboxRepository.saveAll(productStock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        productStock.markEventsAsCommitted();

        logger.info("Hold released successfully: SKU={}, holdId={}, newATP={}",
                sku, holdId, productStock.getAvailableToPromise());
    }

    /**
     * Place a quality hold
     */
    @Transactional
    public String placeQualityHold(String sku, int quantity, String reason, String placedBy) {
        return placeHold(sku, HoldType.QUALITY_HOLD, quantity, reason, placedBy);
    }

    /**
     * Place a legal hold
     */
    @Transactional
    public String placeLegalHold(String sku, int quantity, String reason, String placedBy) {
        return placeHold(sku, HoldType.LEGAL_HOLD, quantity, reason, placedBy);
    }

    /**
     * Place a recall hold
     */
    @Transactional
    public String placeRecallHold(String sku, int quantity, String reason, String placedBy) {
        return placeHold(sku, HoldType.RECALL_HOLD, quantity, reason, placedBy);
    }

    /**
     * Place a credit hold
     */
    @Transactional
    public String placeCreditHold(String sku, int quantity, String customerId, String placedBy) {
        return placeHold(sku, HoldType.CREDIT_HOLD, quantity, "Customer: " + customerId, placedBy);
    }

    /**
     * Release a hold by hold ID only (overload for controller)
     */
    @Transactional
    public void releaseHold(String holdId) {
        // Find the stock with this hold ID
        List<ProductStock> allStocks = productStockRepository.findAll();
        for (ProductStock stock : allStocks) {
            if (stock.getHolds().stream().anyMatch(h -> h.getHoldId().equals(holdId))) {
                stock.releaseHold(holdId, "SYSTEM", "Released via API");
                productStockRepository.save(stock);
                outboxRepository.saveAll(stock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
                stock.markEventsAsCommitted();
                logger.info("Hold released: holdId={}", holdId);
                return;
            }
        }
        throw new IllegalArgumentException("Hold not found: " + holdId);
    }

    /**
     * Release all expired holds (all SKUs)
     */
    @Transactional
    public int releaseExpiredHolds() {
        logger.info("Releasing all expired holds across all SKUs");
        int totalCount = 0;
        List<ProductStock> allStocks = productStockRepository.findAll();
        for (ProductStock stock : allStocks) {
            totalCount += releaseExpiredHolds(stock.getSku(), "SYSTEM");
        }
        return totalCount;
    }

    /**
     * Get holds by type for a SKU
     */
    @Transactional(readOnly = true)
    public List<InventoryHold> getHoldsByType(String sku, HoldType holdType) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getHolds().stream()
                .filter(InventoryHold::isActive)
                .filter(h -> h.getHoldType() == holdType)
                .toList();
    }

    /**
     * Check if SKU has active holds
     */
    @Transactional(readOnly = true)
    public boolean hasActiveHolds(String sku) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getHolds().stream().anyMatch(InventoryHold::isActive);
    }

    /**
     * Get all active holds for a SKU
     */
    @Transactional(readOnly = true)
    public List<InventoryHold> getActiveHolds(String sku) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getHolds().stream()
                .filter(InventoryHold::isActive)
                .toList();
    }

    /**
     * Get total quantity under hold
     */
    @Transactional(readOnly = true)
    public int getTotalHeldQuantity(String sku) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getTotalHeldQuantity();
    }

    /**
     * Release all expired holds for a SKU
     */
    @Transactional
    public int releaseExpiredHolds(String sku, String releasedBy) {
        logger.info("Releasing expired holds for SKU={}", sku);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        List<InventoryHold> expiredHolds = productStock.getHolds().stream()
                .filter(InventoryHold::isExpired)
                .toList();

        int count = 0;
        for (InventoryHold hold : expiredHolds) {
            productStock.releaseHold(hold.getHoldId(), releasedBy, "Auto-release: hold expired");
            count++;
        }

        if (count > 0) {
            productStockRepository.save(productStock);
            outboxRepository.saveAll(productStock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
            productStock.markEventsAsCommitted();
        }

        logger.info("Released {} expired holds for SKU={}", count, sku);
        return count;
    }
}
