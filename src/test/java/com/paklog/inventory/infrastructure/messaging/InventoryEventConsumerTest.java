package com.paklog.inventory.infrastructure.messaging;

import com.paklog.inventory.infrastructure.messaging.strategy.EventHandler;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTest {

    @Mock
    private EventHandler eventHandler1;
    
    @Mock
    private EventHandler eventHandler2;
    
    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private InventoryEventConsumer consumer;

    private CloudEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = CloudEventBuilder.v1()
                .withId("test-event-id")
                .withSource(URI.create("/test-source"))
                .withType("com.example.fulfillment.warehouse.inventory.allocation.requested")
                .withTime(OffsetDateTime.now())
                .withData("application/json", "{\"sku\":\"TEST-SKU\",\"quantity\":10}".getBytes())
                .build();

        // Set up the handlers list in the consumer
        List<EventHandler> handlers = Arrays.asList(eventHandler1, eventHandler2);
        consumer = new InventoryEventConsumer(handlers);
    }

    @Test
    @DisplayName("Should process event with matching handler")
    void listenForWarehouseEvents_WithMatchingHandler_ProcessesEvent() throws Exception {
        // Arrange
        when(eventHandler1.canHandle("com.example.fulfillment.warehouse.inventory.allocation.requested"))
                .thenReturn(true);
        when(eventHandler2.canHandle("com.example.fulfillment.warehouse.inventory.allocation.requested"))
                .thenReturn(false);

        // Act
        consumer.listenForWarehouseEvents(testEvent, acknowledgment);

        // Assert
        verify(eventHandler1).canHandle("com.example.fulfillment.warehouse.inventory.allocation.requested");
        verify(eventHandler1).handle(testEvent);
        verify(eventHandler2).canHandle("com.example.fulfillment.warehouse.inventory.allocation.requested");
        verify(eventHandler2, never()).handle(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should handle event when no matching handler found")
    void listenForWarehouseEvents_WithNoMatchingHandler_AcknowledgesAnyway() throws Exception {
        // Arrange
        when(eventHandler1.canHandle(anyString())).thenReturn(false);
        when(eventHandler2.canHandle(anyString())).thenReturn(false);

        // Act
        consumer.listenForWarehouseEvents(testEvent, acknowledgment);

        // Assert
        verify(eventHandler1).canHandle("com.example.fulfillment.warehouse.inventory.allocation.requested");
        verify(eventHandler2).canHandle("com.example.fulfillment.warehouse.inventory.allocation.requested");
        verify(eventHandler1, never()).handle(any());
        verify(eventHandler2, never()).handle(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should use first matching handler when multiple handlers match")
    void listenForWarehouseEvents_WithMultipleMatchingHandlers_UsesFirstMatch() throws Exception {
        // Arrange
        when(eventHandler1.canHandle("com.example.fulfillment.warehouse.inventory.allocation.requested"))
                .thenReturn(true);
        lenient().when(eventHandler2.canHandle("com.example.fulfillment.warehouse.inventory.allocation.requested"))
                .thenReturn(true);

        // Act
        consumer.listenForWarehouseEvents(testEvent, acknowledgment);

        // Assert
        verify(eventHandler1).handle(testEvent);
        verify(eventHandler2, never()).handle(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should not acknowledge when handler throws exception")
    void listenForWarehouseEvents_WithHandlerException_DoesNotAcknowledge() throws Exception {
        // Arrange
        when(eventHandler1.canHandle("com.example.fulfillment.warehouse.inventory.allocation.requested"))
                .thenReturn(true);
        doThrow(new RuntimeException("Handler failed")).when(eventHandler1).handle(testEvent);

        // Act
        consumer.listenForWarehouseEvents(testEvent, acknowledgment);

        // Assert
        verify(eventHandler1).handle(testEvent);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("Should handle empty handler list gracefully")
    void listenForWarehouseEvents_WithEmptyHandlerList_HandlesGracefully() {
        // Arrange
        InventoryEventConsumer emptyConsumer = new InventoryEventConsumer(Collections.emptyList());

        // Act
        emptyConsumer.listenForWarehouseEvents(testEvent, acknowledgment);

        // Assert
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should handle different event types correctly")
    void listenForWarehouseEvents_WithDifferentEventTypes_HandlesCorrectly() throws Exception {
        // Arrange
        CloudEvent itemPickedEvent = CloudEventBuilder.v1()
                .withId("picked-event-id")
                .withSource(URI.create("/test-source"))
                .withType("com.example.fulfillment.warehouse.item.picked")
                .withTime(OffsetDateTime.now())
                .withData("application/json", "{\"sku\":\"TEST-SKU\",\"quantity_picked\":5}".getBytes())
                .build();

        when(eventHandler1.canHandle("com.example.fulfillment.warehouse.item.picked"))
                .thenReturn(false);
        when(eventHandler2.canHandle("com.example.fulfillment.warehouse.item.picked"))
                .thenReturn(true);

        // Act
        consumer.listenForWarehouseEvents(itemPickedEvent, acknowledgment);

        // Assert
        verify(eventHandler1).canHandle("com.example.fulfillment.warehouse.item.picked");
        verify(eventHandler2).canHandle("com.example.fulfillment.warehouse.item.picked");
        verify(eventHandler1, never()).handle(any());
        verify(eventHandler2).handle(itemPickedEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should not acknowledge when canHandle throws exception")
    void listenForWarehouseEvents_WithCanHandleException_DoesNotAcknowledge() {
        // Arrange
        when(eventHandler1.canHandle("com.example.fulfillment.warehouse.inventory.allocation.requested"))
                .thenThrow(new RuntimeException("CanHandle failed"));

        // Act
        consumer.listenForWarehouseEvents(testEvent, acknowledgment);

        // Assert
        verify(eventHandler1).canHandle("com.example.fulfillment.warehouse.inventory.allocation.requested");
        verify(acknowledgment, never()).acknowledge();
    }
}