package com.paklog.inventory.infrastructure.client;

import com.paklog.inventory.infrastructure.client.dto.ProductCatalogResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProductCatalogClientTest {

    private RestTemplate restTemplate;
    private ProductCatalogClient productCatalogClient;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        meterRegistry = new SimpleMeterRegistry();
        productCatalogClient = new ProductCatalogClient(
                restTemplate,
                "http://localhost:8082",
                meterRegistry
        );
    }

    @Test
    void productExists_shouldReturnTrue_whenProductFoundInCatalog() {
        // Given
        String sku = "TEST-SKU-001";
        ProductCatalogResponse mockResponse = new ProductCatalogResponse(sku, "Test Product");
        ResponseEntity<ProductCatalogResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(ProductCatalogResponse.class)))
                .thenReturn(responseEntity);

        // When
        boolean exists = productCatalogClient.productExists(sku);

        // Then
        assertTrue(exists);
        verify(restTemplate).getForEntity(
                eq("http://localhost:8082/products/TEST-SKU-001"),
                eq(ProductCatalogResponse.class)
        );
    }

    @Test
    void productExists_shouldReturnFalse_whenProductNotFoundInCatalog() {
        // Given
        String sku = "NONEXISTENT-SKU";

        when(restTemplate.getForEntity(anyString(), eq(ProductCatalogResponse.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        null,
                        null,
                        null
                ));

        // When
        boolean exists = productCatalogClient.productExists(sku);

        // Then
        assertFalse(exists);
    }

    @Test
    void productExists_shouldReturnTrue_whenCatalogServiceUnavailable() {
        // Given
        String sku = "TEST-SKU-001";

        when(restTemplate.getForEntity(anyString(), eq(ProductCatalogResponse.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // When
        boolean exists = productCatalogClient.productExists(sku);

        // Then
        // Service returns true to allow operations to proceed during outages
        assertTrue(exists);
    }

    @Test
    void getProduct_shouldReturnProduct_whenProductFoundInCatalog() {
        // Given
        String sku = "TEST-SKU-001";
        ProductCatalogResponse mockResponse = new ProductCatalogResponse(sku, "Test Product");
        ResponseEntity<ProductCatalogResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(ProductCatalogResponse.class)))
                .thenReturn(responseEntity);

        // When
        Optional<ProductCatalogResponse> result = productCatalogClient.getProduct(sku);

        // Then
        assertTrue(result.isPresent());
        assertEquals(sku, result.get().getSku());
        assertEquals("Test Product", result.get().getTitle());
    }

    @Test
    void getProduct_shouldReturnEmpty_whenProductNotFoundInCatalog() {
        // Given
        String sku = "NONEXISTENT-SKU";

        when(restTemplate.getForEntity(anyString(), eq(ProductCatalogResponse.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        null,
                        null,
                        null
                ));

        // When
        Optional<ProductCatalogResponse> result = productCatalogClient.getProduct(sku);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void getProduct_shouldReturnEmpty_whenCatalogServiceUnavailable() {
        // Given
        String sku = "TEST-SKU-001";

        when(restTemplate.getForEntity(anyString(), eq(ProductCatalogResponse.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // When
        Optional<ProductCatalogResponse> result = productCatalogClient.getProduct(sku);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void productExists_shouldIncrementSuccessMetric_whenProductFound() {
        // Given
        String sku = "TEST-SKU-001";
        ProductCatalogResponse mockResponse = new ProductCatalogResponse(sku, "Test Product");
        ResponseEntity<ProductCatalogResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(ProductCatalogResponse.class)))
                .thenReturn(responseEntity);

        // When
        productCatalogClient.productExists(sku);

        // Then
        assertEquals(1.0, meterRegistry.counter("product.catalog.lookup.success", "client", "product-catalog").count());
    }

    @Test
    void productExists_shouldIncrementNotFoundMetric_whenProductNotFound() {
        // Given
        String sku = "NONEXISTENT-SKU";

        when(restTemplate.getForEntity(anyString(), eq(ProductCatalogResponse.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        null,
                        null,
                        null
                ));

        // When
        productCatalogClient.productExists(sku);

        // Then
        assertEquals(1.0, meterRegistry.counter("product.catalog.lookup.not_found", "client", "product-catalog").count());
    }

    @Test
    void productExists_shouldIncrementErrorMetric_whenServiceUnavailable() {
        // Given
        String sku = "TEST-SKU-001";

        when(restTemplate.getForEntity(anyString(), eq(ProductCatalogResponse.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // When
        productCatalogClient.productExists(sku);

        // Then
        assertEquals(1.0, meterRegistry.counter("product.catalog.lookup.error", "client", "product-catalog").count());
    }

    @Test
    void productExists_shouldHandleNullResponseBody() {
        // Given
        String sku = "TEST-SKU-001";
        ResponseEntity<ProductCatalogResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(ProductCatalogResponse.class)))
                .thenReturn(responseEntity);

        // When
        boolean exists = productCatalogClient.productExists(sku);

        // Then
        assertTrue(exists); // Still returns true because status is OK
    }

    @Test
    void getProduct_shouldReturnEmpty_whenResponseBodyIsNull() {
        // Given
        String sku = "TEST-SKU-001";
        ResponseEntity<ProductCatalogResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(ProductCatalogResponse.class)))
                .thenReturn(responseEntity);

        // When
        Optional<ProductCatalogResponse> result = productCatalogClient.getProduct(sku);

        // Then
        assertFalse(result.isPresent());
    }
}
