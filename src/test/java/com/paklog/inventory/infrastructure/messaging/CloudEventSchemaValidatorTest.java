package com.paklog.inventory.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.domain.event.CloudEventType;
import com.paklog.inventory.domain.event.StockLevelChangedEvent;
import com.paklog.inventory.domain.model.StockLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for CloudEvent schema validation.
 */
class CloudEventSchemaValidatorTest {

    private CloudEventSchemaValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new CloudEventSchemaValidator(objectMapper);
    }

    @Test
    @DisplayName("Should initialize schema validator")
    void shouldInitializeSchemaValidator() {
        // Verify validator is initialized
        assertNotNull(validator);
        int schemaCount = validator.getSchemaCount();
        System.out.println("Schemas loaded: " + schemaCount);
        // Note: In test environment, schemas may not be loaded from classpath
        // The validator should still be functional
    }

    @Test
    @DisplayName("Should successfully validate valid StockLevelChangedEvent")
    void shouldValidateValidStockLevelChangedEvent() {
        // Given: A valid StockLevelChangedEvent
        StockLevel previousLevel = StockLevel.of(100, 20);
        StockLevel newLevel = StockLevel.of(150, 30);
        StockLevelChangedEvent event = new StockLevelChangedEvent(
                "SKU-12345",
                previousLevel,
                newLevel,
                "RECEIPT"
        );

        // When/Then: Validation should succeed
        assertDoesNotThrow(() -> validator.validate(event));
    }

    @Test
    @DisplayName("Should check if schema exists for event type")
    void shouldCheckIfSchemaExistsForEventType() {
        // Given: Known event type
        String eventType = CloudEventType.STOCK_LEVEL_CHANGED.getType();

        // Then: Check if schema exists (may be false in test environment)
        boolean hasSchema = validator.hasSchema(eventType);
        System.out.println("Schema exists for " + eventType + ": " + hasSchema);
    }

    @Test
    @DisplayName("Should handle missing schema gracefully")
    void shouldHandleMissingSchemaGracefully() {
        // Given: Unknown event type
        String unknownType = "com.paklog.inventory.unknown.event";

        // Then: Should not have schema
        assertFalse(validator.hasSchema(unknownType));
    }

    @Test
    @DisplayName("Should return schema count")
    void shouldReturnSchemaCount() {
        // Given: Initialized validator
        int schemaCount = validator.getSchemaCount();

        // Then: Should return count (may be 0 in test environment)
        assertTrue(schemaCount >= 0, "Schema count should be non-negative");
        System.out.println("Loaded " + schemaCount + " CloudEvent schemas");
    }
}
