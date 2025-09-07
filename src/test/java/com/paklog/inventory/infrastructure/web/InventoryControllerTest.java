package com.paklog.inventory.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.application.dto.CreateAdjustmentRequest;
import com.paklog.inventory.application.dto.InventoryHealthMetricsResponse;
import com.paklog.inventory.application.dto.StockLevelResponse;
import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.application.service.InventoryQueryService;
import com.paklog.inventory.domain.exception.InsufficientStockException;
import com.paklog.inventory.domain.exception.InvalidQuantityException;
import com.paklog.inventory.domain.exception.ProductStockNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryCommandService commandService;

    @MockBean
    private InventoryQueryService queryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return stock level for existing SKU")
    void getStockLevel_ExistingSku_ReturnsStockLevel() throws Exception {
        // Arrange
        String sku = "TEST-SKU-001";
        // Create a mock ProductStock to use with fromDomain method
        com.paklog.inventory.domain.model.ProductStock mockProductStock = 
                com.paklog.inventory.domain.model.ProductStock.load(sku, 100, 20, java.time.LocalDateTime.now());
        StockLevelResponse response = StockLevelResponse.fromDomain(mockProductStock);

        when(queryService.getStockLevel(sku)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/inventory/stock_levels/{sku}", sku))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.quantityOnHand").value(100))
                .andExpect(jsonPath("$.quantityAllocated").value(20))
                .andExpect(jsonPath("$.availableToPromise").value(80));

        verify(queryService).getStockLevel(sku);
    }

    @Test
    @DisplayName("Should return 404 when SKU not found")
    void getStockLevel_SkuNotFound_Returns404() throws Exception {
        // Arrange
        String sku = "NONEXISTENT-SKU";
        when(queryService.getStockLevel(sku)).thenThrow(new ProductStockNotFoundException(sku));

        // Act & Assert
        mockMvc.perform(get("/inventory/stock_levels/{sku}", sku))
                .andExpect(status().isNotFound());

        verify(queryService).getStockLevel(sku);
    }

    @Test
    @DisplayName("Should create stock adjustment successfully")
    void createStockAdjustment_ValidRequest_ReturnsAccepted() throws Exception {
        // Arrange
        CreateAdjustmentRequest request = new CreateAdjustmentRequest();
        request.setSku("TEST-SKU-001");
        request.setQuantityChange(10);
        request.setReasonCode("CYCLE_COUNT");
        request.setComment("Found extra stock");

        // Act & Assert
        mockMvc.perform(post("/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(commandService).adjustStock(
                request.getSku(),
                request.getQuantityChange(),
                request.getReasonCode(),
                request.getComment(),
                "admin"
        );
    }

    @Test
    @DisplayName("Should return 404 when adjusting non-existent product")
    void createStockAdjustment_ProductNotFound_Returns404() throws Exception {
        // Arrange
        CreateAdjustmentRequest request = new CreateAdjustmentRequest();
        request.setSku("NONEXISTENT-SKU");
        request.setQuantityChange(10);
        request.setReasonCode("CYCLE_COUNT");

        doThrow(new ProductStockNotFoundException("NONEXISTENT-SKU"))
                .when(commandService).adjustStock("NONEXISTENT-SKU", 10, "CYCLE_COUNT", null, "admin");

        // Act & Assert
        mockMvc.perform(post("/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when invalid quantity")
    void createStockAdjustment_InvalidQuantity_Returns400() throws Exception {
        // Arrange
        CreateAdjustmentRequest request = new CreateAdjustmentRequest();
        request.setSku("TEST-SKU-001");
        request.setQuantityChange(-1000);
        request.setReasonCode("DAMAGE");

        doThrow(new InvalidQuantityException("adjust", -1000, "Would result in negative stock"))
                .when(commandService).adjustStock("TEST-SKU-001", -1000, "DAMAGE", null, "admin");

        // Act & Assert
        mockMvc.perform(post("/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when insufficient stock")
    void createStockAdjustment_InsufficientStock_Returns400() throws Exception {
        // Arrange
        CreateAdjustmentRequest request = new CreateAdjustmentRequest();
        request.setSku("TEST-SKU-001");
        request.setQuantityChange(10);
        request.setReasonCode("ALLOCATION");

        doThrow(new InsufficientStockException("TEST-SKU-001", 10, 5))
                .when(commandService).adjustStock("TEST-SKU-001", 10, "ALLOCATION", null, "admin");

        // Act & Assert
        mockMvc.perform(post("/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return inventory health metrics")
    void getInventoryHealthMetrics_ReturnsMetrics() throws Exception {
        // Arrange
        InventoryHealthMetricsResponse response = InventoryHealthMetricsResponse.of(
                4.5,
                Arrays.asList("DEAD-SKU-001", "DEAD-SKU-002"),
                100L,
                5L
        );

        when(queryService.getInventoryHealthMetrics(null, null, null)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/inventory/reports/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inventoryTurnover").value(4.5))
                .andExpect(jsonPath("$.deadStockSkus").isArray())
                .andExpect(jsonPath("$.deadStockSkus[0]").value("DEAD-SKU-001"))
                .andExpect(jsonPath("$.deadStockSkus[1]").value("DEAD-SKU-002"))
                .andExpect(jsonPath("$.totalSkus").value(100))
                .andExpect(jsonPath("$.outOfStockSkus").value(5));

        verify(queryService).getInventoryHealthMetrics(null, null, null);
    }

    @Test
    @DisplayName("Should return inventory health metrics with query parameters")
    void getInventoryHealthMetrics_WithQueryParameters_ReturnsMetrics() throws Exception {
        // Arrange
        String category = "Electronics";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);

        InventoryHealthMetricsResponse response = InventoryHealthMetricsResponse.of(
                6.0,
                Collections.singletonList("ELECTRONICS-DEAD-001"),
                50L,
                2L
        );

        when(queryService.getInventoryHealthMetrics(category, startDate, endDate)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/inventory/reports/health")
                        .param("category", category)
                        .param("startDate", "2023-01-01")
                        .param("endDate", "2023-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inventoryTurnover").value(6.0))
                .andExpect(jsonPath("$.deadStockSkus[0]").value("ELECTRONICS-DEAD-001"))
                .andExpect(jsonPath("$.totalSkus").value(50))
                .andExpect(jsonPath("$.outOfStockSkus").value(2));

        verify(queryService).getInventoryHealthMetrics(category, startDate, endDate);
    }

    @Test
    @DisplayName("Should handle InvalidQuantityException with proper error message")
    void handleInvalidQuantityException_ReturnsErrorMessage() throws Exception {
        // Arrange
        CreateAdjustmentRequest request = new CreateAdjustmentRequest();
        request.setSku("TEST-SKU-001");
        request.setQuantityChange(-1);
        request.setReasonCode("TEST");

        InvalidQuantityException exception = new InvalidQuantityException("adjust", -1, "Quantity must be positive");
        doThrow(exception)
                .when(commandService).adjustStock("TEST-SKU-001", -1, "TEST", null, "admin");

        // Act & Assert
        mockMvc.perform(post("/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(exception.getMessage()));
    }

    @Test
    @DisplayName("Should handle InsufficientStockException with proper error message")
    void handleInsufficientStockException_ReturnsErrorMessage() throws Exception {
        // Arrange
        CreateAdjustmentRequest request = new CreateAdjustmentRequest();
        request.setSku("TEST-SKU-001");
        request.setQuantityChange(100);
        request.setReasonCode("ALLOCATION");

        InsufficientStockException exception = new InsufficientStockException("TEST-SKU-001", 100, 50);
        doThrow(exception)
                .when(commandService).adjustStock("TEST-SKU-001", 100, "ALLOCATION", null, "admin");

        // Act & Assert
        mockMvc.perform(post("/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(exception.getMessage()));
    }

    @Test
    @DisplayName("Should handle ProductStockNotFoundException with proper error message")
    void handleProductStockNotFoundException_ReturnsErrorMessage() throws Exception {
        // Arrange
        String sku = "NONEXISTENT-SKU";
        ProductStockNotFoundException exception = new ProductStockNotFoundException(sku);
        when(queryService.getStockLevel(sku)).thenThrow(exception);

        // Act & Assert
        mockMvc.perform(get("/inventory/stock_levels/{sku}", sku))
                .andExpect(status().isNotFound())
                .andExpect(content().string(exception.getMessage()));
    }

    @Test
    @DisplayName("Should handle malformed JSON with 400 error")
    void createStockAdjustment_MalformedJson_Returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/inventory/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());

        verify(commandService, never()).adjustStock(anyString(), anyInt(), anyString(), anyString(), anyString());
    }
}