package com.paklog.inventory.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.domain.event.StockLevelChangedEvent;
import com.paklog.inventory.domain.model.StockLevel;
import com.paklog.inventory.infrastructure.observability.InventoryMetrics;
import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration test for the complete CloudEvent publishing flow.
 * Tests the interaction between CloudEventPublisher, CloudEventFactory,
 * and CloudEventSchemaValidator.
 */
class CloudEventPublisherIntegrationTest {

    private CloudEventPublisher publisher;
    private KafkaTemplate<String, CloudEvent> kafkaTemplate;
    private CloudEventFactory cloudEventFactory;
    private CloudEventSchemaValidator schemaValidator;
    private InventoryMetrics metrics;
    private ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        kafkaTemplate = mock(KafkaTemplate.class);
        cloudEventFactory = new CloudEventFactory(objectMapper);
        schemaValidator = new CloudEventSchemaValidator(objectMapper);
        metrics = new InventoryMetrics(new SimpleMeterRegistry());

        publisher = new CloudEventPublisher(kafkaTemplate, cloudEventFactory, schemaValidator, metrics);
    }

    @Test
    @DisplayName("Should successfully publish valid domain event as CloudEvent")
    void shouldPublishValidDomainEventAsCloudEvent() {
        // Given: A valid domain event
        StockLevel previousLevel = StockLevel.of(100, 20);
        StockLevel newLevel = StockLevel.of(150, 30);
        StockLevelChangedEvent domainEvent = new StockLevelChangedEvent(
                "SKU-12345",
                previousLevel,
                newLevel,
                "RECEIPT"
        );

        // When: Publishing the event
        publisher.publish(domainEvent);

        // Then: CloudEvent should be sent to Kafka with correct attributes
        ArgumentCaptor<CloudEvent> cloudEventCaptor = ArgumentCaptor.forClass(CloudEvent.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate, times(1)).send(
                eq("fulfillment.inventory.v1.events"),
                keyCaptor.capture(),
                cloudEventCaptor.capture()
        );

        CloudEvent capturedEvent = cloudEventCaptor.getValue();
        String capturedKey = keyCaptor.getValue();

        // Verify CloudEvent structure
        assertNotNull(capturedEvent);
        assertEquals("1.0", capturedEvent.getSpecVersion().toString());
        assertEquals("com.paklog.inventory.fulfillment.v1.product-stock.level-changed", capturedEvent.getType());
        assertEquals("/fulfillment/inventory-service", capturedEvent.getSource().toString());
        assertEquals("SKU-12345", capturedEvent.getSubject());
        assertEquals("SKU-12345", capturedKey);
        assertEquals("application/json", capturedEvent.getDataContentType());
        assertNotNull(capturedEvent.getId());
        assertNotNull(capturedEvent.getTime());
        assertNotNull(capturedEvent.getData());
    }

    @Test
    @DisplayName("Should handle schema validation when schema is not available")
    void shouldHandleSchemaValidationWhenSchemaNotAvailable() {
        // Given: A domain event (schema may not be loaded in test environment)
        StockLevel previousLevel = StockLevel.of(100, 20);
        StockLevel newLevel = StockLevel.of(150, 30);
        StockLevelChangedEvent domainEvent = new StockLevelChangedEvent(
                "SKU-12345",
                previousLevel,
                newLevel,
                "RECEIPT"
        );

        // When/Then: Publishing should succeed even without schema
        assertDoesNotThrow(() -> publisher.publish(domainEvent));

        // Verify Kafka template was called
        verify(kafkaTemplate, times(1)).send(
                eq("fulfillment.inventory.v1.events"),
                any(String.class),
                any(CloudEvent.class)
        );
    }

    @Test
    @DisplayName("Should use event ID from domain event as CloudEvent ID")
    void shouldUseEventIdFromDomainEvent() {
        // Given: A domain event with known ID
        StockLevel previousLevel = StockLevel.of(100, 20);
        StockLevel newLevel = StockLevel.of(150, 30);
        StockLevelChangedEvent domainEvent = new StockLevelChangedEvent(
                "SKU-12345",
                previousLevel,
                newLevel,
                "RECEIPT"
        );

        // When: Publishing the event
        publisher.publish(domainEvent);

        // Then: CloudEvent ID should match domain event ID
        ArgumentCaptor<CloudEvent> cloudEventCaptor = ArgumentCaptor.forClass(CloudEvent.class);
        verify(kafkaTemplate).send(any(), any(), cloudEventCaptor.capture());

        CloudEvent capturedEvent = cloudEventCaptor.getValue();
        assertEquals(domainEvent.getEventId(), capturedEvent.getId());
    }

    @Test
    @DisplayName("Should use SKU as Kafka partition key")
    void shouldUseSkuAsKafkaPartitionKey() {
        // Given: A domain event with specific SKU
        String expectedSku = "SKU-SPECIAL-001";
        StockLevel previousLevel = StockLevel.of(100, 20);
        StockLevel newLevel = StockLevel.of(150, 30);
        StockLevelChangedEvent domainEvent = new StockLevelChangedEvent(
                expectedSku,
                previousLevel,
                newLevel,
                "RECEIPT"
        );

        // When: Publishing the event
        publisher.publish(domainEvent);

        // Then: Kafka key should be the SKU
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(any(), keyCaptor.capture(), any(CloudEvent.class));

        assertEquals(expectedSku, keyCaptor.getValue());
    }
}
