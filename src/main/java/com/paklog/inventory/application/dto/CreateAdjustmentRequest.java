package com.paklog.inventory.application.dto;

/**
 * Request DTO for creating stock adjustments
 */
public class CreateAdjustmentRequest {
    private String sku;
    private int quantityChange;
    private String reasonCode;
    private String comment;

    public CreateAdjustmentRequest() {
    }

    public CreateAdjustmentRequest(String sku, int quantityChange, String reasonCode, String comment) {
        this.sku = sku;
        this.quantityChange = quantityChange;
        this.reasonCode = reasonCode;
        this.comment = comment;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(int quantityChange) {
        this.quantityChange = quantityChange;
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
}
