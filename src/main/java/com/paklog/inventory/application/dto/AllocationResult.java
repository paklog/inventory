package com.paklog.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of a single allocation attempt within a bulk allocation operation.
 *
 * This immutable DTO represents the outcome of attempting to allocate stock
 * for a specific order and SKU combination. It provides:
 * - Success/failure status
 * - Allocated quantity (on success)
 * - Error message (on failure)
 *
 * Used as part of the BulkAllocationResponse to provide detailed feedback
 * for each allocation request in a batch operation.
 */
public class AllocationResult {

    @JsonProperty("order_id")
    private final String orderId;

    private final String sku;

    private final boolean success;

    @JsonProperty("error_message")
    private final String errorMessage;

    @JsonProperty("allocated_quantity")
    private final Integer allocatedQuantity;

    private AllocationResult(String orderId, String sku, boolean success,
                            String errorMessage, Integer allocatedQuantity) {
        this.orderId = orderId;
        this.sku = sku;
        this.success = success;
        this.errorMessage = errorMessage;
        this.allocatedQuantity = allocatedQuantity;
    }

    /**
     * Creates a successful allocation result.
     *
     * @param orderId the order identifier
     * @param sku the SKU that was allocated
     * @param allocatedQuantity the quantity successfully allocated
     * @return a success result instance
     */
    public static AllocationResult success(String orderId, String sku, int allocatedQuantity) {
        return new AllocationResult(orderId, sku, true, null, allocatedQuantity);
    }

    /**
     * Creates a failed allocation result.
     *
     * @param orderId the order identifier
     * @param sku the SKU that failed to allocate
     * @param errorMessage description of why the allocation failed
     * @return a failure result instance
     */
    public static AllocationResult failure(String orderId, String sku, String errorMessage) {
        return new AllocationResult(orderId, sku, false, errorMessage, null);
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSku() {
        return sku;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getAllocatedQuantity() {
        return allocatedQuantity;
    }

    @Override
    public String toString() {
        return "AllocationResult{" +
                "orderId='" + orderId + '\'' +
                ", sku='" + sku + '\'' +
                ", success=" + success +
                (success ? ", allocatedQuantity=" + allocatedQuantity : ", error='" + errorMessage + "'") +
                '}';
    }
}
