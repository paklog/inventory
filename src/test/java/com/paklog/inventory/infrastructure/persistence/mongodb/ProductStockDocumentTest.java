package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.StockStatus;
import com.paklog.inventory.domain.model.StockStatusQuantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductStockDocument")
class ProductStockDocumentTest {

    @Nested
    @DisplayName("quantityOnHand derivation from stockByStatus")
    class QuantityOnHandDerivation {

        @Test
        @DisplayName("should derive quantityOnHand from single AVAILABLE status")
        void shouldDeriveFromSingleAvailableStatus() {
            Map<String, Integer> stockByStatus = Map.of("AVAILABLE", 938);
            ProductStockDocument doc = new ProductStockDocument(
                    "Bottle-W-021",
                    stockByStatus,
                    0,
                    LocalDateTime.now()
            );

            assertThat(doc.getQuantityOnHand()).isEqualTo(938);
        }

        @Test
        @DisplayName("should derive quantityOnHand from multiple statuses")
        void shouldDeriveFromMultipleStatuses() {
            Map<String, Integer> stockByStatus = Map.of(
                    "AVAILABLE", 900,
                    "DAMAGED", 30,
                    "QUARANTINE", 8
            );
            ProductStockDocument doc = new ProductStockDocument(
                    "Bottle-W-021",
                    stockByStatus,
                    0,
                    LocalDateTime.now()
            );

            assertThat(doc.getQuantityOnHand()).isEqualTo(938); // 900 + 30 + 8
        }

        @Test
        @DisplayName("should return zero when stockByStatus is empty")
        void shouldReturnZeroWhenStockByStatusIsEmpty() {
            ProductStockDocument doc = new ProductStockDocument(
                    "EMPTY-SKU",
                    new HashMap<>(),
                    0,
                    LocalDateTime.now()
            );

            assertThat(doc.getQuantityOnHand()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return zero when stockByStatus is null")
        void shouldReturnZeroWhenStockByStatusIsNull() {
            ProductStockDocument doc = new ProductStockDocument();
            doc.setSku("NULL-SKU");
            doc.setStockByStatus(null);

            assertThat(doc.getQuantityOnHand()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("fromDomain conversion")
    class FromDomainConversion {

        @Test
        @DisplayName("should convert domain with stockByStatus to document")
        void shouldConvertDomainWithStockByStatus() {
            ProductStock domain = ProductStock.create("SKU-001", 100);

            ProductStockDocument doc = ProductStockDocument.fromDomain(domain);

            assertThat(doc.getSku()).isEqualTo("SKU-001");
            assertThat(doc.getQuantityOnHand()).isEqualTo(100);
            assertThat(doc.getQuantityAllocated()).isEqualTo(0);
            assertThat(doc.getStockByStatus()).containsEntry("AVAILABLE", 100);
        }

        @Test
        @DisplayName("should ensure stockByStatus is populated even if domain has empty status map")
        void shouldPopulateStockByStatusWhenDomainIsEmpty() {
            // Load a domain object with empty stockByStatus
            ProductStock domain = ProductStock.load("SKU-002", 500, 0, LocalDateTime.now());

            ProductStockDocument doc = ProductStockDocument.fromDomain(domain);

            // Should default to AVAILABLE
            assertThat(doc.getStockByStatus()).containsEntry("AVAILABLE", 500);
            assertThat(doc.getQuantityOnHand()).isEqualTo(500);
        }

        @Test
        @DisplayName("should handle zero quantity correctly")
        void shouldHandleZeroQuantity() {
            ProductStock domain = ProductStock.create("EMPTY-SKU", 0);

            ProductStockDocument doc = ProductStockDocument.fromDomain(domain);

            assertThat(doc.getQuantityOnHand()).isEqualTo(0);
            assertThat(doc.getStockByStatus()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toDomain conversion")
    class ToDomainConversion {

        @Test
        @DisplayName("should convert document with stockByStatus to domain")
        void shouldConvertDocumentWithStockByStatus() {
            Map<String, Integer> stockByStatus = Map.of("AVAILABLE", 938);
            ProductStockDocument doc = new ProductStockDocument(
                    "Bottle-W-021",
                    stockByStatus,
                    0,
                    LocalDateTime.now()
            );

            ProductStock domain = doc.toDomain();

            assertThat(domain.getSku()).isEqualTo("Bottle-W-021");
            assertThat(domain.getQuantityOnHand()).isEqualTo(938);
            assertThat(domain.getQuantityAllocated()).isEqualTo(0);
            assertThat(domain.getAvailableToPromise()).isEqualTo(938);
            assertThat(domain.getQuantityByStatus(StockStatus.AVAILABLE)).isEqualTo(938);
        }

        @Test
        @DisplayName("should convert document with multiple statuses to domain")
        void shouldConvertDocumentWithMultipleStatuses() {
            Map<String, Integer> stockByStatus = new HashMap<>();
            stockByStatus.put("AVAILABLE", 900);
            stockByStatus.put("DAMAGED", 30);
            stockByStatus.put("QUARANTINE", 8);

            ProductStockDocument doc = new ProductStockDocument(
                    "MULTI-STATUS",
                    stockByStatus,
                    50,
                    LocalDateTime.now()
            );

            ProductStock domain = doc.toDomain();

            assertThat(domain.getQuantityOnHand()).isEqualTo(938); // 900 + 30 + 8
            assertThat(domain.getQuantityAllocated()).isEqualTo(50);
            // ATP = min(baseATP, availableStatusQty - holds) = min(938-50, 900-0) = min(888, 900) = 888
            assertThat(domain.getAvailableToPromise()).isEqualTo(888);
            assertThat(domain.getQuantityByStatus(StockStatus.AVAILABLE)).isEqualTo(900);
            assertThat(domain.getQuantityByStatus(StockStatus.DAMAGED)).isEqualTo(30);
            assertThat(domain.getQuantityByStatus(StockStatus.QUARANTINE)).isEqualTo(8);
        }

        @Test
        @DisplayName("should handle empty stockByStatus correctly")
        void shouldHandleEmptyStockByStatus() {
            ProductStockDocument doc = new ProductStockDocument(
                    "EMPTY-STATUS",
                    new HashMap<>(),
                    0,
                    LocalDateTime.now()
            );

            ProductStock domain = doc.toDomain();

            assertThat(domain.getQuantityOnHand()).isEqualTo(0);
            assertThat(domain.getAvailableToPromise()).isEqualTo(0);
        }

        @Test
        @DisplayName("should preserve version for optimistic locking")
        void shouldPreserveVersion() {
            Map<String, Integer> stockByStatus = Map.of("AVAILABLE", 100);
            ProductStockDocument doc = new ProductStockDocument(
                    "VERSIONED",
                    stockByStatus,
                    0,
                    LocalDateTime.now()
            );
            doc.setVersion(10L);

            ProductStock domain = doc.toDomain();

            assertThat(domain.getVersion()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("round-trip conversion")
    class RoundTripConversion {

        @Test
        @DisplayName("should maintain consistency after domain -> document -> domain")
        void shouldMaintainConsistencyAfterRoundTrip() {
            ProductStock original = ProductStock.create("ROUND-TRIP", 500);

            ProductStockDocument doc = ProductStockDocument.fromDomain(original);
            ProductStock reconstructed = doc.toDomain();

            assertThat(reconstructed.getSku()).isEqualTo(original.getSku());
            assertThat(reconstructed.getQuantityOnHand()).isEqualTo(original.getQuantityOnHand());
            assertThat(reconstructed.getQuantityAllocated()).isEqualTo(original.getQuantityAllocated());
            assertThat(reconstructed.getAvailableToPromise()).isEqualTo(original.getAvailableToPromise());
            assertThat(reconstructed.getQuantityByStatus(StockStatus.AVAILABLE))
                    .isEqualTo(original.getQuantityByStatus(StockStatus.AVAILABLE));
        }

        @Test
        @DisplayName("should maintain ATP consistency after round trip")
        void shouldMaintainATPConsistencyAfterRoundTrip() {
            ProductStock original = ProductStock.create("ATP-TEST", 1000);
            original.allocate(200); // ATP should be 800

            ProductStockDocument doc = ProductStockDocument.fromDomain(original);
            ProductStock reconstructed = doc.toDomain();

            assertThat(reconstructed.getQuantityOnHand()).isEqualTo(1000);
            assertThat(reconstructed.getQuantityAllocated()).isEqualTo(200);
            assertThat(reconstructed.getAvailableToPromise()).isEqualTo(800);
        }
    }

    @Nested
    @DisplayName("stockByStatus as source of truth")
    class StockByStatusSourceOfTruth {

        @Test
        @DisplayName("should not allow inconsistent data")
        void shouldNotAllowInconsistentData() {
            // Old problem: stockByStatus empty but quantityOnHand = 938
            // This would cause ATP = 0
            // New behavior: stockByStatus is source of truth
            Map<String, Integer> stockByStatus = Map.of("AVAILABLE", 938);
            ProductStockDocument doc = new ProductStockDocument(
                    "CONSISTENT",
                    stockByStatus,
                    0,
                    LocalDateTime.now()
            );

            ProductStock domain = doc.toDomain();

            // ATP should be 938, not 0
            assertThat(domain.getAvailableToPromise()).isEqualTo(938);
        }

        @Test
        @DisplayName("should calculate correct ATP with mixed statuses and allocation")
        void shouldCalculateCorrectATPWithMixedStatusesAndAllocation() {
            Map<String, Integer> stockByStatus = new HashMap<>();
            stockByStatus.put("AVAILABLE", 800);
            stockByStatus.put("DAMAGED", 100);
            stockByStatus.put("RESERVED", 100);

            ProductStockDocument doc = new ProductStockDocument(
                    "MIXED-STATUS",
                    stockByStatus,
                    100, // 100 allocated from AVAILABLE
                    LocalDateTime.now()
            );

            ProductStock domain = doc.toDomain();

            // quantityOnHand = 800 + 100 + 100 = 1000
            // baseATP = 1000 - 100 = 900
            // availableStatusQty = 800
            // totalHolds = 0
            // ATP = min(900, max(0, 800 - 0)) = min(900, 800) = 800
            assertThat(domain.getQuantityOnHand()).isEqualTo(1000);
            assertThat(domain.getAvailableToPromise()).isEqualTo(800);
        }
    }
}
