package com.paklog.inventory.infrastructure.messaging.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.application.dto.InventoryAllocationRequestedData;
import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.infrastructure.metrics.InventoryMetricsService;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAllocationRequestedHandlerTest {

    @Mock
    private InventoryCommandService commandService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private InventoryMetricsService metricsService;

    @InjectMocks
    private InventoryAllocationRequestedHandler handler;

    private CloudEvent testEvent;
    private InventoryAllocationRequestedData testData;

    @BeforeEach
    void setUp() throws Exception {
        testData = new InventoryAllocationRequestedData();
        testData.setSku("TEST-SKU-001");
        testData.setQuantity(10);
        testData.setOrderId("ORDER-123");

        testEvent = CloudEventBuilder.v1()
                .withId("test-event-id")
                .withSource(URI.create("/test-source"))
                .withType("com.example.fulfillment.warehouse.inventory.allocation.requested")
                .withTime(OffsetDateTime.now())
                .withData("application/json", "{\"sku\":\"TEST-SKU-001\",\"quantity\":10,\"order_id\":\"ORDER-123\"}".getBytes())
                .build();

        // Mock metrics service
        Timer.Sample mockSample = mock(Timer.Sample.class);
        lenient().when(metricsService.startEventProcessing()).thenReturn(mockSample);
    }

    @Test
    @DisplayName("Should return true for supported event type")
    void canHandle_SupportedEventType_ReturnsTrue() {
        // Arrange
        String eventType = "com.example.fulfillment.warehouse.inventory.allocation.requested";

        // Act
        boolean result = handler.canHandle(eventType);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for unsupported event type")
    void canHandle_UnsupportedEventType_ReturnsFalse() {
        // Arrange
        String eventType = "com.example.fulfillment.warehouse.some.other.event";

        // Act
        boolean result = handler.canHandle(eventType);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle event successfully and call command service")
    void handle_ValidEvent_CallsCommandService() throws Exception {
        // Arrange
        when(objectMapper.readValue(any(byte[].class), eq(InventoryAllocationRequestedData.class)))
                .thenReturn(testData);

        // Act
        handler.handle(testEvent);

        // Assert
        verify(commandService).allocateStock("TEST-SKU-001", 10, "ORDER-123");
        verify(metricsService).startEventProcessing();
        verify(metricsService).incrementEventProcessed("com.example.fulfillment.warehouse.inventory.allocation.requested");
        verify(metricsService).stopEventProcessing(any(Timer.Sample.class), eq("com.example.fulfillment.warehouse.inventory.allocation.requested"));
    }

    @Test
    @DisplayName("Should handle event with no data gracefully")
    void handle_EventWithNoData_HandlesGracefully() throws Exception {
        // Arrange
        CloudEvent eventWithoutData = CloudEventBuilder.v1()
                .withId("test-event-id")
                .withSource(URI.create("/test-source"))
                .withType("com.example.fulfillment.warehouse.inventory.allocation.requested")
                .withTime(OffsetDateTime.now())
                .build();

        // Act
        handler.handle(eventWithoutData);

        // Assert
        verify(commandService, never()).allocateStock(anyString(), anyInt(), anyString());
        verify(metricsService).incrementEventError("com.example.fulfillment.warehouse.inventory.allocation.requested", "no_data");
    }

    @Test
    @DisplayName("Should handle JSON parsing error and record metrics")
    void handle_JsonParsingError_HandlesErrorAndRecordsMetrics() throws Exception {
        // Arrange
        when(objectMapper.readValue(any(byte[].class), eq(InventoryAllocationRequestedData.class)))
                .thenThrow(new RuntimeException("JSON parsing failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> handler.handle(testEvent));

        verify(metricsService).incrementEventError("com.example.fulfillment.warehouse.inventory.allocation.requested", "RuntimeException");
        verify(metricsService).stopEventProcessing(any(Timer.Sample.class), eq("com.example.fulfillment.warehouse.inventory.allocation.requested_error"));
        verify(commandService, never()).allocateStock(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Should handle command service error and record metrics")
    void handle_CommandServiceError_HandlesErrorAndRecordsMetrics() throws Exception {
        // Arrange
        when(objectMapper.readValue(any(byte[].class), eq(InventoryAllocationRequestedData.class)))
                .thenReturn(testData);
        doThrow(new RuntimeException("Allocation failed")).when(commandService)
                .allocateStock("TEST-SKU-001", 10, "ORDER-123");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> handler.handle(testEvent));

        verify(metricsService).incrementEventError("com.example.fulfillment.warehouse.inventory.allocation.requested", "RuntimeException");
        verify(metricsService).stopEventProcessing(any(Timer.Sample.class), eq("com.example.fulfillment.warehouse.inventory.allocation.requested_error"));
    }

    @Test
    @DisplayName("Should handle different data content correctly")
    void handle_DifferentDataContent_HandlesCorrectly() throws Exception {
        // Arrange
        InventoryAllocationRequestedData differentData = new InventoryAllocationRequestedData();
        differentData.setSku("DIFFERENT-SKU");
        differentData.setQuantity(25);
        differentData.setOrderId("ORDER-XYZ");

        when(objectMapper.readValue(any(byte[].class), eq(InventoryAllocationRequestedData.class)))
                .thenReturn(differentData);

        // Act
        handler.handle(testEvent);

        // Assert
        verify(commandService).allocateStock("DIFFERENT-SKU", 25, "ORDER-XYZ");
        verify(metricsService).incrementEventProcessed("com.example.fulfillment.warehouse.inventory.allocation.requested");
    }
}