package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.InventoryLedgerEntry;
import com.paklog.inventory.domain.repository.InventoryLedgerRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InventoryLedgerRepositoryImpl implements InventoryLedgerRepository {

    private final InventoryLedgerSpringRepository springRepository;

    public InventoryLedgerRepositoryImpl(InventoryLedgerSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public InventoryLedgerEntry save(InventoryLedgerEntry entry) {
        InventoryLedgerEntryDocument doc = InventoryLedgerEntryDocument.fromDomain(entry);
        springRepository.save(doc);
        return entry;
    }

    @Override
    public List<InventoryLedgerEntry> saveAll(Iterable<InventoryLedgerEntry> entries) {
        List<InventoryLedgerEntryDocument> docs = new java.util.ArrayList<>();
        for (InventoryLedgerEntry entry : entries) {
            docs.add(InventoryLedgerEntryDocument.fromDomain(entry));
        }
        springRepository.saveAll(docs);

        // Return the original list
        List<InventoryLedgerEntry> result = new java.util.ArrayList<>();
        entries.forEach(result::add);
        return result;
    }

    @Override
    public List<InventoryLedgerEntry> findAll() {
        return springRepository.findAll().stream()
                .map(InventoryLedgerEntryDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryLedgerEntry> findBySku(String sku) {
        return springRepository.findBySku(sku).stream()
                .map(InventoryLedgerEntryDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryLedgerEntry> findBySkuAndTimestampBetween(String sku, LocalDateTime start, LocalDateTime end) {
        return springRepository.findBySkuAndTimestampBetween(sku, start, end).stream()
                .map(InventoryLedgerEntryDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryLedgerEntry> findByChangeType(com.paklog.inventory.domain.model.ChangeType changeType) {
        return springRepository.findByChangeType(changeType.name()).stream()
                .map(InventoryLedgerEntryDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int findTotalQuantityPickedBySkuAndDateRange(String sku, LocalDateTime start, LocalDateTime end) {
        return springRepository.findPicksBySkuAndDateRange(sku, start, end).stream()
                .mapToInt(InventoryLedgerEntryDocument::getQuantityChange)
                .sum();
    }

    @Override
    public Map<String, Integer> findTotalQuantityPickedBySkusAndDateRange(List<String> skus, LocalDateTime start, LocalDateTime end) {
        List<InventoryLedgerEntryDocument> picks = springRepository.findPicksBySkusAndDateRange(skus, start, end);

        // Group by SKU and sum quantities
        return picks.stream()
                .collect(Collectors.groupingBy(
                        InventoryLedgerEntryDocument::getSku,
                        Collectors.summingInt(InventoryLedgerEntryDocument::getQuantityChange)
                ));
    }

    @Override
    public void deleteAll() {
        springRepository.deleteAll();
    }
}