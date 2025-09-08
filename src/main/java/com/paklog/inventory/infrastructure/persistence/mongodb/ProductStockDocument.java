package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.StockLevel;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "product_stocks")
public class ProductStockDocument {

    @Id
    private String sku;
    private int quantityOnHand;
    private int quantityAllocated;
    private LocalDateTime lastUpdated;

    public ProductStockDocument() {
    }

    public ProductStockDocument(String sku, int quantityOnHand, int quantityAllocated, LocalDateTime lastUpdated) {
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.quantityAllocated = quantityAllocated;
        this.lastUpdated = lastUpdated;
    }

    public static ProductStockDocument fromDomain(ProductStock productStock) {
        return new ProductStockDocument(
                productStock.getSku(),
                productStock.getQuantityOnHand(),
                productStock.getQuantityAllocated(),
                productStock.getLastUpdated()
        );
    }

    public ProductStock toDomain() {
        return ProductStock.load(
                this.sku,
                this.quantityOnHand,
                this.quantityAllocated,
                this.lastUpdated
        );
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public int getQuantityAllocated() {
        return quantityAllocated;
    }

    public void setQuantityAllocated(int quantityAllocated) {
        this.quantityAllocated = quantityAllocated;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
