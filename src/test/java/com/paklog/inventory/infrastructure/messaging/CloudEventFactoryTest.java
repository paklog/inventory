package com.paklog.inventory.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.domain.event.CloudEventType;
import com.paklog.inventory.domain.event.StockLevelChangedEvent;
import com.paklog.inventory.domain.model.StockLevel;
import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for CloudEventFactory.
 */
class CloudEventFactoryTest {

    private CloudEventFactory factory;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        factory = new CloudEventFactory(objectMapper);
    }

    @Test
    @DisplayName("Should create valid CloudEvent from domain event")
    void shouldCreateValidCloudEventFromDomainEvent() {
        // Given: A domain event
        StockLevel previousLevel = StockLevel.of(100, 20);
        StockLevel newLevel = StockLevel.of(150, 30);
        StockLevelChangedEvent domainEvent = new StockLevelChangedEvent(
                "SKU-12345",
                previousLevel,
                newLevel,
                "RECEIPT"
        );

        // When: Creating CloudEvent
        CloudEvent cloudEvent = factory.create(domainEvent);

        // Then: CloudEvent should be properly formatted
        assertNotNull(cloudEvent);
        assertEquals("1.0", cloudEvent.getSpecVersion().toString());
        assertEquals(domainEvent.getEventId(), cloudEvent.getId());
        assertEquals(CloudEventType.STOCK_LEVEL_CHANGED.getType(), cloudEvent.getType());
        assertEquals("SKU-12345", cloudEvent.getSubject());
        assertEquals("/fulfillment/inventory-service", cloudEvent.getSource().toString());
        assertNotNull(cloudEvent.getTime());
        assertNotNull(cloudEvent.getData());
    }

    @Test
    @DisplayName("Should set correct data content type")
    void shouldSetCorrectDataContentType() {
        // Given: A domain event
        StockLevel previousLevel = StockLevel.of(100, 20);
        StockLevel newLevel = StockLevel.of(150, 30);
        StockLevelChangedEvent domainEvent = new StockLevelChangedEvent(
                "SKU-12345",
                previousLevel,
                newLevel,
                "RECEIPT"
        );

        // When: Creating CloudEvent
        CloudEvent cloudEvent = factory.create(domainEvent);

        // Then: Content type should be application/json
        assertEquals("application/json", cloudEvent.getDataContentType());
    }

    @Test
    @DisplayName("Should use event type from CloudEventType enum")
    void shouldUseEventTypeFromEnum() {
        // Given: A domain event
        StockLevel previousLevel = StockLevel.of(100, 20);
        StockLevel newLevel = StockLevel.of(150, 30);
        StockLevelChangedEvent domainEvent = new StockLevelChangedEvent(
                "SKU-12345",
                previousLevel,
                newLevel,
                "RECEIPT"
        );

        // When: Creating CloudEvent
        CloudEvent cloudEvent = factory.create(domainEvent);

        // Then: Type should match the standard naming convention
        assertTrue(cloudEvent.getType().startsWith("com.paklog.inventory.fulfillment.v1."));
        assertEquals(CloudEventType.STOCK_LEVEL_CHANGED.getType(), cloudEvent.getType());
    }
}
