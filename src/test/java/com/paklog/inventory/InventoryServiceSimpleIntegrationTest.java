package com.paklog.inventory;

import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.application.service.InventoryQueryService;
import com.paklog.inventory.application.dto.StockLevelResponse;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                "outbox.publisher.enabled=false"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers(disabledWithoutDocker = true)
class InventoryServiceSimpleIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Autowired
    private InventoryCommandService commandService;

    @Autowired
    private InventoryQueryService queryService;

    @Autowired
    private ProductStockRepository productStockRepository;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @BeforeEach
    void setUp() {
        // Clear any existing data before each test
        productStockRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create and retrieve stock level")
    void shouldCreateAndRetrieveStockLevel() {
        // Arrange
        String sku = "TEST-SKU-001";
        int quantity = 100;

        // Act - Create stock via command service
        ProductStock createdStock = commandService.receiveStock(sku, quantity, "RECEIPT-001");

        // Assert - Verify stock was created
        assertNotNull(createdStock);
        assertEquals(sku, createdStock.getSku());
        assertEquals(quantity, createdStock.getQuantityOnHand());
        assertEquals(0, createdStock.getQuantityAllocated());
        assertEquals(quantity, createdStock.getAvailableToPromise());

        // Act - Retrieve stock via query service
        StockLevelResponse stockLevel = queryService.getStockLevel(sku);

        // Assert - Verify stock can be retrieved
        assertNotNull(stockLevel);
        assertEquals(sku, stockLevel.getSku());
        assertEquals(quantity, stockLevel.getQuantityOnHand());
        assertEquals(0, stockLevel.getQuantityAllocated());
        assertEquals(quantity, stockLevel.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should handle stock allocation and deallocation")
    void shouldHandleStockAllocationAndDeallocation() {
        // Arrange
        String sku = "TEST-SKU-002";
        int initialQuantity = 100;
        int allocationQuantity = 30;

        // Act - Create initial stock
        commandService.receiveStock(sku, initialQuantity, "RECEIPT-002");
        
        // Allocate stock
        ProductStock allocatedStock = commandService.allocateStock(sku, allocationQuantity, "ORDER-001");

        // Assert - Verify allocation
        assertEquals(initialQuantity, allocatedStock.getQuantityOnHand());
        assertEquals(allocationQuantity, allocatedStock.getQuantityAllocated());
        assertEquals(initialQuantity - allocationQuantity, allocatedStock.getAvailableToPromise());

        // Act - Process item picked (which deallocates and reduces on hand)
        ProductStock pickedStock = commandService.processItemPicked(sku, allocationQuantity, "ORDER-001");

        // Assert - Verify picked processing
        assertEquals(initialQuantity - allocationQuantity, pickedStock.getQuantityOnHand());
        assertEquals(0, pickedStock.getQuantityAllocated());
        assertEquals(initialQuantity - allocationQuantity, pickedStock.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should handle stock adjustments")
    void shouldHandleStockAdjustments() {
        // Arrange
        String sku = "TEST-SKU-003";
        int initialQuantity = 100;
        int adjustment = 25;

        // Act - Create initial stock
        commandService.receiveStock(sku, initialQuantity, "RECEIPT-003");
        
        // Perform adjustment
        ProductStock adjustedStock = commandService.adjustStock(sku, adjustment, "CYCLE_COUNT", "Found extra units", "admin");

        // Assert - Verify adjustment
        assertEquals(initialQuantity + adjustment, adjustedStock.getQuantityOnHand());
        assertEquals(0, adjustedStock.getQuantityAllocated());
        assertEquals(initialQuantity + adjustment, adjustedStock.getAvailableToPromise());
    }
}
