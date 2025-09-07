package com.paklog.inventory.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StockLevelInvariantViolationExceptionTest {

    @Test
    @DisplayName("Should create exception with invariant rule and quantities")
    void constructor_WithRuleAndQuantities_CreatesExceptionWithCorrectMessage() {
        // Arrange
        String invariantRule = "Allocated quantity cannot exceed quantity on hand";
        int quantityOnHand = 50;
        int quantityAllocated = 60;

        // Act
        StockLevelInvariantViolationException exception = new StockLevelInvariantViolationException(
                invariantRule, quantityOnHand, quantityAllocated);

        // Assert
        assertEquals(invariantRule, exception.getInvariantRule());
        assertEquals(quantityOnHand, exception.getQuantityOnHand());
        assertEquals(quantityAllocated, exception.getQuantityAllocated());
        
        String message = exception.getMessage();
        assertTrue(message.contains("Stock level invariant violation"));
        assertTrue(message.contains(invariantRule));
        assertTrue(message.contains("quantityOnHand=50"));
        assertTrue(message.contains("quantityAllocated=60"));
    }

    @Test
    @DisplayName("Should be a domain exception")
    void stockLevelInvariantViolationException_IsDomainException() {
        // Arrange & Act
        StockLevelInvariantViolationException exception = new StockLevelInvariantViolationException(
                "Test rule", 10, 5);

        // Assert
        assertInstanceOf(DomainException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should handle negative quantities correctly")
    void constructor_WithNegativeQuantities_HandlesCorrectly() {
        // Arrange
        String invariantRule = "Quantity on hand cannot be negative";
        int quantityOnHand = -10;
        int quantityAllocated = 0;

        // Act
        StockLevelInvariantViolationException exception = new StockLevelInvariantViolationException(
                invariantRule, quantityOnHand, quantityAllocated);

        // Assert
        assertEquals(-10, exception.getQuantityOnHand());
        assertEquals(0, exception.getQuantityAllocated());
        assertTrue(exception.getMessage().contains("quantityOnHand=-10"));
        assertTrue(exception.getMessage().contains("quantityAllocated=0"));
    }

    @Test
    @DisplayName("Should handle zero quantities correctly")
    void constructor_WithZeroQuantities_HandlesCorrectly() {
        // Arrange
        String invariantRule = "Both quantities cannot be zero in this context";
        int quantityOnHand = 0;
        int quantityAllocated = 0;

        // Act
        StockLevelInvariantViolationException exception = new StockLevelInvariantViolationException(
                invariantRule, quantityOnHand, quantityAllocated);

        // Assert
        assertEquals(0, exception.getQuantityOnHand());
        assertEquals(0, exception.getQuantityAllocated());
        assertTrue(exception.getMessage().contains("quantityOnHand=0"));
        assertTrue(exception.getMessage().contains("quantityAllocated=0"));
    }

    @Test
    @DisplayName("Should handle different invariant rules correctly")
    void constructor_WithDifferentRules_HandlesCorrectly() {
        // Arrange & Act
        StockLevelInvariantViolationException exception1 = new StockLevelInvariantViolationException(
                "Quantity on hand cannot be negative", -5, 0);
        
        StockLevelInvariantViolationException exception2 = new StockLevelInvariantViolationException(
                "Quantity allocated cannot be negative", 100, -10);
        
        StockLevelInvariantViolationException exception3 = new StockLevelInvariantViolationException(
                "Allocated quantity cannot exceed quantity on hand", 30, 40);

        // Assert
        assertEquals("Quantity on hand cannot be negative", exception1.getInvariantRule());
        assertEquals("Quantity allocated cannot be negative", exception2.getInvariantRule());
        assertEquals("Allocated quantity cannot exceed quantity on hand", exception3.getInvariantRule());
        
        assertTrue(exception1.getMessage().contains("Quantity on hand cannot be negative"));
        assertTrue(exception2.getMessage().contains("Quantity allocated cannot be negative"));
        assertTrue(exception3.getMessage().contains("Allocated quantity cannot exceed quantity on hand"));
    }

    @Test
    @DisplayName("Should handle large quantities correctly")
    void constructor_WithLargeQuantities_HandlesCorrectly() {
        // Arrange
        String invariantRule = "Test with large numbers";
        int quantityOnHand = Integer.MAX_VALUE;
        int quantityAllocated = Integer.MAX_VALUE - 1;

        // Act
        StockLevelInvariantViolationException exception = new StockLevelInvariantViolationException(
                invariantRule, quantityOnHand, quantityAllocated);

        // Assert
        assertEquals(Integer.MAX_VALUE, exception.getQuantityOnHand());
        assertEquals(Integer.MAX_VALUE - 1, exception.getQuantityAllocated());
    }
}