package com.paklog.inventory.infrastructure.async;

import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Async event publishing service for domain events.
 * Publishes events to outbox in background to avoid blocking main transaction.
 */
@Service
public class AsyncEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AsyncEventPublisher.class);

    private final OutboxRepository outboxRepository;

    public AsyncEventPublisher(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /**
     * Asynchronously publish domain events to outbox.
     * Returns CompletableFuture for caller to wait if needed.
     *
     * @param events List of outbox events to publish
     * @return CompletableFuture that completes when events are published
     */
    @Async("eventExecutor")
    public CompletableFuture<Void> publishEventsAsync(List<OutboxEvent> events) {
        try {
            if (events.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            log.debug("Async publishing {} domain events", events.size());
            outboxRepository.saveAll(events);
            log.info("Successfully published {} events asynchronously", events.size());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to publish {} events asynchronously", events.size(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Fire-and-forget event publishing.
     * Use when caller doesn't need to know about publish completion.
     */
    @Async("eventExecutor")
    public void publishEventsFireAndForget(List<OutboxEvent> events) {
        try {
            if (events.isEmpty()) {
                return;
            }

            log.debug("Fire-and-forget publishing {} domain events", events.size());
            outboxRepository.saveAll(events);
            log.info("Published {} events (fire-and-forget)", events.size());
        } catch (Exception e) {
            log.error("Failed to publish {} events (fire-and-forget)", events.size(), e);
            // Don't propagate - this is fire-and-forget
        }
    }

    /**
     * Asynchronously publish single event.
     */
    @Async("eventExecutor")
    public CompletableFuture<OutboxEvent> publishEventAsync(OutboxEvent event) {
        try {
            log.debug("Async publishing event: {}", event.getEventType());
            OutboxEvent saved = outboxRepository.save(event);
            return CompletableFuture.completedFuture(saved);
        } catch (Exception e) {
            log.error("Failed to publish event asynchronously: {}", event.getEventType(), e);
            return CompletableFuture.failedFuture(e);
        

}
}
}
