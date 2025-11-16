package com.paklog.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paklog.inventory.domain.model.ProductStock;

/**
 * Response DTO representing the current stock level for a specific SKU.
 *
 * This response contains the complete inventory position including:
 * - Physical quantity on hand in the warehouse
 * - Quantity already allocated to pending orders
 * - Available-to-promise quantity for new orders
 *
 * The available_to_promise is calculated as: quantity_on_hand - quantity_allocated
 */
public class StockLevelResponse {

    private String sku;

    @JsonProperty("quantity_on_hand")
    private int quantityOnHand;

    @JsonProperty("quantity_allocated")
    private int quantityAllocated;

    @JsonProperty("available_to_promise")
    private int availableToPromise;

    // Private constructor for factory method
    private StockLevelResponse(String sku, int quantityOnHand, int quantityAllocated, int availableToPromise) {
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.quantityAllocated = quantityAllocated;
        this.availableToPromise = availableToPromise;
    }

    /**
     * Creates a StockLevelResponse from a domain ProductStock aggregate.
     *
     * @param productStock the domain model containing stock information
     * @return a new StockLevelResponse instance
     */
    public static StockLevelResponse fromDomain(ProductStock productStock) {
        return new StockLevelResponse(
                productStock.getSku(),
                productStock.getQuantityOnHand(),
                productStock.getQuantityAllocated(),
                productStock.getAvailableToPromise()
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
}