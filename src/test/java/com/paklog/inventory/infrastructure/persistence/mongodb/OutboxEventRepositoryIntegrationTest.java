package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for OutboxEventRepository.
 */
@DataMongoTest
@Testcontainers
@Import({OutboxEventRepositoryImpl.class})
class OutboxEventRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private OutboxEventRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save outbox event")
    void shouldSaveOutboxEvent() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("sku", "SKU-001");
        payload.put("quantity", 100);

        OutboxEvent event = OutboxEvent.create(
                "stock.level.changed",
                "SKU-001",
                payload
        );

        // When
        OutboxEvent saved = outboxRepository.save(event);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventType()).isEqualTo("stock.level.changed");
        assertThat(saved.getAggregateId()).isEqualTo("SKU-001");
        assertThat(saved.isPublished()).isFalse();
    }

    @Test
    @DisplayName("Should find unpublished events")
    void shouldFindUnpublishedEvents() {
        // Given
        outboxRepository.save(createEvent("event-1", false));
        outboxRepository.save(createEvent("event-2", false));
        outboxRepository.save(createEvent("event-3", true)); // published
        outboxRepository.save(createEvent("event-4", false));

        // When
        List<OutboxEvent> unpublished = outboxRepository.findByPublishedFalse();

        // Then
        assertThat(unpublished).hasSize(3);
        assertThat(unpublished).allMatch(e -> !e.isPublished());
    }

    @Test
    @DisplayName("Should mark event as published")
    void shouldMarkEventAsPublished() {
        // Given
        OutboxEvent event = createEvent("test-event", false);
        OutboxEvent saved = outboxRepository.save(event);

        // When
        saved.markAsPublished();
        outboxRepository.save(saved);

        // Then
        OutboxEvent found = outboxRepository.findById(saved.getId()).get();
        assertThat(found.isPublished()).isTrue();
        assertThat(found.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should save multiple events in batch")
    void shouldSaveMultipleEventsInBatch() {
        // Given
        List<OutboxEvent> events = List.of(
                createEvent("event-1", false),
                createEvent("event-2", false),
                createEvent("event-3", false)
        );

        // When
        List<OutboxEvent> saved = outboxRepository.saveAll(events);

        // Then
        assertThat(saved).hasSize(3);
        assertThat(outboxRepository.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("Should find events by aggregate ID")
    void shouldFindEventsByAggregateId() {
        // Given
        outboxRepository.save(createEventForAggregate("AGG-001", "event-1"));
        outboxRepository.save(createEventForAggregate("AGG-001", "event-2"));
        outboxRepository.save(createEventForAggregate("AGG-002", "event-3"));

        // When
        List<OutboxEvent> events = outboxRepository.findByAggregateId("AGG-001");

        // Then
        assertThat(events).hasSize(2);
        assertThat(events).allMatch(e -> e.getAggregateId().equals("AGG-001"));
    }

    @Test
    @DisplayName("Should order unpublished events by creation time")
    void shouldOrderUnpublishedEventsByCreationTime() {
        // Given - save in reverse chronological order
        LocalDateTime now = LocalDateTime.now();

        OutboxEvent event1 = createEventWithTimestamp("event-1", now.minusHours(3), false);
        OutboxEvent event2 = createEventWithTimestamp("event-2", now.minusHours(2), false);
        OutboxEvent event3 = createEventWithTimestamp("event-3", now.minusHours(1), false);

        outboxRepository.save(event3);
        outboxRepository.save(event1);
        outboxRepository.save(event2);

        // When
        List<OutboxEvent> unpublished = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();

        // Then
        assertThat(unpublished).hasSize(3);
        // Should be ordered oldest first
        assertThat(unpublished.get(0).getEventType()).isEqualTo("event-1");
        assertThat(unpublished.get(1).getEventType()).isEqualTo("event-2");
        assertThat(unpublished.get(2).getEventType()).isEqualTo("event-3");
    }

    @Test
    @DisplayName("Should delete published events older than cutoff date")
    void shouldDeletePublishedEventsOlderThanCutoffDate() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDate = now.minusDays(7);

        // Old published event (should be deleted)
        OutboxEvent oldPublished = createEventWithTimestamp("old-published", now.minusDays(10), true);
        // Recent published event (should be kept)
        OutboxEvent recentPublished = createEventWithTimestamp("recent-published", now.minusDays(3), true);
        // Old unpublished event (should be kept)
        OutboxEvent oldUnpublished = createEventWithTimestamp("old-unpublished", now.minusDays(10), false);

        outboxRepository.save(oldPublished);
        outboxRepository.save(recentPublished);
        outboxRepository.save(oldUnpublished);

        // When
        outboxRepository.deleteByPublishedTrueAndCreatedAtBefore(cutoffDate);

        // Then
        List<OutboxEvent> remaining = outboxRepository.findAll();
        assertThat(remaining).hasSize(2);
        assertThat(remaining).noneMatch(e -> e.getEventType().equals("old-published"));
    }

    @Test
    @DisplayName("Should persist all outbox event fields correctly")
    void shouldPersistAllOutboxEventFieldsCorrectly() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("field1", "value1");
        payload.put("field2", 123);
        payload.put("field3", true);

        OutboxEvent event = OutboxEvent.create(
                "test.event.type",
                "TEST-AGG-001",
                payload
        );

        // When
        outboxRepository.save(event);
        List<OutboxEvent> found = outboxRepository.findByAggregateId("TEST-AGG-001");

        // Then
        assertThat(found).hasSize(1);
        OutboxEvent savedEvent = found.get(0);
        assertThat(savedEvent.getEventType()).isEqualTo("test.event.type");
        assertThat(savedEvent.getAggregateId()).isEqualTo("TEST-AGG-001");
        assertThat(savedEvent.getPayload()).containsEntry("field1", "value1");
        assertThat(savedEvent.getPayload()).containsEntry("field2", 123);
        assertThat(savedEvent.getPayload()).containsEntry("field3", true);
        assertThat(savedEvent.getCreatedAt()).isNotNull();
        assertThat(savedEvent.isPublished()).isFalse();
        assertThat(savedEvent.getPublishedAt()).isNull();
    }

    // Helper methods
    private OutboxEvent createEvent(String eventType, boolean published) {
        OutboxEvent event = OutboxEvent.create(
                eventType,
                "AGG-DEFAULT",
                Map.of("data", "test")
        );
        if (published) {
            event.markAsPublished();
        }
        return event;
    }

    private OutboxEvent createEventForAggregate(String aggregateId, String eventType) {
        return OutboxEvent.create(
                eventType,
                aggregateId,
                Map.of("data", "test")
        );
    }

    private OutboxEvent createEventWithTimestamp(String eventType, LocalDateTime createdAt, boolean published) {
        OutboxEvent event = OutboxEvent.load(
                null,
                eventType,
                "AGG-DEFAULT",
                Map.of("data", "test"),
                createdAt,
                published,
                published ? createdAt.plusMinutes(5) : null
        );
        return event;
    }
}
