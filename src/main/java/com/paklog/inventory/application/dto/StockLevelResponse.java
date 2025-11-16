package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.ProductStock;

public class StockLevelResponse {
    private String sku;
    private int quantityOnHand;
    private int quantityAllocated;
    private int availableToPromise;
    private String locationId;

    // Private constructor for factory method
    private StockLevelResponse(String sku, int quantityOnHand, int quantityAllocated, int availableToPromise, String locationId) {
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.quantityAllocated = quantityAllocated;
        this.availableToPromise = availableToPromise;
        this.locationId = locationId;
    }

    public static StockLevelResponse fromDomain(ProductStock productStock) {
        return new StockLevelResponse(
                productStock.getSku(),
                productStock.getQuantityOnHand(),
                productStock.getQuantityAllocated(),
                productStock.getAvailableToPromise(),
                productStock.getLocationId()
        );
    }

    // Getters
    public String getSku() {
        return sku;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityAllocated() {
        return quantityAllocated;
    }

    public int getAvailableToPromise() {
        return availableToPromise;
    }

    public String getLocationId() {
        return locationId;
    }
}