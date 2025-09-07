package com.paklog.inventory.application.dto;

public class InventoryAllocationRequestedData {
    private String sku;
    private int quantity;
    private String orderId;

    // Getters and Setters (required for JSON deserialization)
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

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}