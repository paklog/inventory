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
}