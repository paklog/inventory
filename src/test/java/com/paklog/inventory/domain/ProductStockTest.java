package com.paklog.inventory.domain;

import com.paklog.inventory.domain.event.StockLevelChangedEvent;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.StockLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductStockTest {

    private ProductStock productStock;
    private final String SKU = "TEST-SKU-001";

    @BeforeEach
    void setUp() {
        productStock = ProductStock.create(SKU, 100);
        productStock.markEventsAsCommitted(); // Clear initial creation event for test clarity
    }

    @Test
    @DisplayName("Should create ProductStock with initial quantity and generate event")
    void createProductStock() {
        ProductStock newStock = ProductStock.create("NEW-SKU", 50);
        assertEquals("NEW-SKU", newStock.getSku());
        assertEquals(50, newStock.getQuantityOnHand());
        assertEquals(0, newStock.getQuantityAllocated());
        assertEquals(50, newStock.getAvailableToPromise());
        assertNotNull(newStock.getLastUpdated());

        List<com.paklog.inventory.domain.event.DomainEvent> events = newStock.getUncommittedEvents();
        assertEquals(0, events.size());
        // No initial event is added by ProductStock.create anymore
    }

    @Test
    @DisplayName("Should allocate stock successfully")
    void allocateStock() {
        productStock.allocate(10);
        assertEquals(100, productStock.getQuantityOnHand());
        assertEquals(10, productStock.getQuantityAllocated());
        assertEquals(90, productStock.getAvailableToPromise());
        assertFalse(productStock.getUncommittedEvents().isEmpty()); // Now expects an event
        assertTrue(productStock.getUncommittedEvents().get(0) instanceof StockLevelChangedEvent);
        StockLevelChangedEvent event = (StockLevelChangedEvent) productStock.getUncommittedEvents().get(0);
        assertEquals("ALLOCATION", event.getChangeReason());
        assertEquals(100, event.getPreviousStockLevel().getQuantityOnHand());
        assertEquals(0, event.getPreviousStockLevel().getQuantityAllocated());
        assertEquals(100, event.getPreviousStockLevel().getAvailableToPromise());
        assertEquals(100, event.getNewStockLevel().getQuantityOnHand());
        assertEquals(10, event.getNewStockLevel().getQuantityAllocated());
        assertEquals(90, event.getNewStockLevel().getAvailableToPromise());
    }

    @Test
    @DisplayName("Should throw exception when allocating more than available")
    void allocateStockExceedsAvailable() {
        assertThrows(IllegalArgumentException.class, () -> productStock.allocate(101));
    }

    @Test
    @DisplayName("Should deallocate stock successfully")
    void deallocateStock() {
        productStock.allocate(20);
        productStock.markEventsAsCommitted(); // Clear allocation event
        productStock.deallocate(10);
        assertEquals(100, productStock.getQuantityOnHand());
        assertEquals(10, productStock.getQuantityAllocated());
        assertEquals(90, productStock.getAvailableToPromise());
        assertFalse(productStock.getUncommittedEvents().isEmpty()); // Now expects an event
        assertTrue(productStock.getUncommittedEvents().get(0) instanceof StockLevelChangedEvent);
        StockLevelChangedEvent event = (StockLevelChangedEvent) productStock.getUncommittedEvents().get(0);
        assertEquals("DEALLOCATION", event.getChangeReason());
        assertEquals(100, event.getPreviousStockLevel().getQuantityOnHand());
        assertEquals(20, event.getPreviousStockLevel().getQuantityAllocated());
        assertEquals(80, event.getPreviousStockLevel().getAvailableToPromise());
        assertEquals(100, event.getNewStockLevel().getQuantityOnHand());
        assertEquals(10, event.getNewStockLevel().getQuantityAllocated());
        assertEquals(90, event.getNewStockLevel().getAvailableToPromise());
    }

    @Test
    @DisplayName("Should throw exception when deallocating more than allocated")
    void deallocateStockExceedsAllocated() {
        assertThrows(IllegalArgumentException.class, () -> productStock.deallocate(1)); // No stock allocated yet
    }

    @Test
    @DisplayName("Should adjust quantity on hand positively")
    void adjustQuantityOnHandPositive() {
        productStock.adjustQuantityOnHand(50, "STOCK_INTAKE");
        assertEquals(150, productStock.getQuantityOnHand());
        assertEquals(0, productStock.getQuantityAllocated());
        assertEquals(150, productStock.getAvailableToPromise());
        assertFalse(productStock.getUncommittedEvents().isEmpty()); // Now expects an event
        assertTrue(productStock.getUncommittedEvents().get(0) instanceof StockLevelChangedEvent);
        StockLevelChangedEvent event = (StockLevelChangedEvent) productStock.getUncommittedEvents().get(0);
        assertEquals("STOCK_INTAKE", event.getChangeReason());
        assertEquals(100, event.getPreviousStockLevel().getQuantityOnHand());
        assertEquals(0, event.getPreviousStockLevel().getQuantityAllocated());
        assertEquals(100, event.getPreviousStockLevel().getAvailableToPromise());
        assertEquals(150, event.getNewStockLevel().getQuantityOnHand());
        assertEquals(0, event.getNewStockLevel().getQuantityAllocated());
        assertEquals(150, event.getNewStockLevel().getAvailableToPromise());
    }

    @Test
    @DisplayName("Should adjust quantity on hand negatively")
    void adjustQuantityOnHandNegative() {
        productStock.adjustQuantityOnHand(-20, "DAMAGE");
        assertEquals(80, productStock.getQuantityOnHand());
        assertEquals(0, productStock.getQuantityAllocated());
        assertEquals(80, productStock.getAvailableToPromise());
        assertFalse(productStock.getUncommittedEvents().isEmpty()); // Now expects an event
        assertTrue(productStock.getUncommittedEvents().get(0) instanceof StockLevelChangedEvent);
        StockLevelChangedEvent event = (StockLevelChangedEvent) productStock.getUncommittedEvents().get(0);
        assertEquals("DAMAGE", event.getChangeReason());
        assertEquals(100, event.getPreviousStockLevel().getQuantityOnHand());
        assertEquals(0, event.getPreviousStockLevel().getQuantityAllocated());
        assertEquals(100, event.getPreviousStockLevel().getAvailableToPromise());
        assertEquals(80, event.getNewStockLevel().getQuantityOnHand());
        assertEquals(0, event.getNewStockLevel().getQuantityAllocated());
        assertEquals(80, event.getNewStockLevel().getAvailableToPromise());
    }

    @Test
    @DisplayName("Should throw exception when adjusting quantity on hand to negative")
    void adjustQuantityOnHandToNegative() {
        assertThrows(IllegalArgumentException.class, () -> productStock.adjustQuantityOnHand(-101, "LOSS"));
    }

    @Test
    @DisplayName("Should receive stock successfully")
    void receiveStock() {
        productStock.receiveStock(30);
        assertEquals(130, productStock.getQuantityOnHand());
        assertEquals(0, productStock.getQuantityAllocated());
        assertEquals(130, productStock.getAvailableToPromise());
        assertFalse(productStock.getUncommittedEvents().isEmpty()); // Now expects an event
        assertTrue(productStock.getUncommittedEvents().get(0) instanceof StockLevelChangedEvent);
        StockLevelChangedEvent event = (StockLevelChangedEvent) productStock.getUncommittedEvents().get(0);
        assertEquals("STOCK_RECEIPT", event.getChangeReason());
    }

    @Test
    @DisplayName("Should load ProductStock from persistence correctly")
    void loadProductStock() {
        ProductStock loadedStock = ProductStock.load(SKU, 70, 30, LocalDateTime.now().minusDays(1));
        assertEquals(SKU, loadedStock.getSku());
        assertEquals(70, loadedStock.getQuantityOnHand());
        assertEquals(30, loadedStock.getQuantityAllocated());
        assertEquals(40, loadedStock.getAvailableToPromise());
        assertTrue(loadedStock.getUncommittedEvents().isEmpty()); // Loaded stock has no uncommitted events
    }

    @Test
    @DisplayName("Should maintain invariants after operations")
    void maintainInvariants() {
        productStock.allocate(50);
        assertDoesNotThrow(() -> productStock.deallocate(20));
        assertDoesNotThrow(() -> productStock.adjustQuantityOnHand(10, "TEST"));
        assertThrows(IllegalArgumentException.class, () -> {
            // Simulate an invalid state directly for testing invariant
            ProductStock invalidStock = ProductStock.load(SKU, 50, 60, LocalDateTime.now());
            // This should ideally be caught by the constructor, but if somehow bypassed,
            // subsequent operations should fail.
            invalidStock.allocate(1); // This operation would trigger invariant check
        });
    }
}