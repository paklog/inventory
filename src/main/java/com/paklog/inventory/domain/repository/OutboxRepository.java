package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxRepository {
    OutboxEvent save(OutboxEvent event);
    List<OutboxEvent> findByProcessedFalse();
    List<OutboxEvent> findEventsBetween(LocalDateTime startTime, LocalDateTime endTime);
    Optional<OutboxEvent> findById(String id);
    void saveAll(List<OutboxEvent> events);
    void deleteAll();
}