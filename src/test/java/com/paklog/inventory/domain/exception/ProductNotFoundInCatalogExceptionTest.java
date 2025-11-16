package com.paklog.inventory.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductNotFoundInCatalogExceptionTest {

    @Test
    void constructor_shouldSetSkuAndMessage() {
        // Given
        String sku = "TEST-SKU-001";

        // When
        ProductNotFoundInCatalogException exception = new ProductNotFoundInCatalogException(sku);

        // Then
        assertEquals(sku, exception.getSku());
        assertTrue(exception.getMessage().contains(sku));
        assertTrue(exception.getMessage().contains("Product Catalog"));
        assertNull(exception.getCause());
    }

    @Test
    void constructorWithCause_shouldSetSkuMessageAndCause() {
        // Given
        String sku = "TEST-SKU-001";
        Exception cause = new RuntimeException("Network error");

        // When
        ProductNotFoundInCatalogException exception = new ProductNotFoundInCatalogException(sku, cause);

        // Then
        assertEquals(sku, exception.getSku());
        assertTrue(exception.getMessage().contains(sku));
        assertTrue(exception.getMessage().contains("Product Catalog"));
        assertEquals(cause, exception.getCause());
    }

    @Test
    void exception_shouldExtendDomainException() {
        // Given
        String sku = "TEST-SKU-001";

        // When
        ProductNotFoundInCatalogException exception = new ProductNotFoundInCatalogException(sku);

        // Then
        assertInstanceOf(DomainException.class, exception);
    }

    @Test
    void getMessage_shouldContainGuidanceToRegisterProduct() {
        // Given
        String sku = "TEST-SKU-001";

        // When
        ProductNotFoundInCatalogException exception = new ProductNotFoundInCatalogException(sku);

        // Then
        String message = exception.getMessage();
        assertTrue(message.contains("register the product"));
        assertTrue(message.contains("catalog"));
        assertTrue(message.contains("inventory"));
    }
}
