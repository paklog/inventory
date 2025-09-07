package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.OutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxSpringRepository extends MongoRepository<OutboxEvent, String> {
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
    void deleteAll();
}