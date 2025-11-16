package com.paklog.inventory.application.validator;

import com.paklog.inventory.domain.exception.ProductNotFoundInCatalogException;
import com.paklog.inventory.infrastructure.client.ProductCatalogClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductExistenceValidatorTest {

    private ProductCatalogClient productCatalogClient;
    private ProductExistenceValidator validator;

    @BeforeEach
    void setUp() {
        productCatalogClient = mock(ProductCatalogClient.class);
    }

    @Test
    void validateProductExists_shouldNotThrowException_whenProductExistsInCatalog() {
        // Given
        String sku = "TEST-SKU-001";
        validator = new ProductExistenceValidator(productCatalogClient, true);

        when(productCatalogClient.productExists(sku)).thenReturn(true);

        // When & Then
        assertDoesNotThrow(() -> validator.validateProductExists(sku));
        verify(productCatalogClient).productExists(sku);
    }

    @Test
    void validateProductExists_shouldThrowException_whenProductNotFoundInCatalog() {
        // Given
        String sku = "NONEXISTENT-SKU";
        validator = new ProductExistenceValidator(productCatalogClient, true);

        when(productCatalogClient.productExists(sku)).thenReturn(false);

        // When & Then
        ProductNotFoundInCatalogException exception = assertThrows(
                ProductNotFoundInCatalogException.class,
                () -> validator.validateProductExists(sku)
        );

        assertEquals("NONEXISTENT-SKU", exception.getSku());
        assertTrue(exception.getMessage().contains("NONEXISTENT-SKU"));
        assertTrue(exception.getMessage().contains("Product Catalog"));
        verify(productCatalogClient).productExists(sku);
    }

    @Test
    void validateProductExists_shouldSkipValidation_whenValidationDisabled() {
        // Given
        String sku = "ANY-SKU";
        validator = new ProductExistenceValidator(productCatalogClient, false);

        // When & Then
        assertDoesNotThrow(() -> validator.validateProductExists(sku));
        verify(productCatalogClient, never()).productExists(anyString());
    }

    @Test
    void isValidationEnabled_shouldReturnTrue_whenEnabled() {
        // Given
        validator = new ProductExistenceValidator(productCatalogClient, true);

        // When & Then
        assertTrue(validator.isValidationEnabled());
    }

    @Test
    void isValidationEnabled_shouldReturnFalse_whenDisabled() {
        // Given
        validator = new ProductExistenceValidator(productCatalogClient, false);

        // When & Then
        assertFalse(validator.isValidationEnabled());
    }

    @Test
    void validateProductExists_shouldHandleNullSku_whenValidationEnabled() {
        // Given
        validator = new ProductExistenceValidator(productCatalogClient, true);

        when(productCatalogClient.productExists(null)).thenReturn(false);

        // When & Then
        assertThrows(
                ProductNotFoundInCatalogException.class,
                () -> validator.validateProductExists(null)
        );
    }

    @Test
    void validateProductExists_shouldHandleEmptySku_whenValidationEnabled() {
        // Given
        String emptySku = "";
        validator = new ProductExistenceValidator(productCatalogClient, true);

        when(productCatalogClient.productExists(emptySku)).thenReturn(false);

        // When & Then
        ProductNotFoundInCatalogException exception = assertThrows(
                ProductNotFoundInCatalogException.class,
                () -> validator.validateProductExists(emptySku)
        );

        assertEquals("", exception.getSku());
    }
}
