package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);
    List<OutboxEvent> saveAll(Iterable<OutboxEvent> events);
    Optional<OutboxEvent> findById(String id);
    List<OutboxEvent> findAll();
    List<OutboxEvent> findUnprocessedEvents();
    List<OutboxEvent> findByProcessedFalse();
    List<OutboxEvent> findByPublishedFalse(); // Alias for findByProcessedFalse
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();
    List<OutboxEvent> findByAggregateId(String aggregateId);
    List<OutboxEvent> findByProcessedFalseAndCreatedAtBefore(LocalDateTime dateTime);
    void deleteByPublishedTrueAndCreatedAtBefore(LocalDateTime dateTime);
    void deleteAll();
}
