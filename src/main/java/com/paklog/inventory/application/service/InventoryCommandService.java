package com.paklog.inventory.application.service;

import com.paklog.inventory.application.validator.ProductExistenceValidator;
import com.paklog.inventory.domain.event.StockLevelChangedEvent;
import com.paklog.inventory.domain.exception.ProductStockNotFoundException;
import com.paklog.inventory.domain.model.InventoryLedgerEntry;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.StockLevel;
import com.paklog.inventory.domain.repository.InventoryLedgerRepository;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.paklog.inventory.infrastructure.cache.CacheConfiguration;
import com.paklog.inventory.infrastructure.metrics.InventoryMetricsService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryCommandService {

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandService.class);

    private final ProductStockRepository productStockRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final OutboxRepository outboxRepository;
    private final InventoryMetricsService metricsService;
    private final ProductExistenceValidator productExistenceValidator;

    public InventoryCommandService(ProductStockRepository productStockRepository,
                                   InventoryLedgerRepository inventoryLedgerRepository,
                                   OutboxRepository outboxRepository,
                                   InventoryMetricsService metricsService,
                                   ProductExistenceValidator productExistenceValidator) {
        this.productStockRepository = productStockRepository;
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.outboxRepository = outboxRepository;
        this.metricsService = metricsService;
        this.productExistenceValidator = productExistenceValidator;
    }

    @Transactional
    @CacheEvict(value = CacheConfiguration.PRODUCT_STOCK_CACHE, key = "#sku")
    public ProductStock adjustStock(String sku, int quantityChange, String reasonCode, String comment, String operatorId) {
        log.info("Adjusting stock for sku: {}, quantityChange: {}, reasonCode: {}", sku, quantityChange, reasonCode);
        Timer.Sample sample = metricsService.startStockOperation();
        
        try {
            ProductStock productStock = productStockRepository.findBySku(sku)
                    .orElseThrow(() -> new ProductStockNotFoundException(sku));

            int previousQuantityOnHand = productStock.getQuantityOnHand();
            int previousQuantityAllocated = productStock.getQuantityAllocated();
            
            productStock.adjustQuantityOnHand(quantityChange, reasonCode);

            // Create ledger entry
            InventoryLedgerEntry ledgerEntry = InventoryLedgerEntry.forAdjustment(sku, quantityChange, reasonCode + (comment != null ? " - " + comment : ""), operatorId);
            inventoryLedgerRepository.save(ledgerEntry);

            ProductStock savedStock = productStockRepository.save(productStock);
            
            // Update metrics
            metricsService.incrementStockAdjustment(sku, quantityChange, reasonCode);
            metricsService.updateInventoryMetrics(sku, previousQuantityOnHand, previousQuantityAllocated,
                    savedStock.getQuantityOnHand(), savedStock.getQuantityAllocated());
            
            metricsService.stopStockOperation(sample, "adjust");

            publishDomainEvents(savedStock);
            log.info("Stock adjusted for sku: {}", sku);
            return savedStock;
        } catch (Exception e) {
            log.error("Error adjusting stock for sku: {}", sku, e);
            metricsService.stopStockOperation(sample, "adjust_error");
            throw e;
        }
    }

    private void publishDomainEvents(ProductStock productStock) {
        List<OutboxEvent> outboxEvents = productStock.getUncommittedEvents().stream()
                .map(OutboxEvent::from)
                .collect(Collectors.toList());
        if (!outboxEvents.isEmpty()) {
            log.info("Publishing {} domain events for sku: {}", outboxEvents.size(), productStock.getSku());
            outboxRepository.saveAll(outboxEvents);
            productStock.markEventsAsCommitted();
        }
    }

    @Transactional
    public ProductStock allocateStock(String sku, int quantity, String orderId) {
        log.info("Allocating stock for sku: {}, quantity: {}, orderId: {}", sku, quantity, orderId);
        Timer.Sample sample = metricsService.startStockOperation();
        
        try {
            ProductStock productStock = productStockRepository.findBySku(sku)
                    .orElseThrow(() -> new ProductStockNotFoundException(sku));

            int previousQuantityOnHand = productStock.getQuantityOnHand();
            int previousQuantityAllocated = productStock.getQuantityAllocated();
            
            productStock.allocate(quantity);

            InventoryLedgerEntry ledgerEntry = InventoryLedgerEntry.forAllocation(sku, quantity, orderId);
            inventoryLedgerRepository.save(ledgerEntry);

            ProductStock savedStock = productStockRepository.save(productStock);
            
            // Update metrics
            metricsService.incrementStockAllocation(sku, quantity);
            metricsService.updateInventoryMetrics(sku, previousQuantityOnHand, previousQuantityAllocated,
                    savedStock.getQuantityOnHand(), savedStock.getQuantityAllocated());
            
            metricsService.stopStockOperation(sample, "allocate");

            publishDomainEvents(savedStock);
            log.info("Stock allocated for sku: {}", sku);
            return savedStock;
        } catch (Exception e) {
            log.error("Error allocating stock for sku: {}", sku, e);
            metricsService.stopStockOperation(sample, "allocate_error");
            throw e;
        }
    }

    @Transactional
    public ProductStock processItemPicked(String sku, int quantity, String orderId) {
        log.info("Processing item picked for sku: {}, quantity: {}, orderId: {}", sku, quantity, orderId);
        Timer.Sample sample = metricsService.startStockOperation();
        
        try {
            ProductStock productStock = productStockRepository.findBySku(sku)
                    .orElseThrow(() -> new ProductStockNotFoundException(sku));

            int previousQuantityOnHand = productStock.getQuantityOnHand();
            int previousQuantityAllocated = productStock.getQuantityAllocated();
            
            productStock.deallocate(quantity); // Reverse allocation
            productStock.adjustQuantityOnHand(-quantity, "ITEM_PICKED"); // Decrease on hand

            InventoryLedgerEntry ledgerEntry = InventoryLedgerEntry.forPick(sku, quantity, orderId);
            inventoryLedgerRepository.save(ledgerEntry);

            ProductStock savedStock = productStockRepository.save(productStock);
            
            // Update metrics
            metricsService.incrementItemPicked(sku, quantity);
            metricsService.incrementStockDeallocation(sku, quantity);
            metricsService.updateInventoryMetrics(sku, previousQuantityOnHand, previousQuantityAllocated,
                    savedStock.getQuantityOnHand(), savedStock.getQuantityAllocated());
            
            metricsService.stopStockOperation(sample, "item_picked");

            publishDomainEvents(savedStock);
            log.info("Item picked processed for sku: {}", sku);
            return savedStock;
        } catch (Exception e) {
            log.error("Error processing item picked for sku: {}", sku, e);
            metricsService.stopStockOperation(sample, "item_picked_error");
            throw e;
        }
    }

    @Transactional
    public ProductStock receiveStock(String sku, int quantity, String receiptId) {
        log.info("Receiving stock for sku: {}, quantity: {}, receiptId: {}", sku, quantity, receiptId);
        Timer.Sample sample = metricsService.startStockOperation();

        try {
            ProductStock productStock = productStockRepository.findBySku(sku)
                    .orElseGet(() -> {
                        log.info("Creating new product stock for sku: {}", sku);
                        // Validate product exists in catalog before creating new stock
                        productExistenceValidator.validateProductExists(sku);
                        return ProductStock.create(sku, 0);
                    });

            int previousQuantityOnHand = productStock.getQuantityOnHand();
            int previousQuantityAllocated = productStock.getQuantityAllocated();
            
            productStock.receiveStock(quantity);

            InventoryLedgerEntry ledgerEntry = InventoryLedgerEntry.forReceipt(sku, quantity, receiptId);
            inventoryLedgerRepository.save(ledgerEntry);

            ProductStock savedStock = productStockRepository.save(productStock);
            
            // Update metrics
            metricsService.incrementStockReceipt(sku, quantity);
            metricsService.updateInventoryMetrics(sku, previousQuantityOnHand, previousQuantityAllocated,
                    savedStock.getQuantityOnHand(), savedStock.getQuantityAllocated());
            
            metricsService.stopStockOperation(sample, "receive");

            publishDomainEvents(savedStock);
            log.info("Stock received for sku: {}", sku);
            return savedStock;
        } catch (Exception e) {
            log.error("Error receiving stock for sku: {}", sku, e);
            metricsService.stopStockOperation(sample, "receive_error");
            throw e;
        }
    }

    @Transactional
    public void increaseQuantityOnHand(String sku, int quantity) {
        log.info("Increasing quantity on hand for sku: {}, quantity: {}", sku, quantity);
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseGet(() -> {
                    log.info("Creating new product stock for sku: {}", sku);
                    // Validate product exists in catalog before creating new stock
                    productExistenceValidator.validateProductExists(sku);
                    return ProductStock.create(sku, 0);
                });

        productStock.increaseQuantityOnHand(quantity);
        productStockRepository.save(productStock);
        publishDomainEvents(productStock);
        log.info("Quantity on hand increased for sku: {}", sku);
    }

    @Transactional
    public void decreaseQuantityOnHand(String sku, int quantity) {
        log.info("Decreasing quantity on hand for sku: {}, quantity: {}", sku, quantity);
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new ProductStockNotFoundException(sku));

        productStock.decreaseQuantityOnHand(quantity);
        productStockRepository.save(productStock);
        publishDomainEvents(productStock);
        log.info("Quantity on hand decreased for sku: {}", sku);
    }

    /**
     * Set absolute stock quantity (physical count).
     * Used for physical inventory counts where exact quantity is known.
     */
    @Transactional
    @CacheEvict(value = CacheConfiguration.PRODUCT_STOCK_CACHE, key = "#sku")
    public ProductStock setStockLevel(String sku, int absoluteQuantity, String reasonCode,
                                     String comment, String operatorId, String locationId) {
        log.info("Setting absolute stock level for sku: {}, quantity: {}, reasonCode: {}",
                sku, absoluteQuantity, reasonCode);
        Timer.Sample sample = metricsService.startStockOperation();

        try {
            ProductStock productStock = productStockRepository.findBySku(sku)
                    .orElseGet(() -> {
                        log.info("Creating new product stock for sku: {}", sku);
                        // Validate product exists in catalog before creating new stock
                        productExistenceValidator.validateProductExists(sku);
                        return ProductStock.create(sku, 0);
                    });

            int previousQuantityOnHand = productStock.getQuantityOnHand();
            int previousQuantityAllocated = productStock.getQuantityAllocated();

            productStock.setAbsoluteQuantity(absoluteQuantity, reasonCode);

            if (locationId != null) {
                productStock.setLocationId(locationId);
            }

            // Create ledger entry
            int quantityChange = absoluteQuantity - previousQuantityOnHand;
            InventoryLedgerEntry ledgerEntry = InventoryLedgerEntry.forAdjustment(
                sku, quantityChange, reasonCode + (comment != null ? " - " + comment : ""), operatorId);
            inventoryLedgerRepository.save(ledgerEntry);

            ProductStock savedStock = productStockRepository.save(productStock);

            // Update metrics
            metricsService.incrementStockAdjustment(sku, quantityChange, reasonCode);
            metricsService.updateInventoryMetrics(sku, previousQuantityOnHand, previousQuantityAllocated,
                    savedStock.getQuantityOnHand(), savedStock.getQuantityAllocated());

            metricsService.stopStockOperation(sample, "set_absolute");

            publishDomainEvents(savedStock);
            log.info("Absolute stock level set for sku: {}", sku);
            return savedStock;
        } catch (Exception e) {
            log.error("Error setting absolute stock level for sku: {}", sku, e);
            metricsService.stopStockOperation(sample, "set_absolute_error");
            throw e;
        }
    }
}