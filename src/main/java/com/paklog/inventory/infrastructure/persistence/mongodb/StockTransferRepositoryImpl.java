package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.StockTransfer;
import com.paklog.inventory.domain.model.TransferStatus;
import com.paklog.inventory.domain.repository.StockTransferRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of StockTransferRepository
 */
@Component
public class StockTransferRepositoryImpl implements StockTransferRepository {

    private final StockTransferSpringRepository springRepository;

    public StockTransferRepositoryImpl(StockTransferSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public StockTransfer save(StockTransfer transfer) {
        StockTransferDocument doc = StockTransferDocument.fromDomain(transfer);
        StockTransferDocument saved = springRepository.save(doc);
        return saved.toDomain();
    }

    @Override
    public Optional<StockTransfer> findById(String transferId) {
        return springRepository.findById(transferId)
                .map(StockTransferDocument::toDomain);
    }

    @Override
    public List<StockTransfer> findBySku(String sku) {
        return springRepository.findBySku(sku).stream()
                .map(StockTransferDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockTransfer> findByStatus(TransferStatus status) {
        return springRepository.findByStatus(status.name()).stream()
                .map(StockTransferDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockTransfer> findBySkuAndStatus(String sku, TransferStatus status) {
        return springRepository.findBySkuAndStatus(sku, status.name()).stream()
                .map(StockTransferDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockTransfer> findInTransitTransfers() {
        return findByStatus(TransferStatus.IN_TRANSIT);
    }

    @Override
    public List<StockTransfer> findByInitiatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return springRepository.findByInitiatedAtBetween(start, end).stream()
                .map(StockTransferDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<StockTransfer> findTransfersWithShrinkage() {
        return springRepository.findTransfersWithShrinkage().stream()
                .map(StockTransferDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByStatus(TransferStatus status) {
        return springRepository.countByStatus(status.name());
    }

    @Override
    public void delete(StockTransfer transfer) {
        springRepository.deleteById(transfer.getTransferId());
    }
}
