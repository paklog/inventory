package com.paklog.inventory.application.validator;

import com.paklog.inventory.domain.exception.ProductNotFoundInCatalogException;
import com.paklog.inventory.infrastructure.client.ProductCatalogClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validator that ensures a product exists in the Product Catalog before
 * allowing inventory operations.
 *
 * This enforces the business rule that inventory can only be managed for
 * products that have been properly registered in the catalog.
 */
@Component
public class ProductExistenceValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductExistenceValidator.class);

    private final ProductCatalogClient productCatalogClient;
    private final boolean validationEnabled;

    public ProductExistenceValidator(
            ProductCatalogClient productCatalogClient,
            @Value("${product-catalog.validation.enabled:true}") boolean validationEnabled) {
        this.productCatalogClient = productCatalogClient;
        this.validationEnabled = validationEnabled;
    }

    /**
     * Validates that a product exists in the Product Catalog.
     *
     * @param sku the Stock Keeping Unit to validate
     * @throws ProductNotFoundInCatalogException if the product does not exist in the catalog
     */
    public void validateProductExists(String sku) {
        if (!validationEnabled) {
            log.debug("Product catalog validation is disabled, skipping validation for SKU: {}", sku);
            return;
        }

        log.debug("Validating product existence in catalog for SKU: {}", sku);

        boolean exists = productCatalogClient.productExists(sku);

        if (!exists) {
            log.warn("Product validation failed: SKU {} not found in Product Catalog", sku);
            throw new ProductNotFoundInCatalogException(sku);
        }

        log.debug("Product validation successful for SKU: {}", sku);
    }

    /**
     * Checks if validation is enabled.
     *
     * @return true if validation is enabled, false otherwise
     */
    public boolean isValidationEnabled() {
        return validationEnabled;
    }
}
