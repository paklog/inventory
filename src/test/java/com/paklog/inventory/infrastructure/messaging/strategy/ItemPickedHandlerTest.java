package com.paklog.inventory.infrastructure.messaging.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.application.dto.ItemPickedData;
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
class ItemPickedHandlerTest {

    @Mock
    private InventoryCommandService commandService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private InventoryMetricsService metricsService;

    @InjectMocks
    private ItemPickedHandler handler;

    private CloudEvent testEvent;
    private ItemPickedData testData;

    @BeforeEach
    void setUp() throws Exception {
        testData = new ItemPickedData();
        testData.setSku("TEST-SKU-002");
        testData.setQuantityPicked(5);
        testData.setOrderId("ORDER-456");

        testEvent = CloudEventBuilder.v1()
                .withId("test-event-id-2")
                .withSource(URI.create("/test-source"))
                .withType("com.example.fulfillment.warehouse.item.picked")
                .withTime(OffsetDateTime.now())
                .withData("application/json", "{\"sku\":\"TEST-SKU-002\",\"quantity_picked\":5,\"order_id\":\"ORDER-456\"}".getBytes())
                .build();

        // Mock metrics service
        Timer.Sample mockSample = mock(Timer.Sample.class);
        when(metricsService.startEventProcessing()).thenReturn(mockSample);
    }

    @Test
    @DisplayName("Should return true for supported event type")
    void canHandle_SupportedEventType_ReturnsTrue() {
        // Arrange
        String eventType = "com.example.fulfillment.warehouse.item.picked";

        // Act
        boolean result = handler.canHandle(eventType);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for unsupported event type")
    void canHandle_UnsupportedEventType_ReturnsFalse() {
        // Arrange
        String eventType = "com.example.fulfillment.warehouse.inventory.allocation.requested";

        // Act
        boolean result = handler.canHandle(eventType);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle event successfully and call command service")
    void handle_ValidEvent_CallsCommandService() throws Exception {
        // Arrange
        when(objectMapper.readValue(any(byte[].class), eq(ItemPickedData.class)))
                .thenReturn(testData);

        // Act
        handler.handle(testEvent);

        // Assert
        verify(commandService).processItemPicked("TEST-SKU-002", 5, "ORDER-456");
        verify(metricsService).startEventProcessing();
        verify(metricsService).incrementEventProcessed("com.example.fulfillment.warehouse.item.picked");
        verify(metricsService).stopEventProcessing(any(Timer.Sample.class), eq("com.example.fulfillment.warehouse.item.picked"));
    }

    @Test
    @DisplayName("Should handle event with no data gracefully")
    void handle_EventWithNoData_HandlesGracefully() throws Exception {
        // Arrange
        CloudEvent eventWithoutData = CloudEventBuilder.v1()
                .withId("test-event-id-2")
                .withSource(URI.create("/test-source"))
                .withType("com.example.fulfillment.warehouse.item.picked")
                .withTime(OffsetDateTime.now())
                .build();

        // Act
        handler.handle(eventWithoutData);

        // Assert
        verify(commandService, never()).processItemPicked(anyString(), anyInt(), anyString());
        verify(metricsService).incrementEventError("com.example.fulfillment.warehouse.item.picked", "no_data");
    }

    @Test
    @DisplayName("Should handle JSON parsing error and record metrics")
    void handle_JsonParsingError_HandlesErrorAndRecordsMetrics() throws Exception {
        // Arrange
        when(objectMapper.readValue(any(byte[].class), eq(ItemPickedData.class)))
                .thenThrow(new RuntimeException("JSON parsing failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> handler.handle(testEvent));

        verify(metricsService).incrementEventError("com.example.fulfillment.warehouse.item.picked", "RuntimeException");
        verify(metricsService).stopEventProcessing(any(Timer.Sample.class), eq("com.example.fulfillment.warehouse.item.picked_error"));
        verify(commandService, never()).processItemPicked(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Should handle command service error and record metrics")
    void handle_CommandServiceError_HandlesErrorAndRecordsMetrics() throws Exception {
        // Arrange
        when(objectMapper.readValue(any(byte[].class), eq(ItemPickedData.class)))
                .thenReturn(testData);
        doThrow(new RuntimeException("Processing failed")).when(commandService)
                .processItemPicked("TEST-SKU-002", 5, "ORDER-456");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> handler.handle(testEvent));

        verify(metricsService).incrementEventError("com.example.fulfillment.warehouse.item.picked", "RuntimeException");
        verify(metricsService).stopEventProcessing(any(Timer.Sample.class), eq("com.example.fulfillment.warehouse.item.picked_error"));
    }

    @Test
    @DisplayName("Should handle different data content correctly")
    void handle_DifferentDataContent_HandlesCorrectly() throws Exception {
        // Arrange
        ItemPickedData differentData = new ItemPickedData();
        differentData.setSku("ANOTHER-SKU");
        differentData.setQuantityPicked(15);
        differentData.setOrderId("ORDER-999");

        when(objectMapper.readValue(any(byte[].class), eq(ItemPickedData.class)))
                .thenReturn(differentData);

        // Act
        handler.handle(testEvent);

        // Assert
        verify(commandService).processItemPicked("ANOTHER-SKU", 15, "ORDER-999");
        verify(metricsService).incrementEventProcessed("com.example.fulfillment.warehouse.item.picked");
    }

    @Test
    @DisplayName("Should handle zero quantity picked correctly")
    void handle_ZeroQuantityPicked_HandlesCorrectly() throws Exception {
        // Arrange
        ItemPickedData zeroQuantityData = new ItemPickedData();
        zeroQuantityData.setSku("ZERO-PICK-SKU");
        zeroQuantityData.setQuantityPicked(0);
        zeroQuantityData.setOrderId("ORDER-ZERO");

        when(objectMapper.readValue(any(byte[].class), eq(ItemPickedData.class)))
                .thenReturn(zeroQuantityData);

        // Act
        handler.handle(testEvent);

        // Assert
        verify(commandService).processItemPicked("ZERO-PICK-SKU", 0, "ORDER-ZERO");
        verify(metricsService).incrementEventProcessed("com.example.fulfillment.warehouse.item.picked");
    }
}