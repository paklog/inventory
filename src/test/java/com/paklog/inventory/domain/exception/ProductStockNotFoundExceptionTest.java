package com.paklog.inventory.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductStockNotFoundExceptionTest {

    @Test
    @DisplayName("Should create exception with SKU")
    void constructor_WithSku_CreatesExceptionWithCorrectMessage() {
        // Arrange
        String sku = "MISSING-SKU-001";

        // Act
        ProductStockNotFoundException exception = new ProductStockNotFoundException(sku);

        // Assert
        assertEquals(sku, exception.getSku());
        assertEquals("ProductStock not found for SKU: " + sku, exception.getMessage());
    }

    @Test
    @DisplayName("Should be a domain exception")
    void productStockNotFoundException_IsDomainException() {
        // Arrange & Act
        ProductStockNotFoundException exception = new ProductStockNotFoundException("TEST-SKU");

        // Assert
        assertInstanceOf(DomainException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Should handle null SKU correctly")
    void constructor_WithNullSku_HandlesCorrectly() {
        // Arrange & Act
        ProductStockNotFoundException exception = new ProductStockNotFoundException(null);

        // Assert
        assertNull(exception.getSku());
        assertEquals("ProductStock not found for SKU: null", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle empty SKU correctly")
    void constructor_WithEmptySku_HandlesCorrectly() {
        // Arrange
        String sku = "";

        // Act
        ProductStockNotFoundException exception = new ProductStockNotFoundException(sku);

        // Assert
        assertEquals("", exception.getSku());
        assertEquals("ProductStock not found for SKU: ", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle complex SKU formats correctly")
    void constructor_WithComplexSkuFormats_HandlesCorrectly() {
        // Arrange
        String[] skus = {
            "SKU-123-ABC-XYZ",
            "product.variant.001",
            "ITEM_WITH_UNDERSCORE_999",
            "123456789",
            "a"
        };

        for (String sku : skus) {
            // Act
            ProductStockNotFoundException exception = new ProductStockNotFoundException(sku);

            // Assert
            assertEquals(sku, exception.getSku());
            assertTrue(exception.getMessage().contains(sku));
        

}
}
}
