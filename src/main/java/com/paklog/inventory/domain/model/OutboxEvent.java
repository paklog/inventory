package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "outbox_events")
@CompoundIndex(name = "processed_created_idx", def = "{'processed': 1, 'createdAt': 1}")
public class OutboxEvent {

    @Id
    private String id;
    private String aggregateId;
    private String eventType;
    private String eventData; // JSON string of the event payload
    private LocalDateTime createdAt;
    private boolean processed;
    private LocalDateTime processedAt;

    // Private constructor for internal use and reconstruction from persistence
    private OutboxEvent(String id, String aggregateId, String eventType, String eventData,
                        LocalDateTime createdAt, boolean processed, LocalDateTime processedAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.createdAt = createdAt;
        this.processed = processed;
        this.processedAt = processedAt;
    }

    public static OutboxEvent from(DomainEvent domainEvent) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register modules for Java 8 Date/Time API
        // Use snake_case naming strategy for CloudEvents compliance
        objectMapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
        try {
            String eventData = objectMapper.writeValueAsString(domainEvent.getEventData());
            return new OutboxEvent(
                    domainEvent.getEventId(),
                    domainEvent.getAggregateId(),
                    domainEvent.getEventType(),
                    eventData,
                    domainEvent.getOccurredOn(),
                    false,
                    null
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize domain event to JSON for outbox.", e);
        }
    }

    public static OutboxEvent create(String eventType, String aggregateId, Map<String, Object> payload) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        // Use snake_case naming strategy for CloudEvents compliance
        objectMapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
        try {
            String eventData = objectMapper.writeValueAsString(payload);
            return new OutboxEvent(
                    UUID.randomUUID().toString(),
                    aggregateId,
                    eventType,
                    eventData,
                    LocalDateTime.now(),
                    false,
                    null
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payload to JSON for outbox.", e);
        }
    }

    public static OutboxEvent load(String id, String eventType, String aggregateId, Map<String, Object> payload,
                                    LocalDateTime createdAt, boolean published, LocalDateTime publishedAt) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        // Use snake_case naming strategy for CloudEvents compliance
        objectMapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
        try {
            String eventData = objectMapper.writeValueAsString(payload);
            return new OutboxEvent(
                    id != null ? id : UUID.randomUUID().toString(),
                    aggregateId,
                    eventType,
                    eventData,
                    createdAt,
                    published,
                    publishedAt
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payload to JSON for outbox.", e);
        }
    }

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }

    // Alias methods for "published" terminology (used by tests)
    public void markAsPublished() {
        markAsProcessed();
    }

    public boolean isPublished() {
        return isProcessed();
    }

    public LocalDateTime getPublishedAt() {
        return getProcessedAt();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public Map<String, Object> getPayload() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        // Use snake_case naming strategy for CloudEvents compliance
        objectMapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
        try {
            return objectMapper.readValue(eventData, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize event data", e);
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isProcessed() {
        return processed;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String aggregateId;
        private String eventType;
        private String eventData;
        private LocalDateTime createdAt;
        private boolean processed;
        private LocalDateTime processedAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder aggregateId(String aggregateId) { this.aggregateId = aggregateId; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder eventData(String eventData) { this.eventData = eventData; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder processed(boolean processed) { this.processed = processed; return this; }
        public Builder processedAt(LocalDateTime processedAt) { this.processedAt = processedAt; return this; }

        public OutboxEvent build() {
            return new OutboxEvent(id, aggregateId, eventType, eventData, createdAt, processed, processedAt);
        }
    }
}
