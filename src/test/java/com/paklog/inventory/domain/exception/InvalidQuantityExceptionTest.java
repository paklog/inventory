package com.paklog.inventory.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidQuantityExceptionTest {

    @Test
    @DisplayName("Should create exception with operation and quantity")
    void constructor_WithOperationAndQuantity_CreatesExceptionWithCorrectMessage() {
        // Arrange
        String operation = "allocate";
        int quantity = -5;

        // Act
        InvalidQuantityException exception = new InvalidQuantityException(operation, quantity);

        // Assert
        assertEquals(operation, exception.getOperation());
        assertEquals(quantity, exception.getQuantity());
        assertTrue(exception.getMessage().contains(operation));
        assertTrue(exception.getMessage().contains("-5"));
        assertTrue(exception.getMessage().contains("Invalid quantity"));
    }

    @Test
    @DisplayName("Should create exception with operation, quantity and reason")
    void constructor_WithOperationQuantityAndReason_CreatesExceptionWithCorrectMessage() {
        // Arrange
        String operation = "deallocate";
        int quantity = 100;
        String reason = "Quantity exceeds allocated amount";

        // Act
        InvalidQuantityException exception = new InvalidQuantityException(operation, quantity, reason);

        // Assert
        assertEquals(operation, exception.getOperation());
        assertEquals(quantity, exception.getQuantity());
        String message = exception.getMessage();
        assertTrue(message.contains(operation));
        assertTrue(message.contains("100"));
        assertTrue(message.contains(reason));
    }

    @Test
    @DisplayName("Should be a domain exception")
    void invalidQuantityException_IsDomainException() {
        // Arrange & Act
        InvalidQuantityException exception = new InvalidQuantityException("test", -1);

        // Assert
        assertInstanceOf(DomainException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should handle zero quantity correctly")
    void constructor_WithZeroQuantity_HandlesCorrectly() {
        // Arrange
        String operation = "receive";
        int quantity = 0;
        String reason = "Received quantity must be positive";

        // Act
        InvalidQuantityException exception = new InvalidQuantityException(operation, quantity, reason);

        // Assert
        assertEquals(0, exception.getQuantity());
        assertTrue(exception.getMessage().contains("0"));
        assertTrue(exception.getMessage().contains(reason));
    }

    @Test
    @DisplayName("Should handle different operations correctly")
    void constructor_WithDifferentOperations_HandlesCorrectly() {
        // Arrange & Act
        InvalidQuantityException allocateException = new InvalidQuantityException("allocate", -10);
        InvalidQuantityException adjustException = new InvalidQuantityException("adjust", -50, "Cannot adjust negative");
        InvalidQuantityException createException = new InvalidQuantityException("create stock level", -1, "Negative not allowed");

        // Assert
        assertEquals("allocate", allocateException.getOperation());
        assertEquals("adjust", adjustException.getOperation());
        assertEquals("create stock level", createException.getOperation());
        
        assertTrue(allocateException.getMessage().contains("allocate"));
        assertTrue(adjustException.getMessage().contains("adjust"));
        assertTrue(createException.getMessage().contains("create stock level"));
    }
}