package com.paklog.inventory.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InventoryMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private InventoryMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new InventoryMetricsService(meterRegistry);
    }

    @Test
    @DisplayName("Should initialize all counters successfully")
    void constructor_InitializesAllCounters() {
        // Assert
        assertNotNull(meterRegistry.get("inventory.stock.allocation").counter());
        assertNotNull(meterRegistry.get("inventory.stock.deallocation").counter());
        assertNotNull(meterRegistry.get("inventory.stock.adjustment").counter());
        assertNotNull(meterRegistry.get("inventory.item.picked").counter());
        assertNotNull(meterRegistry.get("inventory.stock.receipt").counter());
        assertNotNull(meterRegistry.get("inventory.query.stock_level").counter());
        assertNotNull(meterRegistry.get("inventory.event.processed").counter());
        assertNotNull(meterRegistry.get("inventory.event.error").counter());
    }

    @Test
    @DisplayName("Should increment stock allocation counter")
    void incrementStockAllocation_IncrementsCounter() {
        // Arrange
        String sku = "TEST-SKU-001";
        int quantity = 10;

        // Act
        metricsService.incrementStockAllocation(sku, quantity);

        // Assert
        Counter counter = meterRegistry.get("inventory.stock.allocation").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should increment stock deallocation counter")
    void incrementStockDeallocation_IncrementsCounter() {
        // Arrange
        String sku = "TEST-SKU-002";
        int quantity = 5;

        // Act
        metricsService.incrementStockDeallocation(sku, quantity);

        // Assert
        Counter counter = meterRegistry.get("inventory.stock.deallocation").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should increment stock adjustment counter")
    void incrementStockAdjustment_IncrementsCounter() {
        // Arrange
        String sku = "TEST-SKU-003";
        int quantityChange = 15;
        String reasonCode = "CYCLE_COUNT";

        // Act
        metricsService.incrementStockAdjustment(sku, quantityChange, reasonCode);

        // Assert
        Counter counter = meterRegistry.get("inventory.stock.adjustment").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should increment item picked counter")
    void incrementItemPicked_IncrementsCounter() {
        // Arrange
        String sku = "TEST-SKU-004";
        int quantity = 3;

        // Act
        metricsService.incrementItemPicked(sku, quantity);

        // Assert
        Counter counter = meterRegistry.get("inventory.item.picked").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should increment stock receipt counter")
    void incrementStockReceipt_IncrementsCounter() {
        // Arrange
        String sku = "TEST-SKU-005";
        int quantity = 100;

        // Act
        metricsService.incrementStockReceipt(sku, quantity);

        // Assert
        Counter counter = meterRegistry.get("inventory.stock.receipt").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should increment stock level query counter")
    void incrementStockLevelQuery_IncrementsCounter() {
        // Arrange
        String sku = "TEST-SKU-006";

        // Act
        metricsService.incrementStockLevelQuery(sku);

        // Assert
        Counter counter = meterRegistry.get("inventory.query.stock_level").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should increment event processed counter")
    void incrementEventProcessed_IncrementsCounter() {
        // Arrange
        String eventType = "com.example.test.event";

        // Act
        metricsService.incrementEventProcessed(eventType);

        // Assert
        Counter counter = meterRegistry.get("inventory.event.processed").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should increment event error counter")
    void incrementEventError_IncrementsCounter() {
        // Arrange
        String eventType = "com.example.test.event";
        String errorType = "RuntimeException";

        // Act
        metricsService.incrementEventError(eventType, errorType);

        // Assert
        Counter counter = meterRegistry.get("inventory.event.error").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("Should create and return timer sample for stock operation")
    void startStockOperation_ReturnsTimerSample() {
        // Act
        Timer.Sample sample = metricsService.startStockOperation();

        // Assert
        assertNotNull(sample);
    }

    @Test
    @DisplayName("Should create and return timer sample for query operation")
    void startQueryOperation_ReturnsTimerSample() {
        // Act
        Timer.Sample sample = metricsService.startQueryOperation();

        // Assert
        assertNotNull(sample);
    }

    @Test
    @DisplayName("Should create and return timer sample for event processing")
    void startEventProcessing_ReturnsTimerSample() {
        // Act
        Timer.Sample sample = metricsService.startEventProcessing();

        // Assert
        assertNotNull(sample);
    }

    @Test
    @DisplayName("Should stop stock operation timer and register with operation tag")
    void stopStockOperation_RegistersTimerWithOperationTag() {
        // Arrange
        Timer.Sample sample = metricsService.startStockOperation();
        String operation = "adjust";

        // Act
        metricsService.stopStockOperation(sample, operation);

        // Assert
        Timer timer = meterRegistry.get("inventory.operation.duration")
                .tag("operation", operation)
                .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
    }

    @Test
    @DisplayName("Should stop query operation timer and register with query type tag")
    void stopQueryOperation_RegistersTimerWithQueryTypeTag() {
        // Arrange
        Timer.Sample sample = metricsService.startQueryOperation();
        String queryType = "stock_level";

        // Act
        metricsService.stopQueryOperation(sample, queryType);

        // Assert
        Timer timer = meterRegistry.get("inventory.query.duration")
                .tag("query_type", queryType)
                .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
    }

    @Test
    @DisplayName("Should stop event processing timer and register with event type tag")
    void stopEventProcessing_RegistersTimerWithEventTypeTag() {
        // Arrange
        Timer.Sample sample = metricsService.startEventProcessing();
        String eventType = "com.example.test.event";

        // Act
        metricsService.stopEventProcessing(sample, eventType);

        // Assert
        Timer timer = meterRegistry.get("inventory.event.processing.duration")
                .tag("event_type", eventType)
                .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
    }

    @Test
    @DisplayName("Should handle multiple counter increments correctly")
    void multipleIncrements_HandlesCorrectly() {
        // Arrange
        String sku = "MULTI-TEST-SKU";

        // Act
        metricsService.incrementStockAllocation(sku, 10);
        metricsService.incrementStockAllocation(sku, 5);
        metricsService.incrementStockAllocation(sku, 15);

        // Assert
        Counter counter = meterRegistry.get("inventory.stock.allocation").counter();
        assertEquals(3.0, counter.count());
    }

    @Test
    @DisplayName("Should handle updateInventoryMetrics without throwing exceptions")
    void updateInventoryMetrics_CompletesWithoutError() {
        // Arrange
        String sku = "UPDATE-TEST-SKU";
        int previousQuantityOnHand = 50;
        int previousQuantityAllocated = 10;
        int newQuantityOnHand = 60;
        int newQuantityAllocated = 15;

        // Act & Assert - should not throw any exceptions
        assertDoesNotThrow(() -> 
            metricsService.updateInventoryMetrics(sku, previousQuantityOnHand, previousQuantityAllocated,
                    newQuantityOnHand, newQuantityAllocated)
        );
    }

    @Test
    @DisplayName("Should handle null and empty string parameters gracefully")
    void nullAndEmptyParameters_HandleGracefully() {
        // Act & Assert - should not throw any exceptions
        assertDoesNotThrow(() -> {
            metricsService.incrementStockAllocation(null, 10);
            metricsService.incrementStockAllocation("", 10);
            metricsService.incrementEventProcessed(null);
            metricsService.incrementEventProcessed("");
            metricsService.incrementEventError(null, null);
            metricsService.incrementEventError("", "");
        });
    }

    @Test
    @DisplayName("Should handle edge case quantities correctly")
    void edgeCaseQuantities_HandleCorrectly() {
        // Arrange & Act & Assert - should not throw any exceptions
        assertDoesNotThrow(() -> {
            metricsService.incrementStockAllocation("EDGE-SKU", 0);
            metricsService.incrementStockAllocation("EDGE-SKU", Integer.MAX_VALUE);
            metricsService.incrementStockAllocation("EDGE-SKU", -1);
        });
    }
}