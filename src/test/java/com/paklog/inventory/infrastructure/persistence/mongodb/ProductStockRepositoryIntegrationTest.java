package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.StockLevel;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ProductStockRepository using Testcontainers.
 */
@DataMongoTest
@Testcontainers
@Import({ProductStockRepositoryImpl.class})
class ProductStockRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ProductStockRepository productStockRepository;

    @BeforeEach
    void setUp() {
        productStockRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        productStockRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save and find product stock by SKU")
    void shouldSaveAndFindProductStockBySku() {
        // Given
        ProductStock productStock = ProductStock.create("SKU-TEST-001", 100);
        productStock.allocate(20);

        // When
        ProductStock saved = productStockRepository.save(productStock);
        Optional<ProductStock> found = productStockRepository.findBySku("SKU-TEST-001");

        // Then
        assertThat(saved).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("SKU-TEST-001");
        assertThat(found.get().getQuantityOnHand()).isEqualTo(100);
        assertThat(found.get().getQuantityAllocated()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should return empty when SKU not found")
    void shouldReturnEmptyWhenSkuNotFound() {
        // When
        Optional<ProductStock> found = productStockRepository.findBySku("NONEXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should save all product stocks")
    void shouldSaveAllProductStocks() {
        // Given
        ProductStock stock1 = ProductStock.create("SKU-001", 100);
        ProductStock stock2 = ProductStock.create("SKU-002", 200);
        ProductStock stock3 = ProductStock.create("SKU-003", 300);

        // When
        List<ProductStock> saved = productStockRepository.saveAll(List.of(stock1, stock2, stock3));

        // Then
        assertThat(saved).hasSize(3);
        assertThat(productStockRepository.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("Should find all product stocks")
    void shouldFindAllProductStocks() {
        // Given
        productStockRepository.save(ProductStock.create("SKU-001", 100));
        productStockRepository.save(ProductStock.create("SKU-002", 200));

        // When
        List<ProductStock> all = productStockRepository.findAll();

        // Then
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("Should find all SKUs")
    void shouldFindAllSkus() {
        // Given
        productStockRepository.save(ProductStock.create("SKU-001", 100));
        productStockRepository.save(ProductStock.create("SKU-002", 200));
        productStockRepository.save(ProductStock.create("SKU-003", 300));

        // When
        List<String> skus = productStockRepository.findAllSkus();

        // Then
        assertThat(skus).hasSize(3);
        assertThat(skus).containsExactlyInAnyOrder("SKU-001", "SKU-002", "SKU-003");
    }

    @Test
    @DisplayName("Should update existing product stock")
    void shouldUpdateExistingProductStock() {
        // Given
        ProductStock productStock = ProductStock.create("SKU-001", 100);
        productStockRepository.save(productStock);

        // When - update stock level
        ProductStock updated = productStockRepository.findBySku("SKU-001").get();
        updated.receiveStock(50);
        productStockRepository.save(updated);

        // Then
        ProductStock found = productStockRepository.findBySku("SKU-001").get();
        assertThat(found.getQuantityOnHand()).isEqualTo(150);
    }

    @Test
    @DisplayName("Should handle optimistic locking with version")
    void shouldHandleOptimisticLocking() {
        // Given
        ProductStock productStock = ProductStock.create("SKU-001", 100);
        ProductStock saved = productStockRepository.save(productStock);

        // When
        ProductStock found1 = productStockRepository.findBySku("SKU-001").get();
        ProductStock found2 = productStockRepository.findBySku("SKU-001").get();

        found1.receiveStock(10);
        productStockRepository.save(found1);

        // Then - second save should have different version
        found2.receiveStock(20);
        ProductStock saved2 = productStockRepository.save(found2);

        // Version should be incremented
        assertThat(saved2.getVersion()).isGreaterThan(saved.getVersion());
    }

    @Test
    @DisplayName("Should delete all product stocks")
    void shouldDeleteAllProductStocks() {
        // Given
        productStockRepository.save(ProductStock.create("SKU-001", 100));
        productStockRepository.save(ProductStock.create("SKU-002", 200));

        // When
        productStockRepository.deleteAll();

        // Then
        assertThat(productStockRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should persist stock level correctly")
    void shouldPersistStockLevelCorrectly() {
        // Given
        ProductStock productStock = ProductStock.create("SKU-001", 150);
        productStock.allocate(30);

        // When
        productStockRepository.save(productStock);
        ProductStock found = productStockRepository.findBySku("SKU-001").get();

        // Then
        assertThat(found.getQuantityOnHand()).isEqualTo(150);
        assertThat(found.getQuantityAllocated()).isEqualTo(30);
        assertThat(found.getAvailableToPromise()).isEqualTo(120); // 150 - 30
    }
}
