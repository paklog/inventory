package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventSpringRepository springRepository;

    public OutboxEventRepositoryImpl(OutboxEventSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return springRepository.save(event);
    }

    @Override
    public List<OutboxEvent> saveAll(Iterable<OutboxEvent> events) {
        return springRepository.saveAll(events);
    }

    @Override
    public Optional<OutboxEvent> findById(String id) {
        return springRepository.findById(id);
    }

    @Override
    public List<OutboxEvent> findAll() {
        return springRepository.findAll();
    }

    @Override
    public List<OutboxEvent> findUnprocessedEvents() {
        return springRepository.findByProcessedFalse();
    }

    @Override
    public List<OutboxEvent> findByProcessedFalse() {
        return springRepository.findByProcessedFalse();
    }

    @Override
    public List<OutboxEvent> findByPublishedFalse() {
        return findByProcessedFalse();
    }

    @Override
    public List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc() {
        return springRepository.findByProcessedFalseOrderByCreatedAtAsc();
    }

    @Override
    public List<OutboxEvent> findByAggregateId(String aggregateId) {
        return springRepository.findByAggregateId(aggregateId);
    }

    @Override
    public List<OutboxEvent> findByProcessedFalseAndCreatedAtBefore(LocalDateTime dateTime) {
        return springRepository.findByProcessedFalseAndCreatedAtBefore(dateTime);
    }

    @Override
    public void deleteByPublishedTrueAndCreatedAtBefore(LocalDateTime dateTime) {
        springRepository.deleteByProcessedTrueAndCreatedAtBefore(dateTime);
    }

    @Override
    public void deleteAll() {
        springRepository.deleteAll();
    }
}
