package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.exception.InvalidQuantityException;
import com.paklog.inventory.domain.exception.StockLevelInvariantViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StockLevelTest {

    @Test
    @DisplayName("Should create StockLevel with valid quantities")
    void of_ValidQuantities_CreatesStockLevel() {
        // Arrange
        int quantityOnHand = 100;
        int quantityAllocated = 20;

        // Act
        StockLevel stockLevel = StockLevel.of(quantityOnHand, quantityAllocated);

        // Assert
        assertNotNull(stockLevel);
        assertEquals(quantityOnHand, stockLevel.getQuantityOnHand());
        assertEquals(quantityAllocated, stockLevel.getQuantityAllocated());
        assertEquals(quantityOnHand - quantityAllocated, stockLevel.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should throw InvalidQuantityException when quantity on hand is negative")
    void of_NegativeQuantityOnHand_ThrowsException() {
        // Arrange
        int quantityOnHand = -10;
        int quantityAllocated = 0;

        // Act & Assert
        InvalidQuantityException exception = assertThrows(InvalidQuantityException.class, () ->
                StockLevel.of(quantityOnHand, quantityAllocated));
        assertTrue(exception.getMessage().contains("Quantity on hand cannot be negative"));
        assertEquals("create stock level", exception.getOperation());
        assertEquals(-10, exception.getQuantity());
    }

    @Test
    @DisplayName("Should throw InvalidQuantityException when quantity allocated is negative")
    void of_NegativeQuantityAllocated_ThrowsException() {
        // Arrange
        int quantityOnHand = 100;
        int quantityAllocated = -10;

        // Act & Assert
        InvalidQuantityException exception = assertThrows(InvalidQuantityException.class, () ->
                StockLevel.of(quantityOnHand, quantityAllocated));
        assertTrue(exception.getMessage().contains("Quantity allocated cannot be negative"));
        assertEquals("create stock level", exception.getOperation());
        assertEquals(-10, exception.getQuantity());
    }

    @Test
    @DisplayName("Should throw StockLevelInvariantViolationException when allocated quantity exceeds quantity on hand")
    void of_AllocatedExceedsOnHand_ThrowsException() {
        // Arrange
        int quantityOnHand = 50;
        int quantityAllocated = 60;

        // Act & Assert
        StockLevelInvariantViolationException exception = assertThrows(StockLevelInvariantViolationException.class, () ->
                StockLevel.of(quantityOnHand, quantityAllocated));
        assertTrue(exception.getMessage().contains("Allocated quantity cannot exceed quantity on hand"));
        assertEquals("Allocated quantity cannot exceed quantity on hand", exception.getInvariantRule());
        assertEquals(50, exception.getQuantityOnHand());
        assertEquals(60, exception.getQuantityAllocated());
    }

    @Test
    @DisplayName("Should calculate available to promise correctly")
    void getAvailableToPromise_CalculatesCorrectly() {
        // Arrange
        StockLevel stockLevel = StockLevel.of(100, 30);

        // Act
        int availableToPromise = stockLevel.getAvailableToPromise();

        // Assert
        assertEquals(70, availableToPromise);
    }

    @Test
    @DisplayName("Should create new StockLevel with increased allocation")
    void withAllocation_IncreasesAllocation() {
        // Arrange
        StockLevel initialStockLevel = StockLevel.of(100, 20);
        int quantityToAllocate = 30;

        // Act
        StockLevel newStockLevel = initialStockLevel.withAllocation(quantityToAllocate);

        // Assert
        assertNotSame(initialStockLevel, newStockLevel); // Ensure immutability
        assertEquals(100, newStockLevel.getQuantityOnHand());
        assertEquals(50, newStockLevel.getQuantityAllocated()); // 20 + 30
        assertEquals(50, newStockLevel.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should create new StockLevel with decreased allocation")
    void withDeallocation_DecreasesAllocation() {
        // Arrange
        StockLevel initialStockLevel = StockLevel.of(100, 50);
        int quantityToDeallocate = 20;

        // Act
        StockLevel newStockLevel = initialStockLevel.withDeallocation(quantityToDeallocate);

        // Assert
        assertNotSame(initialStockLevel, newStockLevel); // Ensure immutability
        assertEquals(100, newStockLevel.getQuantityOnHand());
        assertEquals(30, newStockLevel.getQuantityAllocated()); // 50 - 20
        assertEquals(70, newStockLevel.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should create new StockLevel with positive quantity change")
    void withQuantityChange_PositiveChange_IncreasesQuantityOnHand() {
        // Arrange
        StockLevel initialStockLevel = StockLevel.of(100, 20);
        int change = 50;

        // Act
        StockLevel newStockLevel = initialStockLevel.withQuantityChange(change);

        // Assert
        assertNotSame(initialStockLevel, newStockLevel); // Ensure immutability
        assertEquals(150, newStockLevel.getQuantityOnHand()); // 100 + 50
        assertEquals(20, newStockLevel.getQuantityAllocated());
        assertEquals(130, newStockLevel.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should create new StockLevel with negative quantity change")
    void withQuantityChange_NegativeChange_DecreasesQuantityOnHand() {
        // Arrange
        StockLevel initialStockLevel = StockLevel.of(100, 20);
        int change = -30;

        // Act
        StockLevel newStockLevel = initialStockLevel.withQuantityChange(change);

        // Assert
        assertNotSame(initialStockLevel, newStockLevel); // Ensure immutability
        assertEquals(70, newStockLevel.getQuantityOnHand()); // 100 - 30
        assertEquals(20, newStockLevel.getQuantityAllocated());
        assertEquals(50, newStockLevel.getAvailableToPromise());
    }

    @Test
    @DisplayName("Equals and HashCode should work based on quantities")
    void equalsAndHashCode_BasedOnQuantities() {
        // Arrange
        StockLevel stockLevel1 = StockLevel.of(100, 20);
        StockLevel stockLevel2 = StockLevel.of(100, 20);
        StockLevel stockLevel3 = StockLevel.of(100, 30);
        StockLevel stockLevel4 = StockLevel.of(90, 20);

        // Assert
        assertEquals(stockLevel1, stockLevel2);
        assertNotEquals(stockLevel1, stockLevel3);
        assertNotEquals(stockLevel1, stockLevel4);
        assertEquals(stockLevel1.hashCode(), stockLevel2.hashCode());
        assertNotEquals(stockLevel1.hashCode(), stockLevel3.hashCode());
        assertNotEquals(stockLevel1.hashCode(), stockLevel4.hashCode());
    }

    @Test
    @DisplayName("toString should return correct string representation")
    void toString_ReturnsCorrectString() {
        // Arrange
        StockLevel stockLevel = StockLevel.of(100, 20);

        // Act
        String toStringResult = stockLevel.toString();

        // Assert
        assertTrue(toStringResult.contains("quantityOnHand=100"));
        assertTrue(toStringResult.contains("quantityAllocated=20"));
    }
}