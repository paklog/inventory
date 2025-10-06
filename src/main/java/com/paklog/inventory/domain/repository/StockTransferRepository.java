package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.StockTransfer;
import com.paklog.inventory.domain.model.TransferStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for StockTransfer aggregate.
 * Part of the Inventory bounded context.
 */
public interface StockTransferRepository {

    /**
     * Save a stock transfer
     */
    StockTransfer save(StockTransfer transfer);

    /**
     * Find by transfer ID
     */
    Optional<StockTransfer> findById(String transferId);

    /**
     * Find all transfers for a SKU
     */
    List<StockTransfer> findBySku(String sku);

    /**
     * Find transfers by status
     */
    List<StockTransfer> findByStatus(TransferStatus status);

    /**
     * Find transfers by SKU and status
     */
    List<StockTransfer> findBySkuAndStatus(String sku, TransferStatus status);

    /**
     * Find in-transit transfers (for inventory visibility)
     */
    List<StockTransfer> findInTransitTransfers();

    /**
     * Find transfers initiated within date range
     */
    List<StockTransfer> findByInitiatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find transfers with shrinkage (actualQtyReceived < quantity)
     */
    List<StockTransfer> findTransfersWithShrinkage();

    /**
     * Count transfers by status
     */
    long countByStatus(TransferStatus status);

    /**
     * Delete a transfer
     */
    void delete(StockTransfer transfer);
}
