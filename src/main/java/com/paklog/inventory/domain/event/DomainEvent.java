package com.paklog.inventory.domain.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public abstract class DomainEvent {

    private final String eventId;
    private final String aggregateId;
    protected final LocalDateTime occurredOn;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = null;
        this.occurredOn = LocalDateTime.now();
    }

    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.occurredOn = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public abstract String getEventType();
    public abstract Map<String, Object> getEventData();
}