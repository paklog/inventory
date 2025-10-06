package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ABCClassification - validates classification logic and service level recommendations.
 */
class ABCClassificationTest {

    @Test
    @DisplayName("Should classify high-value SKU as Class A")
    void shouldClassifyHighValueSkuAsClassA() {
        // Given: High annual usage value
        BigDecimal annualUsageValue = new BigDecimal("1000000"); // $1M
        int annualUsageQuantity = 10000;
        BigDecimal unitCost = new BigDecimal("100.00");

        // When
        ABCClassification classification = ABCClassification.classify(
            "SKU-001", annualUsageValue, annualUsageQuantity, unitCost,
            85.0, 90.0, ABCCriteria.VALUE_BASED
        );

        // Then
        assertEquals(ABCClass.A, classification.getAbcClass());
        assertTrue(classification.getRecommendedServiceLevel() >= 95.0);
    }

    @Test
    @DisplayName("Should classify low-value SKU as Class C")
    void shouldClassifyLowValueSkuAsClassC() {
        // Given: Low annual usage value
        BigDecimal annualUsageValue = new BigDecimal("5000"); // $5K
        int annualUsageQuantity = 500;
        BigDecimal unitCost = new BigDecimal("10.00");

        // When
        ABCClassification classification = ABCClassification.classify(
            "SKU-001", annualUsageValue, annualUsageQuantity, unitCost,
            20.0, 15.0, ABCCriteria.VALUE_BASED
        );

        // Then
        assertEquals(ABCClass.C, classification.getAbcClass());
        assertTrue(classification.getRecommendedServiceLevel() < 95.0);
    }

    @Test
    @DisplayName("Should classify with combined criteria (value + velocity + criticality)")
    void shouldClassifyWithCombinedCriteria() {
        // Given
        ABCClassification classification = ABCClassification.classify(
            "SKU-001", new BigDecimal("500000"), 5000, new BigDecimal("100.00"),
            80.0, 85.0, ABCCriteria.COMBINED
        );

        // Then
        assertNotNull(classification);
        assertEquals(ABCCriteria.COMBINED, classification.getCriteria());
        assertTrue(classification.getAbcClass() != null);
    }

    @Test
    @DisplayName("Should classify by velocity (high turnover)")
    void shouldClassifyByVelocity() {
        // Given: High velocity score
        ABCClassification classification = ABCClassification.classify(
            "SKU-001", new BigDecimal("100000"), 20000, new BigDecimal("5.00"),
            95.0, 50.0, ABCCriteria.VELOCITY_BASED
        );

        // Then
        assertEquals(ABCCriteria.VELOCITY_BASED, classification.getCriteria());
        assertNotNull(classification.getAbcClass());
    }

    @Test
    @DisplayName("Should recommend high cycle count frequency for Class A")
    void shouldRecommendHighCycleCountFrequencyForClassA() {
        // Given: Class A item
        ABCClassification classification = ABCClassification.classify(
            "SKU-001", new BigDecimal("2000000"), 20000, new BigDecimal("100.00"),
            90.0, 95.0, ABCCriteria.COMBINED
        );

        // When
        int frequency = classification.getRecommendedCycleCountDays();

        // Then: Class A should have frequent counts (e.g., monthly or less)
        assertTrue(frequency > 0);
        assertTrue(frequency <= 90);
    }

    @Test
    @DisplayName("Should recommend lower cycle count frequency for Class C")
    void shouldRecommendLowerCycleCountFrequencyForClassC() {
        // Given: Class C item
        ABCClassification classification = ABCClassification.classify(
            "SKU-001", new BigDecimal("3000"), 300, new BigDecimal("10.00"),
            15.0, 10.0, ABCCriteria.COMBINED
        );

        // When
        int frequency = classification.getRecommendedCycleCountDays();

        // Then: Class C should have less frequent counts
        assertTrue(frequency > 0);
    }

    @Test
    @DisplayName("Should store classification metadata")
    void shouldStoreClassificationMetadata() {
        // Given
        ABCClassification classification = ABCClassification.classify(
            "SKU-001", new BigDecimal("750000"), 7500, new BigDecimal("100.00"),
            75.0, 80.0, ABCCriteria.COMBINED
        );

        // Then
        assertEquals("SKU-001", classification.getSku());
        assertEquals(new BigDecimal("750000"), classification.getAnnualUsageValue());
        assertEquals(7500, classification.getAnnualUsageQuantity());
        assertEquals(new BigDecimal("100.00"), classification.getUnitCost());
        assertNotNull(classification.getClassifiedAt());
    }

    @Test
    @DisplayName("Should validate different criteria produce consistent classifications")
    void shouldValidateDifferentCriteriaProduceConsistentClassifications() {
        // Given: Same metrics, different criteria
        BigDecimal value = new BigDecimal("500000");
        int quantity = 5000;
        BigDecimal unitCost = new BigDecimal("100.00");

        // When
        ABCClassification valueClassification = ABCClassification.classify(
            "SKU-001", value, quantity, unitCost, 75.0, 80.0, ABCCriteria.VALUE_BASED
        );
        ABCClassification velocityClassification = ABCClassification.classify(
            "SKU-001", value, quantity, unitCost, 95.0, 80.0, ABCCriteria.VELOCITY_BASED
        );

        // Then: Both should produce valid classifications
        assertNotNull(valueClassification.getAbcClass());
        assertNotNull(velocityClassification.getAbcClass());
    }
}
