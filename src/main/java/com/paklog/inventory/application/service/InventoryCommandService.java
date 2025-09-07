package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.event.StockLevelChangedEvent;
import com.paklog.inventory.domain.exception.ProductStockNotFoundException;
import com.paklog.inventory.domain.model.InventoryLedgerEntry;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.StockLevel;
import com.paklog.inventory.domain.repository.InventoryLedgerRepository;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.infrastructure.metrics.InventoryMetricsService;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryCommandService {

    private final ProductStockRepository productStockRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final InventoryMetricsService metricsService;

    public InventoryCommandService(ProductStockRepository productStockRepository,
                                   InventoryLedgerRepository inventoryLedgerRepository,
                                   InventoryMetricsService metricsService) {
        this.productStockRepository = productStockRepository;
        this.inventoryLedgerRepository = inventoryLedgerRepository;
        this.metricsService = metricsService;
    }

    @Transactional
    public ProductStock adjustStock(String sku, int quantityChange, String reasonCode, String comment, String operatorId) {
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
            return savedStock;
        } catch (Exception e) {
            metricsService.stopStockOperation(sample, "adjust_error");
            throw e;
        }
    }

    @Transactional
    public ProductStock allocateStock(String sku, int quantity, String orderId) {
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
            return savedStock;
        } catch (Exception e) {
            metricsService.stopStockOperation(sample, "allocate_error");
            throw e;
        }
    }

    @Transactional
    public ProductStock processItemPicked(String sku, int quantity, String orderId) {
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
            return savedStock;
        } catch (Exception e) {
            metricsService.stopStockOperation(sample, "item_picked_error");
            throw e;
        }
    }

    @Transactional
    public ProductStock receiveStock(String sku, int quantity, String receiptId) {
        Timer.Sample sample = metricsService.startStockOperation();
        
        try {
            ProductStock productStock = productStockRepository.findBySku(sku)
                    .orElseGet(() -> {
                        ProductStock newProduct = ProductStock.create(sku, 0);
                        newProduct.addEvent(new StockLevelChangedEvent(newProduct.getSku(), StockLevel.of(0, 0), newProduct.getStockLevel(), "INITIAL_STOCK"));
                        return newProduct;
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
            return savedStock;
        } catch (Exception e) {
            metricsService.stopStockOperation(sample, "receive_error");
            throw e;
        }
    }
}