package com.paklog.inventory.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.application.dto.StockLevelResponse;
import com.paklog.inventory.application.dto.UpdateStockLevelRequest;
import com.paklog.inventory.domain.model.ProductStock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test for snake_case JSON serialization/deserialization.
 */
@SpringBootTest
class JacksonConfigurationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void objectMapper_shouldSerializeToSnakeCase() throws Exception {
        // Given: A StockLevelResponse with camelCase field names
        ProductStock productStock = ProductStock.create("SKU-TEST-001", 100);
        productStock.allocate(20);
        StockLevelResponse response = StockLevelResponse.fromDomain(productStock);

        // When: Serializing to JSON
        String json = objectMapper.writeValueAsString(response);

        // Then: JSON should use snake_case
        assertTrue(json.contains("\"quantity_on_hand\""), "Should contain quantity_on_hand");
        assertTrue(json.contains("\"quantity_allocated\""), "Should contain quantity_allocated");
        assertTrue(json.contains("\"available_to_promise\""), "Should contain available_to_promise");
        assertTrue(json.contains("\"location_id\""), "Should contain location_id");

        // Should NOT contain camelCase
        assertFalse(json.contains("\"quantityOnHand\""), "Should NOT contain quantityOnHand");
        assertFalse(json.contains("\"quantityAllocated\""), "Should NOT contain quantityAllocated");
        assertFalse(json.contains("\"availableToPromise\""), "Should NOT contain availableToPromise");
        assertFalse(json.contains("\"locationId\""), "Should NOT contain locationId");

        System.out.println("✅ Serialized JSON: " + json);
    }

    @Test
    void objectMapper_shouldDeserializeFromSnakeCase() throws Exception {
        // Given: JSON with snake_case field names
        String json = """
                {
                  "quantity_change": 50,
                  "reason_code": "PURCHASE_RECEIPT",
                  "comment": "Test adjustment"
                }
                """;

        // When: Deserializing from JSON
        UpdateStockLevelRequest request = objectMapper.readValue(json, UpdateStockLevelRequest.class);

        // Then: Fields should be populated correctly
        assertEquals(50, request.quantityChange(), "quantity_change should map to quantityChange");
        assertEquals("PURCHASE_RECEIPT", request.reasonCode(), "reason_code should map to reasonCode");
        assertEquals("Test adjustment", request.comment(), "comment should map to comment");

        System.out.println("✅ Deserialized request: " + request);
    }

    @Test
    void objectMapper_shouldRoundTripCorrectly() throws Exception {
        // Given: Original object
        ProductStock productStock = ProductStock.create("SKU-ROUNDTRIP", 200);
        productStock.allocate(50);
        StockLevelResponse original = StockLevelResponse.fromDomain(productStock);

        // When: Serialize then deserialize
        String json = objectMapper.writeValueAsString(original);
        StockLevelResponse deserialized = objectMapper.readValue(json, StockLevelResponse.class);

        // Then: Should maintain data integrity
        assertEquals(original.getSku(), deserialized.getSku());
        assertEquals(original.getQuantityOnHand(), deserialized.getQuantityOnHand());
        assertEquals(original.getQuantityAllocated(), deserialized.getQuantityAllocated());
        assertEquals(original.getAvailableToPromise(), deserialized.getAvailableToPromise());

        System.out.println("✅ Round-trip successful: " + json);
    }
}
