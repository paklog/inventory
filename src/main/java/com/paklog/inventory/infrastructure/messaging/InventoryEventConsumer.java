package com.paklog.inventory.infrastructure.messaging;

import com.paklog.inventory.infrastructure.messaging.strategy.EventHandler;
import io.cloudevents.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);
    private final List<EventHandler> eventHandlers;

    public InventoryEventConsumer(List<EventHandler> eventHandlers) {
        this.eventHandlers = eventHandlers;
    }

    @KafkaListener(topics = "fulfillment.warehouse.v1.events", groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "cloudEventKafkaListenerContainerFactory")
    public void listenForWarehouseEvents(CloudEvent event, Acknowledgment ack) {
        log.info("Received CloudEvent: id={}, type={}, source={}, subject={}",
                event.getId(), event.getType(), event.getSource(), event.getSubject());

        try {
            Optional<EventHandler> handler = eventHandlers.stream()
                    .filter(h -> h.canHandle(event.getType()))
                    .findFirst();

            if (handler.isPresent()) {
                handler.get().handle(event);
                log.debug("Successfully processed event type: {} with handler: {}", 
                         event.getType(), handler.get().getClass().getSimpleName());
            } else {
                log.warn("No handler found for CloudEvent type: {}", event.getType());
            }
            
            ack.acknowledge(); // Commit offset after successful processing
        } catch (Exception e) {
            log.error("Error processing CloudEvent with ID {}: {}", event.getId(), e.getMessage(), e);
            // Depending on the error, you might want to send to a Dead Letter Topic
            // For now, we'll let the message be retried by not acknowledging.
            // Or, if it's a permanent error, acknowledge and log.
        

}
}
}
