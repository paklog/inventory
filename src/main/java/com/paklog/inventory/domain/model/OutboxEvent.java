package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
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

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isProcessed() {
        return processed;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}