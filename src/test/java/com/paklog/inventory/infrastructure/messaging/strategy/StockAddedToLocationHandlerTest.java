package com.paklog.inventory.infrastructure.messaging.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.domain.event.StockAddedToLocation;
import com.paklog.inventory.domain.model.Location;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockAddedToLocationHandlerTest {

    @Mock
    private InventoryCommandService inventoryCommandService;

    private ObjectMapper objectMapper;
    private StockAddedToLocationHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new StockAddedToLocationHandler(inventoryCommandService, objectMapper);
    }

    @Test
    void canHandle_correctEventType_returnsTrue() {
        assertTrue(handler.canHandle("paklog.inventory.stock-added-to-location.v1"));
    }

    @Test
    void canHandle_incorrectEventType_returnsFalse() {
        assertFalse(handler.canHandle("some.other.event.type"));
    }

    @Test
    void handle_callsIncreaseQuantityOnHand() throws Exception {
        String sku = "TEST-SKU-001";
        Location location = new Location("A1", "S1", "B1");
        int quantity = 50;
        StockAddedToLocation eventData = new StockAddedToLocation(sku, location, quantity);

        CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId("123")
                .withSource(URI.create("/test"))
                .withType("paklog.inventory.stock-added-to-location.v1")
                .withTime(OffsetDateTime.now())
                .withData(PojoCloudEventData.wrap(objectMapper.writeValueAsBytes(eventData.getEventData()), bytes -> bytes))
                .build();

        handler.handle(cloudEvent);

        verify(inventoryCommandService, times(1)).increaseQuantityOnHand(sku, quantity);
    }
}
