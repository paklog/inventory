package com.paklog.inventory.application.service;

import com.paklog.inventory.application.validator.ProductExistenceValidator;
import com.paklog.inventory.domain.exception.ProductNotFoundInCatalogException;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.InventoryLedgerRepository;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.infrastructure.client.ProductCatalogClient;
import com.paklog.inventory.infrastructure.metrics.InventoryMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Product Catalog validation in InventoryCommandService.
 * Tests the complete flow from service method to catalog validation.
 */
@ExtendWith(MockitoExtension.class)
class InventoryCommandServiceProductValidationIntegrationTest {

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private InventoryLedgerRepository inventoryLedgerRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private InventoryMetricsService metricsService;

    @Mock
    private ProductCatalogClient productCatalogClient;

    private ProductExistenceValidator productExistenceValidator;
    private InventoryCommandService inventoryCommandService;

    @BeforeEach
    void setUp() {
        // Initialize validator with validation enabled
        productExistenceValidator = new ProductExistenceValidator(productCatalogClient, true);

        // Initialize service with validator
        inventoryCommandService = new InventoryCommandService(
                productStockRepository,
                inventoryLedgerRepository,
                outboxRepository,
                metricsService,
                productExistenceValidator
        );
    }

    @Test
    void receiveStock_shouldValidateProductExists_whenCreatingNewProductStock() {
        // Given
        String sku = "NEW-SKU-001";
        int quantity = 100;
        String receiptId = "RECEIPT-001";

        // Product does not exist in inventory yet
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.empty());

        // Product exists in catalog
        when(productCatalogClient.productExists(sku)).thenReturn(true);

        // Mock save operation
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductStock result = inventoryCommandService.receiveStock(sku, quantity, receiptId);

        // Then
        assertNotNull(result);
        assertEquals(sku, result.getSku());
        verify(productCatalogClient).productExists(sku);
        verify(productStockRepository).save(any(ProductStock.class));
    }

    @Test
    void receiveStock_shouldThrowException_whenProductNotFoundInCatalog() {
        // Given
        String sku = "NONEXISTENT-SKU";
        int quantity = 100;
        String receiptId = "RECEIPT-001";

        // Product does not exist in inventory yet
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.empty());

        // Product does NOT exist in catalog
        when(productCatalogClient.productExists(sku)).thenReturn(false);

        // When & Then
        ProductNotFoundInCatalogException exception = assertThrows(
                ProductNotFoundInCatalogException.class,
                () -> inventoryCommandService.receiveStock(sku, quantity, receiptId)
        );

        assertEquals(sku, exception.getSku());
        verify(productCatalogClient).productExists(sku);
        verify(productStockRepository, never()).save(any(ProductStock.class));
    }

    @Test
    void receiveStock_shouldNotValidate_whenProductAlreadyExistsInInventory() {
        // Given
        String sku = "EXISTING-SKU";
        int quantity = 100;
        String receiptId = "RECEIPT-001";

        // Product already exists in inventory
        ProductStock existingStock = ProductStock.create(sku, 50);
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.of(existingStock));

        // Mock save operation
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductStock result = inventoryCommandService.receiveStock(sku, quantity, receiptId);

        // Then
        assertNotNull(result);
        assertEquals(sku, result.getSku());
        // Should NOT call catalog client because product already exists in inventory
        verify(productCatalogClient, never()).productExists(anyString());
        verify(productStockRepository).save(any(ProductStock.class));
    }

    @Test
    void increaseQuantityOnHand_shouldValidateProductExists_whenCreatingNewProductStock() {
        // Given
        String sku = "NEW-SKU-002";
        int quantity = 50;

        // Product does not exist in inventory yet
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.empty());

        // Product exists in catalog
        when(productCatalogClient.productExists(sku)).thenReturn(true);

        // Mock save operation
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        inventoryCommandService.increaseQuantityOnHand(sku, quantity);

        // Then
        verify(productCatalogClient).productExists(sku);
        verify(productStockRepository).save(any(ProductStock.class));
    }

    @Test
    void increaseQuantityOnHand_shouldThrowException_whenProductNotFoundInCatalog() {
        // Given
        String sku = "NONEXISTENT-SKU";
        int quantity = 50;

        // Product does not exist in inventory yet
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.empty());

        // Product does NOT exist in catalog
        when(productCatalogClient.productExists(sku)).thenReturn(false);

        // When & Then
        ProductNotFoundInCatalogException exception = assertThrows(
                ProductNotFoundInCatalogException.class,
                () -> inventoryCommandService.increaseQuantityOnHand(sku, quantity)
        );

        assertEquals(sku, exception.getSku());
        verify(productCatalogClient).productExists(sku);
        verify(productStockRepository, never()).save(any(ProductStock.class));
    }

    @Test
    void increaseQuantityOnHand_shouldNotValidate_whenProductAlreadyExistsInInventory() {
        // Given
        String sku = "EXISTING-SKU";
        int quantity = 50;

        // Product already exists in inventory
        ProductStock existingStock = ProductStock.create(sku, 100);
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.of(existingStock));

        // Mock save operation
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        inventoryCommandService.increaseQuantityOnHand(sku, quantity);

        // Then
        // Should NOT call catalog client because product already exists in inventory
        verify(productCatalogClient, never()).productExists(anyString());
        verify(productStockRepository).save(any(ProductStock.class));
    }

    @Test
    void receiveStock_withValidationDisabled_shouldNotValidateProduct() {
        // Given
        String sku = "NEW-SKU-003";
        int quantity = 100;
        String receiptId = "RECEIPT-003";

        // Create validator with validation disabled
        ProductExistenceValidator disabledValidator = new ProductExistenceValidator(productCatalogClient, false);
        InventoryCommandService serviceWithDisabledValidation = new InventoryCommandService(
                productStockRepository,
                inventoryLedgerRepository,
                outboxRepository,
                metricsService,
                disabledValidator
        );

        // Product does not exist in inventory yet
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.empty());

        // Mock save operation
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductStock result = serviceWithDisabledValidation.receiveStock(sku, quantity, receiptId);

        // Then
        assertNotNull(result);
        assertEquals(sku, result.getSku());
        // Should NOT call catalog client when validation is disabled
        verify(productCatalogClient, never()).productExists(anyString());
        verify(productStockRepository).save(any(ProductStock.class));
    }
}
