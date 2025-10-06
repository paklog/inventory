package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.InventorySnapshot;
import com.paklog.inventory.domain.model.SnapshotType;
import com.paklog.inventory.domain.repository.InventorySnapshotRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of InventorySnapshotRepository.
 */
@Component
public class InventorySnapshotRepositoryImpl implements InventorySnapshotRepository {

    private final InventorySnapshotSpringRepository springRepository;

    public InventorySnapshotRepositoryImpl(InventorySnapshotSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public InventorySnapshot save(InventorySnapshot snapshot) {
        InventorySnapshotDocument document = InventorySnapshotDocument.fromDomain(snapshot);
        InventorySnapshotDocument saved = springRepository.save(document);
        return saved.toDomain();
    }

    @Override
    public Optional<InventorySnapshot> findById(String snapshotId) {
        return springRepository.findBySnapshotId(snapshotId)
            .map(InventorySnapshotDocument::toDomain);
    }

    @Override
    public Optional<InventorySnapshot> findNearestSnapshotBefore(String sku, LocalDateTime targetTimestamp) {
        List<InventorySnapshotDocument> snapshots = springRepository
            .findBySkuAndSnapshotTimestampBefore(sku, targetTimestamp);

        // Find the snapshot with the latest timestamp before target
        return snapshots.stream()
            .max(Comparator.comparing(InventorySnapshotDocument::getSnapshotTimestamp))
            .map(InventorySnapshotDocument::toDomain);
    }

    @Override
    public Optional<InventorySnapshot> findBySkuAndTimestamp(String sku, LocalDateTime timestamp) {
        return springRepository.findBySkuAndSnapshotTimestamp(sku, timestamp)
            .map(InventorySnapshotDocument::toDomain);
    }

    @Override
    public List<InventorySnapshot> findBySkuAndTimestampBetween(String sku, LocalDateTime start, LocalDateTime end) {
        return springRepository.findBySkuAndSnapshotTimestampBetween(sku, start, end).stream()
            .map(InventorySnapshotDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InventorySnapshot> findByType(SnapshotType type) {
        return springRepository.findBySnapshotType(type.name()).stream()
            .map(InventorySnapshotDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InventorySnapshot> findBySkuAndType(String sku, SnapshotType type) {
        return springRepository.findBySkuAndSnapshotType(sku, type.name()).stream()
            .map(InventorySnapshotDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InventorySnapshot> findAllAtTimestamp(LocalDateTime timestamp) {
        return springRepository.findBySnapshotTimestamp(timestamp).stream()
            .map(InventorySnapshotDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InventorySnapshot> findAllByTypeAndTimestamp(SnapshotType type, LocalDateTime timestamp) {
        return springRepository.findBySnapshotTypeAndSnapshotTimestamp(type.name(), timestamp).stream()
            .map(InventorySnapshotDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteOlderThan(LocalDateTime cutoffDate) {
        springRepository.deleteBySnapshotTimestampBefore(cutoffDate);
    }

    @Override
    public void deleteByTypeOlderThan(SnapshotType type, LocalDateTime cutoffDate) {
        springRepository.deleteBySnapshotTypeAndSnapshotTimestampBefore(type.name(), cutoffDate);
    }

    @Override
    public long count() {
        return springRepository.count();
    }

    @Override
    public long countByType(SnapshotType type) {
        return springRepository.countBySnapshotType(type.name());
    }
}
