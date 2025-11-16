package com.paklog.inventory.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom metrics for inventory service operations.
 * Tracks business metrics and performance indicators.
 */
@Component
public class InventoryMetrics {

    private final Counter stockReceiptsCounter;
    private final Counter stockAllocationsCounter;
    private final Counter stockAdjustmentsCounter;
    private final Counter stockTransfersCounter;
    private final Counter cloudEventsPublishedCounter;
    private final Counter cloudEventsFailedCounter;
    private final Timer stockOperationTimer;

    public InventoryMetrics(MeterRegistry meterRegistry) {
        // Business metrics
        this.stockReceiptsCounter = Counter.builder("inventory.stock.receipts")
                .description("Total number of stock receipts processed")
                .tag("operation", "receive")
                .register(meterRegistry);

        this.stockAllocationsCounter = Counter.builder("inventory.stock.allocations")
                .description("Total number of stock allocations")
                .tag("operation", "allocate")
                .register(meterRegistry);

        this.stockAdjustmentsCounter = Counter.builder("inventory.stock.adjustments")
                .description("Total number of stock adjustments")
                .tag("operation", "adjust")
                .register(meterRegistry);

        this.stockTransfersCounter = Counter.builder("inventory.stock.transfers")
                .description("Total number of stock transfers")
                .tag("operation", "transfer")
                .register(meterRegistry);

        // Event metrics
        this.cloudEventsPublishedCounter = Counter.builder("inventory.cloudevents.published")
                .description("Total number of CloudEvents published")
                .register(meterRegistry);

        this.cloudEventsFailedCounter = Counter.builder("inventory.cloudevents.failed")
                .description("Total number of failed CloudEvents")
                .register(meterRegistry);

        // Performance metrics
        this.stockOperationTimer = Timer.builder("inventory.operation.duration")
                .description("Duration of inventory operations")
                .register(meterRegistry);
    }

    public void recordStockReceipt() {
        stockReceiptsCounter.increment();
    }

    public void recordStockAllocation() {
        stockAllocationsCounter.increment();
    }

    public void recordStockAdjustment() {
        stockAdjustmentsCounter.increment();
    }

    public void recordStockTransfer() {
        stockTransfersCounter.increment();
    }

    public void recordCloudEventPublished() {
        cloudEventsPublishedCounter.increment();
    }

    public void recordCloudEventFailed() {
        cloudEventsFailedCounter.increment();
    }

    public void recordOperationDuration(String operationType, long durationMillis) {
        stockOperationTimer.record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public <T> T timeOperation(String operationType, java.util.function.Supplier<T> operation) {
        long startTime = System.currentTimeMillis();
        try {
            return operation.get();
        } finally {
            recordOperationDuration(operationType, System.currentTimeMillis() - startTime);
        

}
}
}
