package com.paklog.inventory.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class InventoryMetricsService {

    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter stockAllocationCounter;
    private final Counter stockDeallocationCounter;
    private final Counter stockAdjustmentCounter;
    private final Counter itemPickedCounter;
    private final Counter stockReceiptCounter;
    private final Counter stockLevelQueryCounter;
    private final Counter eventProcessingCounter;
    private final Counter eventProcessingErrorCounter;

    public InventoryMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.stockAllocationCounter = Counter.builder("inventory.stock.allocation")
                .description("Number of stock allocations performed")
                .register(meterRegistry);
                
        this.stockDeallocationCounter = Counter.builder("inventory.stock.deallocation")
                .description("Number of stock deallocations performed")
                .register(meterRegistry);
                
        this.stockAdjustmentCounter = Counter.builder("inventory.stock.adjustment")
                .description("Number of stock adjustments performed")
                .register(meterRegistry);
                
        this.itemPickedCounter = Counter.builder("inventory.item.picked")
                .description("Number of items picked from inventory")
                .register(meterRegistry);
                
        this.stockReceiptCounter = Counter.builder("inventory.stock.receipt")
                .description("Number of stock receipts processed")
                .register(meterRegistry);
                
        this.stockLevelQueryCounter = Counter.builder("inventory.query.stock_level")
                .description("Number of stock level queries")
                .register(meterRegistry);
                
        this.eventProcessingCounter = Counter.builder("inventory.event.processed")
                .description("Number of events successfully processed")
                .register(meterRegistry);
                
        this.eventProcessingErrorCounter = Counter.builder("inventory.event.error")
                .description("Number of event processing errors")
                .register(meterRegistry);
    }
    
    // Counter increment methods
    public void incrementStockAllocation(String sku, int quantity) {
        stockAllocationCounter.increment();
    }
    
    public void incrementStockDeallocation(String sku, int quantity) {
        stockDeallocationCounter.increment();
    }
    
    public void incrementStockAdjustment(String sku, int quantityChange, String reasonCode) {
        stockAdjustmentCounter.increment();
    }
    
    public void incrementItemPicked(String sku, int quantity) {
        itemPickedCounter.increment();
    }
    
    public void incrementStockReceipt(String sku, int quantity) {
        stockReceiptCounter.increment();
    }
    
    public void incrementStockLevelQuery(String sku) {
        stockLevelQueryCounter.increment();
    }
    
    public void incrementEventProcessed(String eventType) {
        eventProcessingCounter.increment();
    }
    
    public void incrementEventError(String eventType, String errorType) {
        eventProcessingErrorCounter.increment();
    }
    
    // Timer sample methods
    public Timer.Sample startStockOperation() {
        return Timer.start(meterRegistry);
    }
    
    public Timer.Sample startQueryOperation() {
        return Timer.start(meterRegistry);
    }
    
    public Timer.Sample startEventProcessing() {
        return Timer.start(meterRegistry);
    }
    
    public void stopStockOperation(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("inventory.operation.duration")
                .tag("operation", operation)
                .register(meterRegistry));
    }
    
    public void stopQueryOperation(Timer.Sample sample, String queryType) {
        sample.stop(Timer.builder("inventory.query.duration")
                .tag("query_type", queryType)
                .register(meterRegistry));
    }
    
    public void stopEventProcessing(Timer.Sample sample, String eventType) {
        sample.stop(Timer.builder("inventory.event.processing.duration")
                .tag("event_type", eventType)
                .register(meterRegistry));
    }
    
    // Simplified metrics update - just logging for now
    public void updateInventoryMetrics(String sku, int previousQuantityOnHand, int previousQuantityAllocated, 
                                      int newQuantityOnHand, int newQuantityAllocated) {
        // In a real implementation, this would update gauge metrics
        // For now, we'll just track the basic counters
    }
}