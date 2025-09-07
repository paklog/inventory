package com.paklog.inventory.domain.exception;

public class InsufficientStockException extends DomainException {
    
    private final String sku;
    private final int requestedQuantity;
    private final int availableQuantity;
    
    public InsufficientStockException(String sku, int requestedQuantity, int availableQuantity) {
        super(String.format("Insufficient stock for SKU '%s': requested %d but only %d available", 
              sku, requestedQuantity, availableQuantity));
        this.sku = sku;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }
    
    public String getSku() {
        return sku;
    }
    
    public int getRequestedQuantity() {
        return requestedQuantity;
    }
    
    public int getAvailableQuantity() {
        return availableQuantity;
    }
}