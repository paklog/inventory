package com.paklog.inventory.application.dto;

/**
 * Result of a single allocation attempt.
 */
public class AllocationResult {

    private final String orderId;
    private final String sku;
    private final boolean success;
    private final String errorMessage;
    private final Integer allocatedQuantity;

    private AllocationResult(String orderId, String sku, boolean success,
                            String errorMessage, Integer allocatedQuantity) {
        this.orderId = orderId;
        this.sku = sku;
        this.success = success;
        this.errorMessage = errorMessage;
        this.allocatedQuantity = allocatedQuantity;
    }

    public static AllocationResult success(String orderId, String sku, int allocatedQuantity) {
        return new AllocationResult(orderId, sku, true, null, allocatedQuantity);
    }

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
