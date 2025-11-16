package com.paklog.inventory.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.domain.event.DomainEvent;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Factory for creating CloudEvents from domain events.
 * Ensures type consistency and proper CloudEvents spec compliance.
 */
@Component
public class CloudEventFactory {

    private static final String INVENTORY_SERVICE_SOURCE = "/fulfillment/inventory-service";
    private static final String DATA_CONTENT_TYPE = "application/json";

    private final ObjectMapper objectMapper;

    public CloudEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Create a CloudEvent from a domain event
     *
     * @param domainEvent The domain event to convert
     * @return CloudEvent compliant with CloudEvents v1.0 specification
     */
    public CloudEvent create(DomainEvent domainEvent) {
        try {
            // Convert domain event data to byte array
            byte[] dataBytes = objectMapper.writeValueAsBytes(domainEvent.getEventData());
            PojoCloudEventData<byte[]> data = PojoCloudEventData.wrap(dataBytes, bytes -> bytes);

            return CloudEventBuilder.v1()
                    .withId(domainEvent.getEventId())
                    .withSource(URI.create(INVENTORY_SERVICE_SOURCE))
                    .withType(domainEvent.getEventType())
                    .withTime(OffsetDateTime.of(domainEvent.getOccurredOn(), ZoneOffset.UTC))
                    .withSubject(domainEvent.getAggregateId())
                    .withDataContentType(DATA_CONTENT_TYPE)
                    .withData(data)
                    .build();

        } catch (Exception e) {
            throw new CloudEventCreationException(
                    String.format("Failed to create CloudEvent for type '%s'", domainEvent.getEventType()), e
            );
        }
    }

    /**
     * Exception thrown when CloudEvent creation fails
     */
    public static class CloudEventCreationException extends RuntimeException {
        public CloudEventCreationException(String message, Throwable cause) {
            super(message, cause);
        

}
}
}
