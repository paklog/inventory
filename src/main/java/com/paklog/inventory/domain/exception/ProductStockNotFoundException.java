package com.paklog.inventory.domain.exception;

public class ProductStockNotFoundException extends DomainException {
    
    private final String sku;
    
    public ProductStockNotFoundException(String sku) {
        super(String.format("ProductStock not found for SKU: %s", sku));
        this.sku = sku;
    }
    
    public String getSku() {
        return sku;
    }
}