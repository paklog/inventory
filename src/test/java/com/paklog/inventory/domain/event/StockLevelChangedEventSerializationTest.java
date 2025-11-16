package com.paklog.inventory.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.domain.model.StockLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test for StockLevelChangedEvent CloudEvent payload structure.
 * Ensures events match AsyncAPI spec with nested structure and snake_case.
 */
@SpringBootTest
class StockLevelChangedEventSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getEventData_shouldReturnNestedSnakeCaseStructure() throws Exception {
        // Given: A StockLevelChangedEvent
        StockLevel previousLevel = StockLevel.of(100, 20);
        StockLevel newLevel = StockLevel.of(150, 30);

        StockLevelChangedEvent event = new StockLevelChangedEvent(
                "SKU-TEST-001",
                previousLevel,
                newLevel,
                "PURCHASE_RECEIPT"
        );

        // When: Getting event data
        Map<String, Object> eventData = event.getEventData();

        // Then: Should have correct structure (with snake_case keys)
        assertEquals("SKU-TEST-001", eventData.get("sku"));
        assertNotNull(eventData.get("previous_stock_level"));
        assertNotNull(eventData.get("new_stock_level"));
        assertEquals("PURCHASE_RECEIPT", eventData.get("change_reason"));

        // When: Serializing to JSON
        String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(eventData);

        System.out.println("📦 CloudEvent Data Payload:");
        System.out.println(json);

        // Then: Should use snake_case field names
        assertTrue(json.contains("\"previous_stock_level\""),
                "Should contain previous_stock_level");
        assertTrue(json.contains("\"new_stock_level\""),
                "Should contain new_stock_level");
        assertTrue(json.contains("\"change_reason\""),
                "Should contain change_reason");
        assertTrue(json.contains("\"quantity_on_hand\""),
                "Should contain quantity_on_hand");
        assertTrue(json.contains("\"quantity_allocated\""),
                "Should contain quantity_allocated");
        assertTrue(json.contains("\"available_to_promise\""),
                "Should contain available_to_promise");

        // Should NOT contain camelCase
        assertFalse(json.contains("\"previousStockLevel\""),
                "Should NOT contain previousStockLevel");
        assertFalse(json.contains("\"newStockLevel\""),
                "Should NOT contain newStockLevel");
        assertFalse(json.contains("\"changeReason\""),
                "Should NOT contain changeReason");
        assertFalse(json.contains("\"quantityOnHand\""),
                "Should NOT contain quantityOnHand");
    }

    @Test
    void getEventData_shouldHaveNestedObjects() {
        // Given: A StockLevelChangedEvent
        StockLevel previousLevel = StockLevel.of(100, 20);
        StockLevel newLevel = StockLevel.of(150, 30);

        StockLevelChangedEvent event = new StockLevelChangedEvent(
                "SKU-TEST-002",
                previousLevel,
                newLevel,
                "ADJUSTMENT"
        );

        // When: Getting event data
        Map<String, Object> eventData = event.getEventData();

        // Then: previous_stock_level should be an object (StockLevelData)
        Object prevStockLevel = eventData.get("previous_stock_level");
        assertNotNull(prevStockLevel, "previous_stock_level should not be null");
        assertTrue(prevStockLevel instanceof com.paklog.inventory.application.dto.StockLevelData,
                "previous_stock_level should be StockLevelData instance");

        // And: new_stock_level should be an object (StockLevelData)
        Object newStockLevel = eventData.get("new_stock_level");
        assertNotNull(newStockLevel, "new_stock_level should not be null");
        assertTrue(newStockLevel instanceof com.paklog.inventory.application.dto.StockLevelData,
                "new_stock_level should be StockLevelData instance");
    }

    @Test
    void completeEventPayload_shouldMatchAsyncAPISpec() throws Exception {
        // Given: A complete stock level change scenario
        StockLevel before = StockLevel.of(200, 50);  // 200 on hand, 50 allocated, 150 ATP
        StockLevel after = StockLevel.of(250, 50);   // 250 on hand, 50 allocated, 200 ATP

        StockLevelChangedEvent event = new StockLevelChangedEvent(
                "PowerBank-10k-33",
                before,
                after,
                "PURCHASE_RECEIPT"
        );

        // When: Serializing complete event data
        String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(event.getEventData());

        System.out.println("\n📋 Complete Event Payload Example:");
        System.out.println(json);

        // Then: Should match AsyncAPI schema structure
        assertTrue(json.contains("\"sku\" : \"PowerBank-10k-33\""));
        assertTrue(json.contains("\"previous_stock_level\""));
        assertTrue(json.contains("\"new_stock_level\""));
        assertTrue(json.contains("\"change_reason\" : \"PURCHASE_RECEIPT\""));

        // Verify nested structure has correct values
        assertTrue(json.contains("\"quantity_on_hand\" : 200"));
        assertTrue(json.contains("\"quantity_on_hand\" : 250"));
        assertTrue(json.contains("\"quantity_allocated\" : 50"));
        assertTrue(json.contains("\"available_to_promise\" : 150"));
        assertTrue(json.contains("\"available_to_promise\" : 200"));
    }
}
