package com.paklog.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for creating a stock reservation (soft allocation).
 *
 * A reservation reduces the available-to-promise (ATP) quantity without
 * physically moving inventory. This is used to reserve stock for specific
 * orders before the actual picking process begins.
 *
 * The reservation:
 * - Decreases ATP immediately
 * - Increases allocated quantity
 * - Does not affect quantity on hand until item is picked
 *
 * Common use cases:
 * - Order confirmation in e-commerce
 * - Pre-allocation for priority customers
 * - Marketplace order processing
 */
public class CreateReservationRequest {

    private String sku;

    private int quantity;

    @JsonProperty("order_id")
    private String orderId;

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
