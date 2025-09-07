package com.paklog.inventory.infrastructure.messaging;

import com.paklog.inventory.application.port.EventPublisherPort;
import com.paklog.inventory.domain.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper; // Added ObjectMapper import
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Component
public class CloudEventPublisher implements EventPublisherPort {

    private static final String INVENTORY_SERVICE_SOURCE = "/fulfillment/inventory-service";
    private static final String STOCK_LEVEL_CHANGED_TOPIC = "fulfillment.inventory.v1.events";

    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;
    private final ObjectMapper objectMapper; // Added ObjectMapper

    public CloudEventPublisher(KafkaTemplate<String, CloudEvent> kafkaTemplate, ObjectMapper objectMapper) { // Added ObjectMapper to constructor
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper; // Initialize ObjectMapper
    }

    @Override
    public void publish(DomainEvent domainEvent) {
        CloudEvent cloudEvent = buildCloudEvent(domainEvent);
        kafkaTemplate.send(STOCK_LEVEL_CHANGED_TOPIC, cloudEvent.getSubject(), cloudEvent);
    }

    private CloudEvent buildCloudEvent(DomainEvent domainEvent) {
        // Use the specific event type from the domain event
        String eventType = domainEvent.getEventType();
        String subject = domainEvent.getAggregateId(); // For ProductStock, aggregateId is SKU

        // Convert domain event data to a CloudEventData payload
        // Use ObjectMapper to convert Map to byte[]
        try {
            byte[] dataBytes = objectMapper.writeValueAsBytes(domainEvent.getEventData());
            PojoCloudEventData<byte[]> data = PojoCloudEventData.wrap(dataBytes, bytes -> bytes);

            return CloudEventBuilder.v1()
                    .withId(domainEvent.getEventId())
                    .withSource(URI.create(INVENTORY_SERVICE_SOURCE))
                    .withType(eventType)
                    .withTime(OffsetDateTime.of(domainEvent.getOccurredOn(), ZoneOffset.UTC))
                    .withSubject(subject)
                    .withData("application/json", data)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize domain event data to CloudEventData", e);
        }
    }
}