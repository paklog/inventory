package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OutboxRepositoryImpl implements OutboxRepository {

    private final OutboxSpringRepository springRepository;

    public OutboxRepositoryImpl(OutboxSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return springRepository.save(event);
    }

    @Override
    public List<OutboxEvent> findByProcessedFalse() {
        return springRepository.findByProcessedFalseOrderByCreatedAtAsc();
    }

    @Override
    public List<OutboxEvent> findEventsBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return springRepository.findAll().stream()
            .filter(event -> {
                LocalDateTime eventTime = event.getCreatedAt();
                return !eventTime.isBefore(startTime) && !eventTime.isAfter(endTime);
            })
            .collect(Collectors.toList());
    }

    @Override
    public Optional<OutboxEvent> findById(String id) {
        return springRepository.findById(id);
    }

    @Override
    public void saveAll(List<OutboxEvent> events) {
        springRepository.saveAll(events);
    }
    @Override
    public void deleteAll() {
        springRepository.deleteAll();
    }
}