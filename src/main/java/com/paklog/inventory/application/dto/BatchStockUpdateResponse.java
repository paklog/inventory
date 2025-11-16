package com.paklog.inventory.application.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for batch stock updates
 */
public class BatchStockUpdateResponse {

    private int totalRequested;
    private int successfulUpdates;
    private int failedUpdates;
    private long processingTimeMs;
    private List<UpdateResult> results;
    private String batchId;

    public BatchStockUpdateResponse() {
        this.results = new ArrayList<>();
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getSuccessfulUpdates() {
        return successfulUpdates;
    }

    public void setSuccessfulUpdates(int successfulUpdates) {
        this.successfulUpdates = successfulUpdates;
    }

    public int getFailedUpdates() {
        return failedUpdates;
    }

    public void setFailedUpdates(int failedUpdates) {
        this.failedUpdates = failedUpdates;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public List<UpdateResult> getResults() {
        return results;
    }

    public void setResults(List<UpdateResult> results) {
        this.results = results;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    /**
     * Result for individual update within batch
     */
    public static class UpdateResult {
        private String sku;
        private boolean success;
        private String errorMessage;
        private Integer previousQuantity;
        private Integer newQuantity;

        public UpdateResult() {
        }

        public UpdateResult(String sku, boolean success) {
            this.sku = sku;
            this.success = success;
        }

        public static UpdateResult success(String sku, Integer previousQuantity, Integer newQuantity) {
            UpdateResult result = new UpdateResult(sku, true);
            result.setPreviousQuantity(previousQuantity);
            result.setNewQuantity(newQuantity);
            return result;
        }

        public static UpdateResult failure(String sku, String errorMessage) {
            UpdateResult result = new UpdateResult(sku, false);
            result.setErrorMessage(errorMessage);
            return result;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public Integer getPreviousQuantity() {
            return previousQuantity;
        }

        public void setPreviousQuantity(Integer previousQuantity) {
            this.previousQuantity = previousQuantity;
        }

        public Integer getNewQuantity() {
            return newQuantity;
        }

        public void setNewQuantity(Integer newQuantity) {
            this.newQuantity = newQuantity;
        }
    }
}
