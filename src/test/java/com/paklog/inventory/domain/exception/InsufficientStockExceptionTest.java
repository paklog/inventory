package com.paklog.inventory.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InsufficientStockExceptionTest {

    @Test
    @DisplayName("Should create exception with SKU and quantity details")
    void constructor_WithSkuAndQuantities_CreatesExceptionWithCorrectMessage() {
        // Arrange
        String sku = "TEST-SKU-001";
        int requestedQuantity = 50;
        int availableQuantity = 30;

        // Act
        InsufficientStockException exception = new InsufficientStockException(sku, requestedQuantity, availableQuantity);

        // Assert
        assertEquals(sku, exception.getSku());
        assertEquals(requestedQuantity, exception.getRequestedQuantity());
        assertEquals(availableQuantity, exception.getAvailableQuantity());
        assertTrue(exception.getMessage().contains(sku));
        assertTrue(exception.getMessage().contains("50"));
        assertTrue(exception.getMessage().contains("30"));
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    @DisplayName("Should be a domain exception")
    void insufficientStockException_IsDomainException() {
        // Arrange & Act
        InsufficientStockException exception = new InsufficientStockException("SKU001", 10, 5);

        // Assert
        assertInstanceOf(DomainException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should format message correctly with zero available quantity")
    void constructor_WithZeroAvailableQuantity_FormatsMessageCorrectly() {
        // Arrange
        String sku = "OUT-OF-STOCK";
        int requestedQuantity = 10;
        int availableQuantity = 0;

        // Act
        InsufficientStockException exception = new InsufficientStockException(sku, requestedQuantity, availableQuantity);

        // Assert
        String message = exception.getMessage();
        assertTrue(message.contains("OUT-OF-STOCK"));
        assertTrue(message.contains("requested 10"));
        assertTrue(message.contains("only 0 available"));
    }

    @Test
    @DisplayName("Should handle large quantities correctly")
    void constructor_WithLargeQuantities_HandlesCorrectly() {
        // Arrange
        String sku = "BULK-ITEM";
        int requestedQuantity = 999999;
        int availableQuantity = 500000;

        // Act
        InsufficientStockException exception = new InsufficientStockException(sku, requestedQuantity, availableQuantity);

        // Assert
        assertEquals(sku, exception.getSku());
        assertEquals(requestedQuantity, exception.getRequestedQuantity());
        assertEquals(availableQuantity, exception.getAvailableQuantity());
        assertTrue(exception.getMessage().contains("999999"));
        assertTrue(exception.getMessage().contains("500000"));
    }
}