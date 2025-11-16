package com.paklog.inventory.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for batch stock updates.
 * Supports up to 1000 updates in a single request for high-volume operations.
 */
public class BatchStockUpdateRequest {

    @NotNull
    @NotEmpty
    @Size(min = 1, max = 1000, message = "Batch size must be between 1 and 1000 items")
    private List<@Valid StockUpdateItem> updates;

    private String batchId; // Optional batch tracking ID
    private String sourceSystem; // Source system for all updates

    public BatchStockUpdateRequest() {
    }

    public BatchStockUpdateRequest(List<StockUpdateItem> updates) {
        this.updates = updates;
    }

    public List<StockUpdateItem> getUpdates() {
        return updates;
    }

    public void setUpdates(List<StockUpdateItem> updates) {
        this.updates = updates;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    /**
     * Individual stock update item within a batch
     */
    public static class StockUpdateItem {

        @NotEmpty
        private String sku;

        @NotNull
        private UpdateType updateType;

        @NotNull
        @jakarta.validation.constraints.Min(0)
        private Integer quantity;

        @NotEmpty
        private String reasonCode;

        private String comment;
        private String locationId;
        private String sourceTransactionId;

        public StockUpdateItem() {
        }

        public StockUpdateItem(String sku, UpdateType updateType, Integer quantity, String reasonCode) {
            this.sku = sku;
            this.updateType = updateType;
            this.quantity = quantity;
            this.reasonCode = reasonCode;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public UpdateType getUpdateType() {
            return updateType;
        }

        public void setUpdateType(UpdateType updateType) {
            this.updateType = updateType;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getReasonCode() {
            return reasonCode;
        }

        public void setReasonCode(String reasonCode) {
            this.reasonCode = reasonCode;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getLocationId() {
            return locationId;
        }

        public void setLocationId(String locationId) {
            this.locationId = locationId;
        }

        public String getSourceTransactionId() {
            return sourceTransactionId;
        }

        public void setSourceTransactionId(String sourceTransactionId) {
            this.sourceTransactionId = sourceTransactionId;
        }
    }

    /**
     * Type of stock update operation
     */
    public enum UpdateType {
        /** Relative adjustment (increase or decrease) */
        ADJUST,

        /** Absolute set (physical count) */
        SET
    }
}
