# Product Catalog Integration

## Overview

The Inventory Service now validates that products exist in the Product Catalog API before accepting stock registration. This ensures referential integrity between the Product Catalog and Inventory services.

## Architecture

### Components

1. **ProductCatalogClient** (`infrastructure/client/ProductCatalogClient.java`)
   - REST client for integrating with the Product Catalog API
   - Implements retry logic and circuit breaker patterns via RestTemplate
   - Provides metrics for monitoring catalog API health
   - Gracefully handles catalog service outages (returns `true` to allow operations to proceed)

2. **ProductExistenceValidator** (`application/validator/ProductExistenceValidator.java`)
   - Validates that a product exists in the catalog before creating inventory records
   - Can be enabled/disabled via configuration
   - Throws `ProductNotFoundInCatalogException` if product doesn't exist

3. **ProductNotFoundInCatalogException** (`domain/exception/ProductNotFoundInCatalogException.java`)
   - Domain exception thrown when attempting to register stock for non-existent products
   - Extends `DomainException` for consistent error handling

### Integration Points

The validator is integrated at the following points in `InventoryCommandService`:

- **`receiveStock()`** - When creating new product stock records
- **`increaseQuantityOnHand()`** - When creating new product stock records

The validator is **NOT** called for existing products (already in inventory), only when creating new stock entries.

## Configuration

### Required Properties

```properties
# Product Catalog Integration
product-catalog.base-url=http://localhost:8082
product-catalog.validation.enabled=true
product-catalog.connection.timeout=5000
product-catalog.read.timeout=10000
```

### Configuration Options

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `product-catalog.base-url` | Base URL of the Product Catalog API | `http://localhost:8082` | Yes |
| `product-catalog.validation.enabled` | Enable/disable product validation | `true` | No |
| `product-catalog.connection.timeout` | Connection timeout in milliseconds | `5000` | No |
| `product-catalog.read.timeout` | Read timeout in milliseconds | `10000` | No |

## API Contract

The integration expects the Product Catalog API to implement the following endpoint:

```
GET /products/{sku}
```

**Response Codes:**
- `200 OK` - Product exists
- `404 Not Found` - Product does not exist
- `5xx` - Server error (validation passes to allow operations)

**Response Body:**
```json
{
  "sku": "EXAMPLE-SKU-123",
  "title": "Industrial Grade Widget",
  "dimensions": { ... },
  "attributes": { ... }
}
```

See `product_catalog.yaml` for the complete API specification.

## Usage

### Normal Flow

```java
// When receiving stock for a new product
ProductStock stock = inventoryCommandService.receiveStock("NEW-SKU", 100, "RECEIPT-001");
// ProductCatalogClient is called to verify SKU exists
// If product exists in catalog, stock record is created
// If product doesn't exist, ProductNotFoundInCatalogException is thrown
```

### Error Handling

```java
try {
    inventoryCommandService.receiveStock("INVALID-SKU", 100, "RECEIPT-001");
} catch (ProductNotFoundInCatalogException e) {
    // Handle case where product doesn't exist in catalog
    logger.error("Cannot register stock for SKU {}: {}",
        e.getSku(), e.getMessage());
}
```

### Disabling Validation

For testing or emergency scenarios, validation can be disabled:

```properties
product-catalog.validation.enabled=false
```

When disabled, stock can be registered for any SKU without catalog validation.

## Observability

### Metrics

The `ProductCatalogClient` exposes the following Micrometer metrics:

| Metric | Type | Description |
|--------|------|-------------|
| `product.catalog.lookup.duration` | Timer | Time taken for catalog lookups |
| `product.catalog.lookup.success` | Counter | Number of successful lookups (200 OK) |
| `product.catalog.lookup.not_found` | Counter | Number of 404 responses |
| `product.catalog.lookup.error` | Counter | Number of errors (network, timeout, etc.) |

All metrics are tagged with `client=product-catalog`.

### Logging

The integration produces logs at the following levels:

- **DEBUG**: Detailed flow of validation checks
- **INFO**: Successful validations
- **WARN**: Product not found, catalog service unavailable
- **ERROR**: Unexpected errors during catalog communication

## Resilience

### Catalog Service Outages

When the Product Catalog API is unavailable:
- The `ProductCatalogClient` catches `RestClientException`
- Returns `true` to allow operations to proceed
- Increments the `product.catalog.lookup.error` counter
- Logs a warning

This behavior prevents catalog service outages from blocking critical inventory operations.

### Timeouts

- **Connection timeout**: 5 seconds (configurable)
- **Read timeout**: 10 seconds (configurable)

If timeouts occur frequently, consider:
1. Increasing timeout values
2. Investigating catalog service performance
3. Implementing caching for frequently accessed products

## Testing

### Unit Tests

- `ProductCatalogClientTest` - Tests client behavior with mocked RestTemplate
- `ProductExistenceValidatorTest` - Tests validator logic
- `ProductNotFoundInCatalogExceptionTest` - Tests exception behavior

### Integration Tests

- `InventoryCommandServiceProductValidationIntegrationTest` - Tests end-to-end validation flow

Run tests:
```bash
mvn test -Dtest="ProductCatalogClientTest,ProductExistenceValidatorTest,ProductNotFoundInCatalogExceptionTest,InventoryCommandServiceProductValidationIntegrationTest"
```

## Migration Guide

### Existing Deployments

For existing deployments with inventory data:

1. **Enable validation** in configuration:
   ```properties
   product-catalog.validation.enabled=true
   ```

2. **Ensure all existing SKUs exist in Product Catalog**
   - Run a reconciliation script to verify all inventory SKUs exist in catalog
   - Create missing products in catalog or clean up orphaned inventory records

3. **Monitor metrics** after deployment:
   - Watch `product.catalog.lookup.error` for integration issues
   - Check `product.catalog.lookup.not_found` for missing products

### New Deployments

For new deployments, the validation is enabled by default. Ensure:
1. Product Catalog service is running and accessible
2. `product-catalog.base-url` points to the correct endpoint
3. Network connectivity exists between services

## Troubleshooting

### Problem: Stock registration fails with ProductNotFoundInCatalogException

**Solution:** Ensure the product exists in the Product Catalog API before registering inventory:
1. Verify product SKU in catalog: `GET /products/{sku}`
2. Create product in catalog if missing
3. Retry inventory operation

### Problem: High catalog lookup errors

**Solution:**
1. Check Product Catalog service health
2. Verify network connectivity
3. Review timeout settings
4. Consider implementing circuit breaker pattern

### Problem: Need to bypass validation temporarily

**Solution:** Disable validation in configuration:
```properties
product-catalog.validation.enabled=false
```

**Warning:** This should only be used in emergencies and re-enabled once the catalog service is healthy.

## Future Enhancements

Potential improvements for this integration:

1. **Caching** - Cache product existence checks to reduce API calls
2. **Circuit Breaker** - Implement Resilience4j circuit breaker
3. **Async Validation** - Validate asynchronously and publish events for failures
4. **Bulk Validation** - Support validating multiple SKUs in a single API call
5. **Product Details Caching** - Cache full product details for enrichment
