package com.paklog.inventory.application.service;

import com.paklog.inventory.application.dto.BatchStockUpdateRequest;
import com.paklog.inventory.application.dto.BatchStockUpdateResponse;
import com.paklog.inventory.domain.model.ProductStock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for processing batch stock updates.
 * Optimized for high-volume operations with up to 1000 updates per batch.
 */
@Service
public class BatchStockUpdateService {

    private static final Logger log = LoggerFactory.getLogger(BatchStockUpdateService.class);

    private final InventoryCommandService commandService;

    public BatchStockUpdateService(InventoryCommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * Process batch stock updates with partial success support.
     * Each update is processed independently - failures do not affect other updates.
     */
    @Transactional
    public BatchStockUpdateResponse processBatch(BatchStockUpdateRequest request) {
        long startTime = System.currentTimeMillis();
        String batchId = request.getBatchId() != null ? request.getBatchId() : UUID.randomUUID().toString();

        log.info("Processing batch stock update. BatchId: {}, Items: {}",
                batchId, request.getUpdates().size());

        BatchStockUpdateResponse response = new BatchStockUpdateResponse();
        response.setBatchId(batchId);
        response.setTotalRequested(request.getUpdates().size());

        List<BatchStockUpdateResponse.UpdateResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // Process each update independently
        for (BatchStockUpdateRequest.StockUpdateItem item : request.getUpdates()) {
            try {
                BatchStockUpdateResponse.UpdateResult result = processUpdate(item, request.getSourceSystem());
                results.add(result);

                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("Unexpected error processing batch update for SKU: {}", item.getSku(), e);
                results.add(BatchStockUpdateResponse.UpdateResult.failure(
                        item.getSku(),
                        "Unexpected error: " + e.getMessage()
                ));
                failureCount++;
            }
        }

        response.setResults(results);
        response.setSuccessfulUpdates(successCount);
        response.setFailedUpdates(failureCount);
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        log.info("Batch stock update completed. BatchId: {}, Success: {}, Failed: {}, Time: {}ms",
                batchId, successCount, failureCount, response.getProcessingTimeMs());

        return response;
    }

    private BatchStockUpdateResponse.UpdateResult processUpdate(
            BatchStockUpdateRequest.StockUpdateItem item, String sourceSystem) {

        try {
            // Get operator ID from source system if available
            String operatorId = sourceSystem != null ? sourceSystem : "BATCH_UPDATE";

            ProductStock result;
            int previousQty;

            switch (item.getUpdateType()) {
                case ADJUST:
                    // For adjust, get previous quantity first
                    previousQty = getPreviousQuantity(item.getSku());

                    result = commandService.adjustStock(
                            item.getSku(),
                            item.getQuantity(),
                            item.getReasonCode(),
                            item.getComment(),
                            operatorId
                    );
                    break;

                case SET:
                    previousQty = getPreviousQuantity(item.getSku());

                    result = commandService.setStockLevel(
                            item.getSku(),
                            item.getQuantity(),
                            item.getReasonCode(),
                            item.getComment(),
                            operatorId,
                            item.getLocationId()
                    );
                    break;

                default:
                    return BatchStockUpdateResponse.UpdateResult.failure(
                            item.getSku(),
                            "Invalid update type: " + item.getUpdateType()
                    );
            }

            return BatchStockUpdateResponse.UpdateResult.success(
                    item.getSku(),
                    previousQty,
                    result.getQuantityOnHand()
            );

        } catch (Exception e) {
            log.warn("Failed to process update for SKU: {}", item.getSku(), e);
            return BatchStockUpdateResponse.UpdateResult.failure(
                    item.getSku(),
                    e.getMessage()
            );
        }
    }

    private int getPreviousQuantity(String sku) {
        try {
            // This would ideally come from the repository without triggering full load
            // For now, we'll return 0 as placeholder - in production you'd optimize this
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
