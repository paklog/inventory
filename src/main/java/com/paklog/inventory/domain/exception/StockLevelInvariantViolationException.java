package com.paklog.inventory.domain.exception;

public class StockLevelInvariantViolationException extends DomainException {
    
    private final String invariantRule;
    private final int quantityOnHand;
    private final int quantityAllocated;
    
    public StockLevelInvariantViolationException(String invariantRule, int quantityOnHand, int quantityAllocated) {
        super(String.format("Stock level invariant violation - %s: quantityOnHand=%d, quantityAllocated=%d", 
              invariantRule, quantityOnHand, quantityAllocated));
        this.invariantRule = invariantRule;
        this.quantityOnHand = quantityOnHand;
        this.quantityAllocated = quantityAllocated;
    }
    
    public String getInvariantRule() {
        return invariantRule;
    }
    
    public int getQuantityOnHand() {
        return quantityOnHand;
    }
    
    public int getQuantityAllocated() {
        return quantityAllocated;
    }
}