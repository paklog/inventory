package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.StockStatus;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for stock status management.
 * Handles status transitions and related business logic.
 */
@Service
public class StockStatusService {

    private static final Logger logger = LoggerFactory.getLogger(StockStatusService.class);

    private final ProductStockRepository productStockRepository;
    private final OutboxRepository outboxRepository;

    public StockStatusService(ProductStockRepository productStockRepository,
                             OutboxRepository outboxRepository) {
        this.productStockRepository = productStockRepository;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Change stock status for a given quantity
     */
    @Transactional
    public void changeStockStatus(String sku, int quantity, StockStatus fromStatus,
                                 StockStatus toStatus, String reason) {
        logger.info("Changing stock status: SKU={}, qty={}, from={}, to={}, reason={}",
                sku, quantity, fromStatus, toStatus, reason);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        productStock.changeStockStatus(quantity, fromStatus, toStatus, reason);

        productStockRepository.save(productStock);
        outboxRepository.saveAll(productStock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        productStock.markEventsAsCommitted();

        logger.info("Stock status changed successfully: SKU={}, newATP={}",
                sku, productStock.getAvailableToPromise());
    }

    /**
     * Move stock to quarantine (quality hold)
     */
    @Transactional
    public void moveToQuarantine(String sku, int quantity, String reason) {
        changeStockStatus(sku, quantity, StockStatus.AVAILABLE, StockStatus.QUARANTINE, reason);
    }

    /**
     * Release stock from quarantine to available
     */
    @Transactional
    public void releaseFromQuarantine(String sku, int quantity, String reason) {
        changeStockStatus(sku, quantity, StockStatus.QUARANTINE, StockStatus.AVAILABLE, reason);
    }

    /**
     * Mark stock as damaged
     */
    @Transactional
    public void markAsDamaged(String sku, int quantity, StockStatus fromStatus, String reason) {
        changeStockStatus(sku, quantity, fromStatus, StockStatus.DAMAGED, reason);
    }

    /**
     * Mark stock as damaged (from AVAILABLE status)
     */
    @Transactional
    public void markAsDamaged(String sku, int quantity, String reason) {
        markAsDamaged(sku, quantity, StockStatus.AVAILABLE, reason);
    }

    /**
     * Mark stock as expired
     */
    @Transactional
    public void markAsExpired(String sku, int quantity, StockStatus fromStatus, String reason) {
        changeStockStatus(sku, quantity, fromStatus, StockStatus.EXPIRED, reason);
    }

    /**
     * Mark stock as expired (from AVAILABLE status, auto-reason)
     */
    @Transactional
    public void markAsExpired(String sku, int quantity) {
        markAsExpired(sku, quantity, StockStatus.AVAILABLE, "Expired");
    }

    /**
     * Release from quarantine (auto-reason)
     */
    @Transactional
    public void releaseFromQuarantine(String sku, int quantity) {
        releaseFromQuarantine(sku, quantity, "Released from quarantine");
    }

    /**
     * Get quantity by status
     */
    @Transactional(readOnly = true)
    public int getQuantityByStatus(String sku, StockStatus status) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        return productStock.getQuantityByStatus(status);
    }

    /**
     * Get all stock quantities grouped by status
     */
    @Transactional(readOnly = true)
    public java.util.Map<StockStatus, Integer> getStockByStatus(String sku) {
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        java.util.Map<StockStatus, Integer> result = new java.util.HashMap<>();
        for (StockStatus status : StockStatus.values()) {
            int qty = productStock.getQuantityByStatus(status);
            if (qty > 0) {
                result.put(status, qty);
            }
        }
        return result;
    }

    /**
     * Get quantity in specific status (alias for getQuantityByStatus)
     */
    @Transactional(readOnly = true)
    public int getQuantityInStatus(String sku, StockStatus status) {
        return getQuantityByStatus(sku, status);
    }

    /**
     * Receive stock with specific status (e.g., damaged goods on receipt)
     */
    @Transactional
    public void receiveStockWithStatus(String sku, int quantity, StockStatus status, String reason) {
        logger.info("Receiving stock with status: SKU={}, qty={}, status={}, reason={}",
                sku, quantity, status, reason);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseGet(() -> ProductStock.create(sku, 0));

        productStock.receiveStockWithStatus(quantity, status);

        productStockRepository.save(productStock);
        outboxRepository.saveAll(productStock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        productStock.markEventsAsCommitted();

        logger.info("Stock received with status: SKU={}, totalOnHand={}",
                sku, productStock.getQuantityOnHand());
    }
}
