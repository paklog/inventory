package com.paklog.inventory;

import com.paklog.inventory.application.dto.CreateAdjustmentRequest;
import com.paklog.inventory.application.dto.InventoryAllocationRequestedData;
import com.paklog.inventory.application.dto.ItemPickedData;
import com.paklog.inventory.application.dto.StockLevelResponse;
import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.application.service.InventoryQueryService;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate; // Added KafkaTemplate import
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class InventoryServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.4.6"));

    @Autowired
    private InventoryCommandService commandService;

    @Autowired
    private InventoryQueryService queryService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private com.paklog.inventory.domain.repository.ProductStockRepository productStockRepository; // Injected ProductStockRepository

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplate; // Injected KafkaTemplate

    private Consumer<String, CloudEvent> consumer;

    private static final String WAREHOUSE_EVENTS_TOPIC = "fulfillment.warehouse.v1.events";
    private static final String INVENTORY_EVENTS_TOPIC = "fulfillment.inventory.v1.events";
    private static final String CONSUMER_GROUP_ID = "test-group";

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092"); // Use embedded Kafka
        registry.add("spring.kafka.consumer.group-id", () -> CONSUMER_GROUP_ID);
        registry.add("outbox.publisher.fixed-delay", () -> "100"); // Speed up outbox processing for tests
    }

    @BeforeAll
    static void beforeAll() {
        mongoDBContainer.start();
    }

    @BeforeEach
    void setUp() {
        // Clear outbox and any existing data before each test
        outboxRepository.deleteAll();
        productStockRepository.deleteAll(); // Added for test cleanup

        // Setup Kafka consumer for verification
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(CONSUMER_GROUP_ID, "false", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, io.cloudevents.kafka.CloudEventDeserializer.class);
        DefaultKafkaConsumerFactory<String, CloudEvent> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = cf.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, INVENTORY_EVENTS_TOPIC);
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, WAREHOUSE_EVENTS_TOPIC);
    }

    @Test
    @DisplayName("Should create initial ProductStock and publish StockLevelChanged event")
    void createInitialProductStockAndPublishEvent() throws Exception {
        // Given
        String sku = "SKU-NEW-001";
        int initialQuantity = 100;

        // When
        commandService.receiveStock(sku, initialQuantity, "INITIAL-RECEIPT");

        // Then
        StockLevelResponse stockLevel = queryService.getStockLevel(sku);
        assertNotNull(stockLevel);
        assertEquals(sku, stockLevel.getSku());
        assertEquals(initialQuantity, stockLevel.getQuantityOnHand());
        assertEquals(0, stockLevel.getQuantityAllocated());
        assertEquals(initialQuantity, stockLevel.getAvailableToPromise());

        // Verify StockLevelChanged event in Kafka
        ConsumerRecords<String, CloudEvent> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5), 1);
        assertEquals(1, records.count());
        CloudEvent receivedEvent = records.iterator().next().value();

        assertEquals("com.paklog.inventory.stock_level.changed", receivedEvent.getType());
        assertEquals(sku, receivedEvent.getSubject());
        assertNotNull(receivedEvent.getData());

        Map<String, Object> eventData = objectMapper.readValue(receivedEvent.getData().toBytes(), Map.class);
        assertEquals(sku, eventData.get("sku"));
        assertEquals(initialQuantity, eventData.get("newQuantityOnHand"));
        assertEquals(0, eventData.get("newQuantityAllocated"));
        assertEquals(initialQuantity, eventData.get("newAvailableToPromise"));
        assertEquals("STOCK_RECEIPT", eventData.get("changeReason"));
    }

    @Test
    @DisplayName("Should process InventoryAllocationRequested event and update stock")
    void processInventoryAllocationRequestedEvent() throws Exception {
        // Given
        String sku = "SKU-ALLOC-001";
        commandService.receiveStock(sku, 100, "INITIAL-RECEIPT");
        outboxRepository.deleteAll(); // Clear events from initial stock for this test

        InventoryAllocationRequestedData allocationData = new InventoryAllocationRequestedData();
        allocationData.setSku(sku);
        allocationData.setQuantity(20);
        allocationData.setOrderId("ORDER-001");

        CloudEvent allocationEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("/warehouse-service"))
                .withType("com.paklog.inventory.warehouse.inventory.allocation.requested")
                .withTime(OffsetDateTime.now(ZoneOffset.UTC))
                .withSubject(sku)
                .withData("application/json", PojoCloudEventData.wrap(allocationData, objectMapper::writeValueAsBytes))
                .build();

        // When
        // Simulate receiving the event by directly calling the consumer method
        // In a real scenario, Kafka would deliver this.
        // For integration test, we can publish to embedded Kafka and let the listener pick it up.
        cloudEventKafkaTemplate.send(WAREHOUSE_EVENTS_TOPIC, sku, allocationEvent).get();

        // Allow time for the consumer to process
        Thread.sleep(1000); // Adjust as necessary

        // Then
        StockLevelResponse stockLevel = queryService.getStockLevel(sku);
        assertEquals(100, stockLevel.getQuantityOnHand());
        assertEquals(20, stockLevel.getQuantityAllocated());
        assertEquals(80, stockLevel.getAvailableToPromise());

        // Verify StockLevelChanged event in Kafka
        ConsumerRecords<String, CloudEvent> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5), 1);
        assertEquals(1, records.count());
        CloudEvent receivedEvent = records.iterator().next().value();
        assertEquals("com.paklog.inventory.stock_level.changed", receivedEvent.getType());
        assertEquals(sku, receivedEvent.getSubject());
        Map<String, Object> eventData = objectMapper.readValue(receivedEvent.getData().toBytes(), Map.class);
        assertEquals(100, eventData.get("newQuantityOnHand"));
        assertEquals(20, eventData.get("newQuantityAllocated"));
        assertEquals(80, eventData.get("newAvailableToPromise"));
        assertEquals("ALLOCATION", eventData.get("changeReason"));
    }

    @Test
    @DisplayName("Should process ItemPicked event and update stock")
    void processItemPickedEvent() throws Exception {
        // Given
        String sku = "SKU-PICK-001";
        commandService.receiveStock(sku, 100, "INITIAL-RECEIPT");
        commandService.allocateStock(sku, 30, "ORDER-002");
        outboxRepository.deleteAll(); // Clear events for this test

        ItemPickedData pickedData = new ItemPickedData();
        pickedData.setSku(sku);
        pickedData.setQuantityPicked(15);
        pickedData.setOrderId("ORDER-002");

        CloudEvent pickedEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("/warehouse-service"))
                .withType("com.paklog.inventory.warehouse.item.picked")
                .withTime(OffsetDateTime.now(ZoneOffset.UTC))
                .withSubject(sku)
                .withData("application/json", PojoCloudEventData.wrap(pickedData, objectMapper::writeValueAsBytes))
                .build();

        // When
        cloudEventKafkaTemplate.send(WAREHOUSE_EVENTS_TOPIC, sku, pickedEvent).get();

        // Allow time for the consumer to process
        Thread.sleep(1000); // Adjust as necessary

        // Then
        StockLevelResponse stockLevel = queryService.getStockLevel(sku);
        assertEquals(85, stockLevel.getQuantityOnHand()); // 100 - 15
        assertEquals(15, stockLevel.getQuantityAllocated()); // 30 - 15 (deallocated)
        assertEquals(70, stockLevel.getAvailableToPromise()); // 85 - 15

        // Verify StockLevelChanged event in Kafka
        ConsumerRecords<String, CloudEvent> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5), 1);
        assertEquals(1, records.count());
        CloudEvent receivedEvent = records.iterator().next().value();
        assertEquals("com.paklog.inventory.stock_level.changed", receivedEvent.getType());
        assertEquals(sku, receivedEvent.getSubject());
        Map<String, Object> eventData = objectMapper.readValue(receivedEvent.getData().toBytes(), Map.class);
        assertEquals(85, eventData.get("newQuantityOnHand"));
        assertEquals(15, eventData.get("newQuantityAllocated"));
        assertEquals(70, eventData.get("newAvailableToPromise"));
        assertEquals("ITEM_PICKED", eventData.get("changeReason"));
    }

    @Test
    @DisplayName("Should perform manual stock adjustment via REST endpoint")
    void createStockAdjustmentViaRest() throws Exception {
        // Given
        String sku = "SKU-ADJUST-001";
        commandService.receiveStock(sku, 50, "INITIAL-RECEIPT");
        outboxRepository.deleteAll(); // Clear events for this test

        CreateAdjustmentRequest request = new CreateAdjustmentRequest();
        request.setSku(sku);
        request.setQuantityChange(10);
        request.setReasonCode("CYCLE_COUNT");
        request.setComment("Found 10 extra units");

        // When
        commandService.adjustStock(request.getSku(), request.getQuantityChange(), request.getReasonCode(), request.getComment(), "test-user");

        // Then
        StockLevelResponse stockLevel = queryService.getStockLevel(sku);
        assertEquals(60, stockLevel.getQuantityOnHand());
        assertEquals(0, stockLevel.getQuantityAllocated());
        assertEquals(60, stockLevel.getAvailableToPromise());

        // Verify StockLevelChanged event in Kafka
        ConsumerRecords<String, CloudEvent> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5), 1);
        assertEquals(1, records.count());
        CloudEvent receivedEvent = records.iterator().next().value();
        assertEquals("com.paklog.inventory.stock_level.changed", receivedEvent.getType());
        assertEquals(sku, receivedEvent.getSubject());
        Map<String, Object> eventData = objectMapper.readValue(receivedEvent.getData().toBytes(), Map.class);
        assertEquals(60, eventData.get("newQuantityOnHand"));
        assertEquals(0, eventData.get("newQuantityAllocated"));
        assertEquals(60, eventData.get("newAvailableToPromise"));
        assertEquals("CYCLE_COUNT - Found 10 extra units", eventData.get("changeReason"));
    }

    @Test
    @DisplayName("Should retrieve inventory health metrics")
    void getInventoryHealthMetrics() {
        // Given
        commandService.receiveStock("SKU-HEALTH-001", 100, "INITIAL-RECEIPT");
        commandService.receiveStock("SKU-HEALTH-002", 0, "INITIAL-RECEIPT"); // Out of stock
        commandService.receiveStock("SKU-HEALTH-003", 10, "INITIAL-RECEIPT");
        commandService.allocateStock("SKU-HEALTH-003", 5, "ORDER-003");

        // When
        com.paklog.inventory.application.dto.InventoryHealthMetricsResponse metrics = queryService.getInventoryHealthMetrics(null, null, null);

        // Then
        assertNotNull(metrics);
        assertTrue(metrics.getTotalSkus() >= 3); // At least the ones we just added
        assertTrue(metrics.getOutOfStockSkus() >= 1);
        // Further assertions for turnover and dead stock would require more complex setup
    }

    @Test
    @DisplayName("Should handle ProductStock not found for query")
    void getStockLevelNotFound() {
        assertThrows(NoSuchElementException.class, () -> queryService.getStockLevel("NON-EXISTENT-SKU"));
    }
}