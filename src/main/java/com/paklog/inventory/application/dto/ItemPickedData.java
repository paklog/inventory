package com.paklog.inventory.application.dto;

public class ItemPickedData {
    private String sku;
    private int quantityPicked;
    private String orderId;

    // Getters and Setters (required for JSON deserialization)
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantityPicked() {
        return quantityPicked;
    }

    public void setQuantityPicked(int quantityPicked) {
        this.quantityPicked = quantityPicked;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}