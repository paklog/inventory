package com.paklog.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Individual allocation request item within a bulk request.
 */
public class AllocationRequestItem {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "SKU is required")
    private String sku;

    @Positive(message = "Quantity must be positive")
    private int quantity;

    private Integer priority; // Optional priority (1-10, 1 highest)
    private Boolean useFEFO; // Use FEFO strategy if lot-tracked

    public AllocationRequestItem() {
    }

    public AllocationRequestItem(String orderId, String sku, int quantity) {
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
    }

    public AllocationRequestItem(String orderId, String sku, int quantity, Integer priority, Boolean useFEFO) {
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
        this.priority = priority;
        this.useFEFO = useFEFO;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getUseFEFO() {
        return useFEFO != null ? useFEFO : false;
    }

    public void setUseFEFO(Boolean useFEFO) {
        this.useFEFO = useFEFO;
    }

    @Override
    public String toString() {
        return "AllocationRequestItem{" +
                "orderId='" + orderId + '\'' +
                ", sku='" + sku + '\'' +
                ", quantity=" + quantity +
                ", priority=" + priority +
                ", useFEFO=" + useFEFO +
                '}';
    }
}
