package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.OutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventSpringRepository extends MongoRepository<OutboxEvent, String> {
    List<OutboxEvent> findByProcessedFalse();
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
    List<OutboxEvent> findByAggregateId(String aggregateId);
    List<OutboxEvent> findByProcessedFalseAndCreatedAtBefore(LocalDateTime dateTime);
    void deleteByProcessedTrueAndCreatedAtBefore(LocalDateTime dateTime);
}
