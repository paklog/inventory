package com.paklog.inventory.domain.exception;

/**
 * Exception thrown when attempting to register stock for a product that does not exist
 * in the Product Catalog service.
 *
 * This exception enforces the invariant that inventory can only be tracked for products
 * that have been properly registered in the catalog first.
 */
public class ProductNotFoundInCatalogException extends DomainException {

    private final String sku;

    public ProductNotFoundInCatalogException(String sku) {
        super(String.format("Product with SKU '%s' does not exist in the Product Catalog. " +
                "Please register the product in the catalog before managing its inventory.", sku));
        this.sku = sku;
    }

    public ProductNotFoundInCatalogException(String sku, Throwable cause) {
        super(String.format("Product with SKU '%s' does not exist in the Product Catalog. " +
                "Please register the product in the catalog before managing its inventory.", sku), cause);
        this.sku = sku;
    }

    public String getSku() {
        return sku;
    }
}
