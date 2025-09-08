package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.event.StockLevelChangedEvent;
import com.paklog.inventory.domain.exception.InvalidQuantityException;
import com.paklog.inventory.domain.exception.InsufficientStockException;
import com.paklog.inventory.domain.exception.StockLevelInvariantViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductStockTest {

    private String sku;
    private int initialQuantity;

    @BeforeEach
    void setUp() {
        sku = "PROD001";
        initialQuantity = 100;
    }

    @Test
    @DisplayName("Should create ProductStock with initial quantity and record event")
    void create_ValidInitialQuantity_CreatesProductStockAndRecordsEvent() {
        // Arrange & Act
        ProductStock productStock = ProductStock.create(sku, initialQuantity);

        // Assert
        assertNotNull(productStock);
        assertEquals(sku, productStock.getSku());
        assertEquals(initialQuantity, productStock.getQuantityOnHand());
        assertEquals(0, productStock.getQuantityAllocated());
        assertEquals(initialQuantity, productStock.getAvailableToPromise());
        assertNotNull(productStock.getLastUpdated());

        List<StockLevelChangedEvent> events = productStock.getUncommittedEvents().stream()
                .filter(e -> e instanceof StockLevelChangedEvent)
                .map(e -> (StockLevelChangedEvent) e)
                .toList();
        assertEquals(1, events.size());
        // Initial event is added by ProductStock.create
    }

    @Test
    @DisplayName("Should throw InvalidQuantityException when creating ProductStock with negative initial quantity")
    void create_NegativeInitialQuantity_ThrowsException() {
        // Arrange
        int negativeQuantity = -10;

        // Act & Assert
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                ProductStock.create(sku, negativeQuantity));
        assertTrue(exception.getMessage().contains("Initial quantity cannot be negative"));
    }

    @Test
    @DisplayName("Should load ProductStock from persistence correctly")
    void load_ValidData_LoadsProductStock() {
        // Arrange
        int quantityOnHand = 50;
        int quantityAllocated = 20;
        LocalDateTime lastUpdated = LocalDateTime.now().minusDays(1);

        // Act
        ProductStock productStock = ProductStock.load(sku, quantityOnHand, quantityAllocated, lastUpdated);

        // Assert
        assertNotNull(productStock);
        assertEquals(sku, productStock.getSku());
        assertEquals(quantityOnHand, productStock.getQuantityOnHand());
        assertEquals(quantityAllocated, productStock.getQuantityAllocated());
        assertEquals(quantityOnHand - quantityAllocated, productStock.getAvailableToPromise());
        assertEquals(lastUpdated, productStock.getLastUpdated());
        assertTrue(productStock.getUncommittedEvents().isEmpty());
    }

    @Test
    @DisplayName("Should allocate stock successfully and record event")
    void allocate_ValidQuantity_AllocatesStockAndRecordsEvent() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        int quantityToAllocate = 30;
        StockLevel previousStockLevel = StockLevel.of(initialQuantity, 0);

        // Act
        productStock.allocate(quantityToAllocate);

        // Assert
        assertEquals(initialQuantity, productStock.getQuantityOnHand());
        assertEquals(quantityToAllocate, productStock.getQuantityAllocated());
        assertEquals(initialQuantity - quantityToAllocate, productStock.getAvailableToPromise());
        assertNotNull(productStock.getLastUpdated());

        List<StockLevelChangedEvent> events = productStock.getUncommittedEvents().stream()
                .filter(e -> e instanceof StockLevelChangedEvent)
                .map(e -> (StockLevelChangedEvent) e)
                .toList();
        assertEquals(2, events.size()); // Creation event + allocation event
        StockLevelChangedEvent event = events.get(1); // Get the allocation event (creation is at index 0)
        assertEquals(sku, event.getSku());
        assertEquals(previousStockLevel.getQuantityOnHand(), event.getPreviousStockLevel().getQuantityOnHand());
        assertEquals(previousStockLevel.getQuantityAllocated(), event.getPreviousStockLevel().getQuantityAllocated());
        assertEquals(initialQuantity, event.getNewStockLevel().getQuantityOnHand());
        assertEquals(quantityToAllocate, event.getNewStockLevel().getQuantityAllocated());
        assertEquals("ALLOCATION", event.getChangeReason());
    }

    @Test
    @DisplayName("Should throw InvalidQuantityException when allocating negative quantity")
    void allocate_NegativeQuantity_ThrowsException() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        int negativeQuantity = -10;

        // Act & Assert
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                productStock.allocate(negativeQuantity));
        assertTrue(exception.getMessage().contains("must be positive"));
    }

    @Test
    @DisplayName("Should throw InvalidQuantityException when allocating zero quantity")
    void allocate_ZeroQuantity_ThrowsException() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        int zeroQuantity = 0;

        // Act & Assert
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                productStock.allocate(zeroQuantity));
        assertTrue(exception.getMessage().contains("must be positive"));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when allocating more than available to promise")
    void allocate_ExceedsAvailableToPromise_ThrowsException() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        int quantityToAllocate = initialQuantity + 1;

        // Act & Assert
        com.paklog.inventory.domain.exception.InsufficientStockException exception = assertThrows(com.paklog.inventory.domain.exception.InsufficientStockException.class, () ->
                productStock.allocate(quantityToAllocate));
        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("Should deallocate stock successfully and record event")
    void deallocate_ValidQuantity_DeallocatesStockAndRecordsEvent() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        productStock.allocate(50); // Allocate some stock first
        int quantityToDeallocate = 20;
        StockLevel previousStockLevel = productStock.getStockLevel(); // Get actual current state

        // Act
        productStock.deallocate(quantityToDeallocate);

        // Assert
        assertEquals(initialQuantity, productStock.getQuantityOnHand());
        assertEquals(30, productStock.getQuantityAllocated()); // 50 - 20
        assertEquals(70, productStock.getAvailableToPromise()); // 100 - 30
        assertNotNull(productStock.getLastUpdated());

        List<StockLevelChangedEvent> events = productStock.getUncommittedEvents().stream()
                .filter(e -> e instanceof StockLevelChangedEvent)
                .map(e -> (StockLevelChangedEvent) e)
                .toList();
        assertEquals(3, events.size()); // Creation + Allocation + Deallocation
        StockLevelChangedEvent event = events.get(2); // Get the deallocation event (creation=0, allocation=1, deallocation=2)
        assertEquals(sku, event.getSku());
        assertEquals(previousStockLevel.getQuantityOnHand(), event.getPreviousStockLevel().getQuantityOnHand());
        assertEquals(previousStockLevel.getQuantityAllocated(), event.getPreviousStockLevel().getQuantityAllocated());
        assertEquals(initialQuantity, event.getNewStockLevel().getQuantityOnHand());
        assertEquals(30, event.getNewStockLevel().getQuantityAllocated());
        assertEquals("DEALLOCATION", event.getChangeReason());
    }

    @Test
    @DisplayName("Should throw InvalidQuantityException when deallocating negative quantity")
    void deallocate_NegativeQuantity_ThrowsException() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        productStock.allocate(50);
        int negativeQuantity = -10;

        // Act & Assert
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                productStock.deallocate(negativeQuantity));
        assertTrue(exception.getMessage().contains("must be positive"));
    }

    @Test
    @DisplayName("Should throw InvalidQuantityException when deallocating zero quantity")
    void deallocate_ZeroQuantity_ThrowsException() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        productStock.allocate(50);
        int zeroQuantity = 0;

        // Act & Assert
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                productStock.deallocate(zeroQuantity));
        assertTrue(exception.getMessage().contains("must be positive"));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when deallocating more than currently allocated")
    void deallocate_ExceedsAllocatedQuantity_ThrowsException() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        productStock.allocate(50);
        int quantityToDeallocate = 60; // More than allocated

        // Act & Assert
        com.paklog.inventory.domain.exception.InsufficientStockException exception = assertThrows(com.paklog.inventory.domain.exception.InsufficientStockException.class, () ->
                productStock.deallocate(quantityToDeallocate));
        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("Should adjust quantity on hand positively and record event")
    void adjustQuantityOnHand_PositiveChange_AdjustsStockAndRecordsEvent() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        int change = 20;
        String reason = "STOCK_INTAKE";
        StockLevel previousStockLevel = StockLevel.of(initialQuantity, 0);

        // Act
        productStock.adjustQuantityOnHand(change, reason);

        // Assert
        assertEquals(initialQuantity + change, productStock.getQuantityOnHand());
        assertEquals(0, productStock.getQuantityAllocated());
        assertEquals(initialQuantity + change, productStock.getAvailableToPromise());
        assertNotNull(productStock.getLastUpdated());

        List<StockLevelChangedEvent> events = productStock.getUncommittedEvents().stream()
                .filter(e -> e instanceof StockLevelChangedEvent)
                .map(e -> (StockLevelChangedEvent) e)
                .toList();
        assertEquals(2, events.size()); // Creation + Adjustment
        StockLevelChangedEvent event = events.get(1); // Get the adjustment event
        assertEquals(sku, event.getSku());
        assertEquals(previousStockLevel.getQuantityOnHand(), event.getPreviousStockLevel().getQuantityOnHand());
        assertEquals(previousStockLevel.getQuantityAllocated(), event.getPreviousStockLevel().getQuantityAllocated());
        assertEquals(initialQuantity + change, event.getNewStockLevel().getQuantityOnHand());
        assertEquals(0, event.getNewStockLevel().getQuantityAllocated());
        assertEquals(reason, event.getChangeReason());
    }

    @Test
    @DisplayName("Should adjust quantity on hand negatively and record event")
    void adjustQuantityOnHand_NegativeChange_AdjustsStockAndRecordsEvent() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        int change = -20;
        String reason = "DAMAGE";
        StockLevel previousStockLevel = StockLevel.of(initialQuantity, 0);

        // Act
        productStock.adjustQuantityOnHand(change, reason);

        // Assert
        assertEquals(initialQuantity + change, productStock.getQuantityOnHand());
        assertEquals(0, productStock.getQuantityAllocated());
        assertEquals(initialQuantity + change, productStock.getAvailableToPromise());
        assertNotNull(productStock.getLastUpdated());

        List<StockLevelChangedEvent> events = productStock.getUncommittedEvents().stream()
                .filter(e -> e instanceof StockLevelChangedEvent)
                .map(e -> (StockLevelChangedEvent) e)
                .toList();
        assertEquals(2, events.size()); // Creation + Adjustment
        StockLevelChangedEvent event = events.get(1); // Get the adjustment event
        assertEquals(sku, event.getSku());
        assertEquals(previousStockLevel.getQuantityOnHand(), event.getPreviousStockLevel().getQuantityOnHand());
        assertEquals(previousStockLevel.getQuantityAllocated(), event.getPreviousStockLevel().getQuantityAllocated());
        assertEquals(initialQuantity + change, event.getNewStockLevel().getQuantityOnHand());
        assertEquals(0, event.getNewStockLevel().getQuantityAllocated());
        assertEquals(reason, event.getChangeReason());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adjusting quantity on hand results in negative stock")
    void adjustQuantityOnHand_ResultsInNegativeStock_ThrowsException() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        int change = -(initialQuantity + 1); // More than available
        String reason = "LOSS";

        // Act & Assert
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                productStock.adjustQuantityOnHand(change, reason));
        assertTrue(exception.getMessage().contains("negative"));
    }

    @Test
    @DisplayName("Should receive stock successfully and record event")
    void receiveStock_ValidQuantity_ReceivesStockAndRecordsEvent() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        int quantityReceived = 50;
        StockLevel previousStockLevel = StockLevel.of(initialQuantity, 0);

        // Act
        productStock.receiveStock(quantityReceived);

        // Assert
        assertEquals(initialQuantity + quantityReceived, productStock.getQuantityOnHand());
        assertEquals(0, productStock.getQuantityAllocated());
        assertEquals(initialQuantity + quantityReceived, productStock.getAvailableToPromise());
        assertNotNull(productStock.getLastUpdated());

        List<StockLevelChangedEvent> events = productStock.getUncommittedEvents().stream()
                .filter(e -> e instanceof StockLevelChangedEvent)
                .map(e -> (StockLevelChangedEvent) e)
                .toList();
        assertEquals(2, events.size()); // Creation + Receipt
        StockLevelChangedEvent event = events.get(1); // Get the receipt event (creation is at index 0)
        assertEquals(sku, event.getSku());
        assertEquals(previousStockLevel.getQuantityOnHand(), event.getPreviousStockLevel().getQuantityOnHand());
        assertEquals(previousStockLevel.getQuantityAllocated(), event.getPreviousStockLevel().getQuantityAllocated());
        assertEquals(initialQuantity + quantityReceived, event.getNewStockLevel().getQuantityOnHand());
        assertEquals(0, event.getNewStockLevel().getQuantityAllocated());
        assertEquals("STOCK_RECEIPT", event.getChangeReason());
    }

    @Test
    @DisplayName("Should throw InvalidQuantityException when receiving negative quantity")
    void receiveStock_NegativeQuantity_ThrowsException() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        int negativeQuantity = -10;

        // Act & Assert
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                productStock.receiveStock(negativeQuantity));
        assertTrue(exception.getMessage().contains("must be positive"));
    }

    @Test
    @DisplayName("Should throw InvalidQuantityException when receiving zero quantity")
    void receiveStock_ZeroQuantity_ThrowsException() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        int zeroQuantity = 0;

        // Act & Assert
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                productStock.receiveStock(zeroQuantity));
        assertTrue(exception.getMessage().contains("must be positive"));
    }

    @Test
    @DisplayName("Should return uncommitted events")
    void getUncommittedEvents_ReturnsEvents() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        productStock.allocate(10);

        // Act
        List<com.paklog.inventory.domain.event.DomainEvent> events = productStock.getUncommittedEvents();

        // Assert
        assertFalse(events.isEmpty());
        assertEquals(2, events.size()); // Creation + Allocation
    }

    @Test
    @DisplayName("Should clear uncommitted events after marking as committed")
    void markEventsAsCommitted_ClearsEvents() {
        // Arrange
        ProductStock productStock = ProductStock.create(sku, initialQuantity);
        productStock.allocate(10);
        assertFalse(productStock.getUncommittedEvents().isEmpty());

        // Act
        productStock.markEventsAsCommitted();

        // Assert
        assertTrue(productStock.getUncommittedEvents().isEmpty());
    }

    @Test
    @DisplayName("Maintain invariants after load")
    void maintainInvariants() {
        // Arrange
        ProductStock productStock = ProductStock.load(sku, 100, 50, LocalDateTime.now());

        // Act
        productStock.validateInvariants();

        // Assert - no exception thrown
        assertNotNull(productStock);
    }

    @Test
    @DisplayName("Load with negative on hand throws InvalidQuantityException")
    void validateInvariants_NegativeOnHand_ThrowsException() {
        // Act & Assert - Exception is thrown during load, not validateInvariants
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                ProductStock.load(sku, -10, 0, LocalDateTime.now()));
        assertTrue(exception.getMessage().contains("cannot be negative"));
    }

    @Test
    @DisplayName("Load with negative allocated throws InvalidQuantityException")
    void validateInvariants_NegativeAllocated_ThrowsException() {
        // Act & Assert - Exception is thrown during load, not validateInvariants
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                ProductStock.load(sku, 100, -10, LocalDateTime.now()));
        assertTrue(exception.getMessage().contains("cannot be negative"));
    }

    @Test
    @DisplayName("Load with allocated exceeds on hand throws StockLevelInvariantViolationException")
    void validateInvariants_AllocatedExceedsOnHand_ThrowsException() {
        // Act & Assert - Exception is thrown during load, not validateInvariants
        com.paklog.inventory.domain.exception.StockLevelInvariantViolationException exception = assertThrows(com.paklog.inventory.domain.exception.StockLevelInvariantViolationException.class, () ->
                ProductStock.load(sku, 50, 60, LocalDateTime.now()));
        assertTrue(exception.getMessage().contains("cannot exceed"));
    }

    @Test
    @DisplayName("Equals and HashCode should work based on SKU")
    void equalsAndHashCode_BasedOnSku() {
        // Arrange
        ProductStock productStock1 = ProductStock.create(sku, initialQuantity);
        ProductStock productStock2 = ProductStock.create(sku, 50); // Different quantity, same SKU
        ProductStock productStock3 = ProductStock.create("ANOTHER_SKU", initialQuantity);

        // Assert
        assertEquals(productStock1, productStock2);
        assertNotEquals(productStock1, productStock3);
        assertEquals(productStock1.hashCode(), productStock2.hashCode());
        assertNotEquals(productStock1.hashCode(), productStock3.hashCode());
    }

    @Test
    @DisplayName("Invariant violation: Quantity on hand cannot be negative after load")
    void load_NegativeQuantityOnHand_ThrowsIllegalStateException() {
        // Arrange
        int quantityOnHand = -10;
        int quantityAllocated = 0;
        LocalDateTime lastUpdated = LocalDateTime.now();

        // Act & Assert
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                ProductStock.load(sku, quantityOnHand, quantityAllocated, lastUpdated));
        assertTrue(exception.getMessage().contains("cannot be negative"));
    }

    @Test
    @DisplayName("Invariant violation: Quantity allocated cannot be negative after load")
    void load_NegativeQuantityAllocated_ThrowsIllegalStateException() {
        // Arrange
        int quantityOnHand = 100;
        int quantityAllocated = -10;
        LocalDateTime lastUpdated = LocalDateTime.now();

        // Act & Assert
        com.paklog.inventory.domain.exception.InvalidQuantityException exception = assertThrows(com.paklog.inventory.domain.exception.InvalidQuantityException.class, () ->
                ProductStock.load(sku, quantityOnHand, quantityAllocated, lastUpdated));
        assertTrue(exception.getMessage().contains("cannot be negative"));
    }

    @Test
    @DisplayName("Invariant violation: Allocated quantity cannot exceed quantity on hand after load")
    void load_AllocatedExceedsOnHand_ThrowsIllegalStateException() {
        // Arrange
        int quantityOnHand = 50;
        int quantityAllocated = 60;
        LocalDateTime lastUpdated = LocalDateTime.now();

        // Act & Assert
        StockLevelInvariantViolationException exception = assertThrows(StockLevelInvariantViolationException.class, () ->
                ProductStock.load(sku, quantityOnHand, quantityAllocated, lastUpdated));
        assertEquals("Allocated quantity cannot exceed quantity on hand", exception.getInvariantRule());
        assertEquals(quantityOnHand, exception.getQuantityOnHand());
        assertEquals(quantityAllocated, exception.getQuantityAllocated());
    }
}