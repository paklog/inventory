package com.paklog.inventory.infrastructure.client;

import com.paklog.inventory.infrastructure.client.dto.ProductCatalogResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Client for integrating with the Product Catalog API.
 * Provides methods to verify product existence and retrieve product information.
 */
@Component
public class ProductCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogClient.class);

    private final RestTemplate restTemplate;
    private final String productCatalogBaseUrl;
    private final Timer productLookupTimer;
    private final Counter productLookupSuccessCounter;
    private final Counter productLookupNotFoundCounter;
    private final Counter productLookupErrorCounter;

    public ProductCatalogClient(
            RestTemplate restTemplate,
            @Value("${product-catalog.base-url}") String productCatalogBaseUrl,
            MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.productCatalogBaseUrl = productCatalogBaseUrl;

        // Initialize metrics
        this.productLookupTimer = Timer.builder("product.catalog.lookup.duration")
                .description("Time taken to lookup product in catalog")
                .tag("client", "product-catalog")
                .register(meterRegistry);

        this.productLookupSuccessCounter = Counter.builder("product.catalog.lookup.success")
                .description("Number of successful product lookups")
                .tag("client", "product-catalog")
                .register(meterRegistry);

        this.productLookupNotFoundCounter = Counter.builder("product.catalog.lookup.not_found")
                .description("Number of product lookups that returned 404")
                .tag("client", "product-catalog")
                .register(meterRegistry);

        this.productLookupErrorCounter = Counter.builder("product.catalog.lookup.error")
                .description("Number of product lookup errors")
                .tag("client", "product-catalog")
                .register(meterRegistry);
    }

    /**
     * Checks if a product exists in the Product Catalog.
     *
     * @param sku the Stock Keeping Unit to check
     * @return true if the product exists, false otherwise
     */
    public boolean productExists(String sku) {
        log.debug("Checking if product exists in catalog: {}", sku);

        Timer.Sample sample = Timer.start();
        try {
            String url = buildProductUrl(sku);
            ResponseEntity<ProductCatalogResponse> response = restTemplate.getForEntity(
                    url,
                    ProductCatalogResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                productLookupSuccessCounter.increment();
                log.debug("Product found in catalog: {}", sku);
                return true;
            }

            log.warn("Unexpected response status for product {}: {}", sku, response.getStatusCode());
            return false;

        } catch (HttpClientErrorException.NotFound e) {
            productLookupNotFoundCounter.increment();
            log.debug("Product not found in catalog: {}", sku);
            return false;

        } catch (RestClientException e) {
            productLookupErrorCounter.increment();
            log.error("Error calling Product Catalog API for SKU: {}", sku, e);
            // In case of error, we allow the operation to proceed
            // This prevents catalog service outages from blocking inventory operations
            // Consider changing this behavior based on your requirements
            log.warn("Product Catalog API unavailable, allowing operation to proceed for SKU: {}", sku);
            return true;
        } finally {
            sample.stop(productLookupTimer);
        }
    }

    /**
     * Retrieves product details from the Product Catalog.
     *
     * @param sku the Stock Keeping Unit to retrieve
     * @return Optional containing the product if found, empty otherwise
     */
    public Optional<ProductCatalogResponse> getProduct(String sku) {
        log.debug("Retrieving product from catalog: {}", sku);

        Timer.Sample sample = Timer.start();
        try {
            String url = buildProductUrl(sku);
            ResponseEntity<ProductCatalogResponse> response = restTemplate.getForEntity(
                    url,
                    ProductCatalogResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                productLookupSuccessCounter.increment();
                log.debug("Successfully retrieved product from catalog: {}", sku);
                return Optional.of(response.getBody());
            }

            return Optional.empty();

        } catch (HttpClientErrorException.NotFound e) {
            productLookupNotFoundCounter.increment();
            log.debug("Product not found in catalog: {}", sku);
            return Optional.empty();

        } catch (RestClientException e) {
            productLookupErrorCounter.increment();
            log.error("Error calling Product Catalog API for SKU: {}", sku, e);
            return Optional.empty();
        } finally {
            sample.stop(productLookupTimer);
        }
    }

    private String buildProductUrl(String sku) {
        return String.format("%s/products/%s", productCatalogBaseUrl, sku);
    }
}
