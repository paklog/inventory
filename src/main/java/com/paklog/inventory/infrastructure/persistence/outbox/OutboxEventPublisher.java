package com.paklog.inventory.infrastructure.persistence.outbox;

import com.paklog.inventory.application.port.EventPublisherPort;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map; // Added import for Map
import com.paklog.inventory.domain.event.DomainEvent; // Corrected package name

@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
    private final OutboxRepository outboxRepository;
    private final EventPublisherPort eventPublisherPort;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxRepository outboxRepository, EventPublisherPort eventPublisherPort, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.eventPublisherPort = eventPublisherPort;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay:5000}") // Default to 5 seconds
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> events = outboxRepository.findByProcessedFalse();
        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} unprocessed outbox events.", events.size());

        for (OutboxEvent event : events) {
            try {
                // Reconstruct DomainEvent from stored JSON data
                // This assumes the original DomainEvent can be deserialized from its data map
                // For simplicity, we're directly publishing the CloudEvent data here.
                // In a more complex scenario, you might need a factory to reconstruct the specific DomainEvent type.
                // For now, the CloudEventPublisher directly uses the eventType and eventData from OutboxEvent.
                eventPublisherPort.publish(new ReconstructedDomainEvent(event.getAggregateId(), event.getEventType(), objectMapper.readValue(event.getEventData(), Map.class)));
                event.markAsProcessed();
            } catch (Exception e) {
                log.error("Failed to publish outbox event with ID {}: {}", event.getId(), e.getMessage(), e);
                // Depending on the error, you might want to mark the event as failed or retry later
            }
        }
        outboxRepository.saveAll(events);
        log.info("Published and marked {} outbox events as processed.", events.size());
    }

    // A simple placeholder for reconstructing a DomainEvent from OutboxEvent data
    private static class ReconstructedDomainEvent extends DomainEvent { // Changed to use the correct DomainEvent
        private final String eventType;
        private final Map<String, Object> eventData;

        public ReconstructedDomainEvent(String aggregateId, String eventType, Map<String, Object> eventData) {
            super(aggregateId);
            this.eventType = eventType;
            this.eventData = eventData;
        }

        @Override
        public String getEventType() {
            return eventType;
        }

        @Override
        public Map<String, Object> getEventData() {
            return eventData;
        }

        // Add equals and hashCode for completeness, though not strictly required for compilation
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ReconstructedDomainEvent that = (ReconstructedDomainEvent) o;
            return eventType.equals(that.eventType) && eventData.equals(that.eventData);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(super.hashCode(), eventType, eventData);
        }
    }
}