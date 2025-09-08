package com.paklog.inventory.application.service;

import com.paklog.inventory.application.dto.InventoryHealthMetricsResponse;
import com.paklog.inventory.application.dto.StockLevelResponse;
import com.paklog.inventory.domain.exception.ProductStockNotFoundException;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.repository.InventoryLedgerRepository;
import com.paklog.inventory.infrastructure.metrics.InventoryMetricsService;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryQueryServiceTest {

    @Mock
    private ProductStockRepository productStockRepository;
    
    @Mock
    private InventoryLedgerRepository inventoryLedgerRepository;
    
    @Mock
    private InventoryMetricsService metricsService;

    @InjectMocks
    private InventoryQueryService inventoryQueryService;

    private String sku;
    private ProductStock productStock;

    @BeforeEach
    void setUp() {
        sku = "PROD001";
        productStock = ProductStock.load(sku, 100, 20, LocalDateTime.now());
        
        // Mock metrics service behavior
        Timer.Sample mockSample = mock(Timer.Sample.class);
        when(metricsService.startQueryOperation()).thenReturn(mockSample);
    }

    @Test
    @DisplayName("Should return StockLevelResponse for existing SKU")
    void getStockLevel_ExistingSku_ReturnsStockLevelResponse() {
        // Arrange
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.of(productStock));

        // Act
        StockLevelResponse response = inventoryQueryService.getStockLevel(sku);

        // Assert
        assertNotNull(response);
        assertEquals(sku, response.getSku());
        assertEquals(100, response.getQuantityOnHand());
        assertEquals(20, response.getQuantityAllocated());
        assertEquals(80, response.getAvailableToPromise());
        verify(productStockRepository, times(1)).findBySku(sku);
    }

    @Test
    @DisplayName("Should throw ProductStockNotFoundException when SKU not found for getStockLevel")
    void getStockLevel_SkuNotFound_ThrowsException() {
        // Arrange
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.empty());

        // Act & Assert
        ProductStockNotFoundException exception = assertThrows(ProductStockNotFoundException.class, () ->
                inventoryQueryService.getStockLevel(sku));
        assertEquals("ProductStock not found for SKU: " + sku, exception.getMessage());
        assertEquals(sku, exception.getSku());
        verify(productStockRepository, times(1)).findBySku(sku);
    }

    @Test
    @DisplayName("Should return inventory health metrics with no products")
    void getInventoryHealthMetrics_NoProducts_ReturnsEmptyMetrics() {
        // Arrange
        when(productStockRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        InventoryHealthMetricsResponse response = inventoryQueryService.getInventoryHealthMetrics(null, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalSkus());
        assertEquals(0, response.getOutOfStockSkus());
        assertEquals(0.0, response.getInventoryTurnover());
        assertTrue(response.getDeadStockSkus().isEmpty());
        verify(productStockRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return inventory health metrics with various product stocks")
    void getInventoryHealthMetrics_WithProducts_ReturnsCorrectMetrics() {
        // Arrange
        ProductStock ps1 = ProductStock.load("SKU001", 100, 20, LocalDateTime.now());
        ProductStock ps2 = ProductStock.load("SKU002", 0, 0, LocalDateTime.now()); // Out of stock
        ProductStock ps3 = ProductStock.load("SKU003", 50, 40, LocalDateTime.now()); // Mostly allocated
        ProductStock ps4 = ProductStock.load("SKU004", 10, 0, LocalDateTime.now().minusMonths(7)); // Dead stock
        ProductStock ps5 = ProductStock.load("SKU005", 20, 5, LocalDateTime.now().minusMonths(1)); // Not dead stock

        List<ProductStock> productStocks = Arrays.asList(ps1, ps2, ps3, ps4, ps5);
        when(productStockRepository.findAll()).thenReturn(productStocks);
        when(productStockRepository.findAllSkus()).thenReturn(Arrays.asList("SKU001", "SKU002", "SKU003", "SKU004", "SKU005"));
        
        // Mock ledger repository calls for turnover calculation
        when(inventoryLedgerRepository.findTotalQuantityPickedBySkuAndDateRange(eq("SKU001"), any(), any())).thenReturn(50);
        when(inventoryLedgerRepository.findTotalQuantityPickedBySkuAndDateRange(eq("SKU002"), any(), any())).thenReturn(0);
        when(inventoryLedgerRepository.findTotalQuantityPickedBySkuAndDateRange(eq("SKU003"), any(), any())).thenReturn(30);
        when(inventoryLedgerRepository.findTotalQuantityPickedBySkuAndDateRange(eq("SKU004"), any(), any())).thenReturn(0); // Dead stock
        when(inventoryLedgerRepository.findTotalQuantityPickedBySkuAndDateRange(eq("SKU005"), any(), any())).thenReturn(20);

        // Act
        InventoryHealthMetricsResponse response = inventoryQueryService.getInventoryHealthMetrics(null, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getTotalSkus());
        assertEquals(1, response.getOutOfStockSkus()); // SKU002
        assertEquals(100.0/180.0, response.getInventoryTurnover(), 0.01); // (50+0+30+0+20)/(100+0+50+10+20)
        assertEquals(2, response.getDeadStockSkus().size()); // SKU002 and SKU004 have 0 picked
        assertTrue(response.getDeadStockSkus().contains("SKU002"));
        assertTrue(response.getDeadStockSkus().contains("SKU004"));
        verify(productStockRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle inventory turnover calculation when total SKUs is zero")
    void getInventoryHealthMetrics_ZeroTotalSkus_InventoryTurnoverIsZero() {
        // Arrange
        when(productStockRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        InventoryHealthMetricsResponse response = inventoryQueryService.getInventoryHealthMetrics(null, null, null);

        // Assert
        assertEquals(0.0, response.getInventoryTurnover());
    }
}