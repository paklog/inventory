package com.paklog.inventory.infrastructure.messaging;

import com.paklog.inventory.application.port.EventPublisherPort;
import com.paklog.inventory.domain.event.DomainEvent;
import com.paklog.inventory.infrastructure.observability.InventoryMetrics;
import io.cloudevents.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events as CloudEvents to Kafka.
 * Validates events against JSON schemas before publishing.
 */
@Component
public class CloudEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(CloudEventPublisher.class);
    private static final String INVENTORY_EVENTS_TOPIC = "fulfillment.inventory.v1.events";

    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;
    private final CloudEventFactory cloudEventFactory;
    private final CloudEventSchemaValidator schemaValidator;
    private final InventoryMetrics metrics;

    public CloudEventPublisher(
            KafkaTemplate<String, CloudEvent> kafkaTemplate,
            CloudEventFactory cloudEventFactory,
            CloudEventSchemaValidator schemaValidator,
            InventoryMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.cloudEventFactory = cloudEventFactory;
        this.schemaValidator = schemaValidator;
        this.metrics = metrics;
    }

    @Override
    public void publish(DomainEvent domainEvent) {
        try {
            // Step 1: Validate event data against JSON schema
            schemaValidator.validate(domainEvent);

            // Step 2: Create CloudEvent
            CloudEvent cloudEvent = cloudEventFactory.create(domainEvent);

            // Step 3: Publish to Kafka
            kafkaTemplate.send(INVENTORY_EVENTS_TOPIC, cloudEvent.getSubject(), cloudEvent);

            // Step 4: Record metrics
            metrics.recordCloudEventPublished();

            log.info("Published CloudEvent: type={}, subject={}, id={}",
                    cloudEvent.getType(), cloudEvent.getSubject(), cloudEvent.getId());

        } catch (CloudEventSchemaValidator.CloudEventValidationException e) {
            metrics.recordCloudEventFailed();
            log.error("CloudEvent validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            metrics.recordCloudEventFailed();
            log.error("Failed to publish CloudEvent for type: {}", domainEvent.getEventType(), e);
            throw new RuntimeException("Failed to publish CloudEvent", e);
        

}
}
}
