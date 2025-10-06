package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.StockTransfer;
import com.paklog.inventory.domain.model.TransferStatus;
import com.paklog.inventory.domain.repository.StockTransferRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Application service for stock transfer management.
 * Handles inter-location transfers and transfer lifecycle.
 */
@Service
public class StockTransferService {

    private static final Logger logger = LoggerFactory.getLogger(StockTransferService.class);

    private final StockTransferRepository stockTransferRepository;
    private final OutboxRepository outboxRepository;

    public StockTransferService(StockTransferRepository stockTransferRepository,
                               OutboxRepository outboxRepository) {
        this.stockTransferRepository = stockTransferRepository;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Initiate a stock transfer
     */
    @Transactional
    public String initiateTransfer(String sku, Location sourceLocation, Location destinationLocation,
                                  int quantity, String initiatedBy, String reason) {
        logger.info("Initiating transfer: SKU={}, from={}, to={}, qty={}, initiatedBy={}",
                sku, sourceLocation.toLocationCode(), destinationLocation.toLocationCode(),
                quantity, initiatedBy);

        StockTransfer transfer = StockTransfer.create(sku, sourceLocation, destinationLocation,
                quantity, initiatedBy, reason);

        StockTransfer saved = stockTransferRepository.save(transfer);
        outboxRepository.saveAll(saved.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        saved.markEventsAsCommitted();

        logger.info("Transfer initiated: transferId={}, SKU={}", saved.getTransferId(), sku);
        return saved.getTransferId();
    }

    /**
     * Mark transfer as in-transit (stock removed from source)
     */
    @Transactional
    public void markInTransit(String transferId) {
        logger.info("Marking transfer in-transit: transferId={}", transferId);

        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        transfer.markInTransit();

        stockTransferRepository.save(transfer);
        outboxRepository.saveAll(transfer.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        transfer.markEventsAsCommitted();

        logger.info("Transfer marked in-transit: transferId={}, SKU={}", transferId, transfer.getSku());
    }

    /**
     * Complete the transfer (stock received at destination)
     */
    @Transactional
    public void completeTransfer(String transferId, int actualQuantityReceived) {
        logger.info("Completing transfer: transferId={}, actualQty={}", transferId, actualQuantityReceived);

        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        transfer.complete(actualQuantityReceived);

        stockTransferRepository.save(transfer);
        outboxRepository.saveAll(transfer.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        transfer.markEventsAsCommitted();

        if (transfer.hasShrinkage()) {
            logger.warn("Transfer completed with shrinkage: transferId={}, SKU={}, shrinkage={}",
                    transferId, transfer.getSku(), transfer.getShrinkageQuantity());
        } else {
            logger.info("Transfer completed successfully: transferId={}, SKU={}",
                    transferId, transfer.getSku());
        }
    }

    /**
     * Cancel a transfer
     */
    @Transactional
    public void cancelTransfer(String transferId, String cancelledBy, String cancellationReason) {
        logger.info("Cancelling transfer: transferId={}, cancelledBy={}, reason={}",
                transferId, cancelledBy, cancellationReason);

        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));

        transfer.cancel(cancelledBy, cancellationReason);

        stockTransferRepository.save(transfer);
        outboxRepository.saveAll(transfer.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        transfer.markEventsAsCommitted();

        logger.info("Transfer cancelled: transferId={}, SKU={}", transferId, transfer.getSku());
    }

    /**
     * Get transfer details
     */
    @Transactional(readOnly = true)
    public StockTransfer getTransfer(String transferId) {
        return stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
    }

    /**
     * Get all transfers for a SKU
     */
    @Transactional(readOnly = true)
    public List<StockTransfer> getTransfersBySKU(String sku) {
        return stockTransferRepository.findBySku(sku);
    }

    /**
     * Get all in-transit transfers
     */
    @Transactional(readOnly = true)
    public List<StockTransfer> getInTransitTransfers() {
        return stockTransferRepository.findInTransitTransfers();
    }

    /**
     * Get transfers by status
     */
    @Transactional(readOnly = true)
    public List<StockTransfer> getTransfersByStatus(TransferStatus status) {
        return stockTransferRepository.findByStatus(status);
    }

    /**
     * Get transfers within date range
     */
    @Transactional(readOnly = true)
    public List<StockTransfer> getTransfersByDateRange(LocalDateTime start, LocalDateTime end) {
        return stockTransferRepository.findByInitiatedAtBetween(start, end);
    }

    /**
     * Get transfers with shrinkage (for analysis)
     */
    @Transactional(readOnly = true)
    public List<StockTransfer> getTransfersWithShrinkage() {
        return stockTransferRepository.findTransfersWithShrinkage();
    }

    /**
     * Count transfers by status
     */
    @Transactional(readOnly = true)
    public long countByStatus(TransferStatus status) {
        return stockTransferRepository.countByStatus(status);
    }
}
