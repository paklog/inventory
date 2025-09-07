package com.paklog.inventory.domain.exception;

public class InvalidQuantityException extends DomainException {
    
    private final int quantity;
    private final String operation;
    
    public InvalidQuantityException(String operation, int quantity) {
        super(String.format("Invalid quantity for %s operation: %d", operation, quantity));
        this.operation = operation;
        this.quantity = quantity;
    }
    
    public InvalidQuantityException(String operation, int quantity, String reason) {
        super(String.format("Invalid quantity for %s operation: %d - %s", operation, quantity, reason));
        this.operation = operation;
        this.quantity = quantity;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public String getOperation() {
        return operation;
    }
}