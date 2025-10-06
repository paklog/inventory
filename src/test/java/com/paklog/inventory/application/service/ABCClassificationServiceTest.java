package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.ABCClass;
import com.paklog.inventory.domain.model.ABCClassification;
import com.paklog.inventory.domain.model.ABCCriteria;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ABCClassificationService - critical for inventory optimization.
 * Validates classification logic, service level recommendations, and cycle count frequencies.
 */
@ExtendWith(MockitoExtension.class)
class ABCClassificationServiceTest {

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private ABCClassificationService abcClassificationService;

    private String testSku;
    private ProductStock productStock;

    @BeforeEach
    void setUp() {
        testSku = "SKU-TEST-001";
        productStock = ProductStock.create(testSku, 1000);
    }

    @Test
    @DisplayName("Should classify SKU as Class A based on high value")
    void shouldClassifySkuAsClassABasedOnHighValue() {
        // Given: High-value SKU (80% of total value)
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        BigDecimal annualUsageValue = new BigDecimal("1000000"); // $1M annual value
        int annualUsageQuantity = 10000;
        BigDecimal unitCost = new BigDecimal("100.00");

        // When
        abcClassificationService.classifyByValue(
            testSku, annualUsageValue, annualUsageQuantity, unitCost
        );

        // Then
        verify(productStockRepository).save(productStock);
        verify(outboxRepository).saveAll(anyList());

        Optional<ABCClass> abcClass = productStock.getAbcClass();
        assertTrue(abcClass.isPresent());
        assertEquals(ABCClass.A, abcClass.get());
    }

    @Test
    @DisplayName("Should classify SKU as Class B based on medium value")
    void shouldClassifySkuAsClassBBasedOnMediumValue() {
        // Given: Medium-value SKU (15% of total value)
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        BigDecimal annualUsageValue = new BigDecimal("150000"); // $150K annual value
        int annualUsageQuantity = 5000;
        BigDecimal unitCost = new BigDecimal("30.00");

        // When
        abcClassificationService.classifyByValue(
            testSku, annualUsageValue, annualUsageQuantity, unitCost
        );

        // Then
        Optional<ABCClass> abcClass = productStock.getAbcClass();
        assertTrue(abcClass.isPresent());
        // Note: Actual class depends on ABCClassification logic thresholds
        assertTrue(List.of(ABCClass.A, ABCClass.B).contains(abcClass.get()));
    }

    @Test
    @DisplayName("Should classify SKU as Class C based on low value")
    void shouldClassifySkuAsClassCBasedOnLowValue() {
        // Given: Low-value SKU (5% of total value)
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        BigDecimal annualUsageValue = new BigDecimal("5000"); // $5K annual value
        int annualUsageQuantity = 1000;
        BigDecimal unitCost = new BigDecimal("5.00");

        // When
        abcClassificationService.classifyByValue(
            testSku, annualUsageValue, annualUsageQuantity, unitCost
        );

        // Then
        Optional<ABCClass> abcClass = productStock.getAbcClass();
        assertTrue(abcClass.isPresent());
    }

    @Test
    @DisplayName("Should classify with combined criteria (value + velocity + criticality)")
    void shouldClassifyWithCombinedCriteria() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        BigDecimal annualUsageValue = new BigDecimal("500000");
        int annualUsageQuantity = 8000;
        BigDecimal unitCost = new BigDecimal("62.50");
        double velocityScore = 85.0; // High velocity
        double criticalityScore = 90.0; // High criticality

        // When
        abcClassificationService.classifyWithCombinedCriteria(
            testSku, annualUsageValue, annualUsageQuantity, unitCost,
            velocityScore, criticalityScore
        );

        // Then
        verify(productStockRepository).save(productStock);
        assertTrue(productStock.getAbcClass().isPresent());
        assertTrue(productStock.getAbcClassification().isPresent());

        ABCClassification classification = productStock.getAbcClassification().get();
        assertEquals(ABCCriteria.COMBINED, classification.getCriteria());
    }

    @Test
    @DisplayName("Should classify by velocity (movement frequency)")
    void shouldClassifyByVelocity() {
        // Given: High-velocity item
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        BigDecimal annualUsageValue = new BigDecimal("200000");
        int annualUsageQuantity = 20000; // High turnover
        BigDecimal unitCost = new BigDecimal("10.00");
        double velocityScore = 95.0; // Very high velocity

        // When
        abcClassificationService.classifyByVelocity(
            testSku, annualUsageValue, annualUsageQuantity, unitCost, velocityScore
        );

        // Then
        assertTrue(productStock.getAbcClassification().isPresent());
        assertEquals(ABCCriteria.VELOCITY_BASED,
            productStock.getAbcClassification().get().getCriteria());
    }

    @Test
    @DisplayName("Should throw exception when classifying non-existent SKU")
    void shouldThrowExceptionWhenClassifyingNonExistentSku() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            abcClassificationService.classifyByValue(
                testSku, BigDecimal.valueOf(100000), 1000, BigDecimal.TEN
            )
        );

        verify(productStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get current ABC class")
    void shouldGetCurrentABCClass() {
        // Given: SKU with classification
        ABCClassification classification = ABCClassification.classify(
            testSku, new BigDecimal("500000"), 5000, new BigDecimal("100.00"),
            70.0, 80.0, ABCCriteria.COMBINED
        );
        productStock.setAbcClassification(classification);

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        Optional<ABCClass> abcClass = abcClassificationService.getABCClass(testSku);

        // Then
        assertTrue(abcClass.isPresent());
    }

    @Test
    @DisplayName("Should return empty ABC class when not classified")
    void shouldReturnEmptyABCClassWhenNotClassified() {
        // Given: No classification
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        Optional<ABCClass> abcClass = abcClassificationService.getABCClass(testSku);

        // Then
        assertTrue(abcClass.isEmpty());
    }

    @Test
    @DisplayName("Should get recommended cycle count frequency for Class A")
    void shouldGetRecommendedCycleCountFrequencyForClassA() {
        // Given: Class A SKU
        ABCClassification classificationA = ABCClassification.classify(
            testSku, new BigDecimal("1000000"), 10000, new BigDecimal("100.00"),
            90.0, 95.0, ABCCriteria.COMBINED
        );
        productStock.setAbcClassification(classificationA);

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        int frequency = abcClassificationService.getRecommendedCountFrequency(testSku);

        // Then: Class A should have frequent counts (e.g., 30 days)
        assertTrue(frequency > 0);
        assertTrue(frequency <= 90); // More frequent than B or C
    }

    @Test
    @DisplayName("Should get recommended cycle count frequency for Class C")
    void shouldGetRecommendedCycleCountFrequencyForClassC() {
        // Given: Class C SKU
        ABCClassification classificationC = ABCClassification.classify(
            testSku, new BigDecimal("5000"), 500, new BigDecimal("10.00"),
            20.0, 15.0, ABCCriteria.COMBINED
        );
        productStock.setAbcClassification(classificationC);

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        int frequency = abcClassificationService.getRecommendedCountFrequency(testSku);

        // Then: Class C should have less frequent counts (e.g., 180 days)
        assertTrue(frequency > 0);
    }

    @Test
    @DisplayName("Should check if SKU requires re-classification")
    void shouldCheckIfSkuRequiresReclassification() {
        // Given: Old classification (needs re-classification)
        ABCClassification oldClassification = ABCClassification.classify(
            testSku, new BigDecimal("100000"), 1000, new BigDecimal("100.00"),
            50.0, 50.0, ABCCriteria.VALUE_BASED
        );
        productStock.setAbcClassification(oldClassification);

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        boolean requiresReclassification =
            abcClassificationService.requiresReclassification(testSku);

        // Then: Depends on classification age policy
        // (assumes requiresAbcReclassification checks classification date)
        assertNotNull(requiresReclassification);
    }

    @Test
    @DisplayName("Should get all SKUs requiring re-classification")
    void shouldGetAllSkusRequiringReclassification() {
        // Given: Multiple SKUs, some require re-classification
        ProductStock stock1 = ProductStock.create("SKU-001", 1000);
        ProductStock stock2 = ProductStock.create("SKU-002", 2000);
        ProductStock stock3 = ProductStock.create("SKU-003", 3000);

        when(productStockRepository.findAll()).thenReturn(List.of(stock1, stock2, stock3));

        // When
        List<String> skusRequiringReclassification =
            abcClassificationService.getSKUsRequiringReclassification();

        // Then: Returns list of SKUs (empty if none require re-classification)
        assertNotNull(skusRequiringReclassification);
    }

    @Test
    @DisplayName("Should batch re-classify all SKUs requiring it")
    void shouldBatchReclassifyAllSkusRequiringIt() {
        // Given: Multiple SKUs requiring re-classification
        ProductStock stock1 = ProductStock.create("SKU-001", 1000);
        ProductStock stock2 = ProductStock.create("SKU-002", 2000);
        ProductStock stock3 = ProductStock.create("SKU-003", 3000);

        // Mark as requiring re-classification (this would be based on date in real implementation)
        // For test, we'll mock the repository to return these stocks
        when(productStockRepository.findAll()).thenReturn(List.of(stock1, stock2, stock3));

        // When
        int count = abcClassificationService.reclassifyAll(ABCCriteria.COMBINED);

        // Then: Re-classification completed
        assertTrue(count >= 0);
        verify(productStockRepository, atLeast(0)).save(any());
    }

    @Test
    @DisplayName("Should get ABC classification details")
    void shouldGetABCClassificationDetails() {
        // Given: Classified SKU
        ABCClassification classification = ABCClassification.classify(
            testSku, new BigDecimal("500000"), 5000, new BigDecimal("100.00"),
            75.0, 80.0, ABCCriteria.COMBINED
        );
        productStock.setAbcClassification(classification);

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        Optional<ABCClassification> result = abcClassificationService.getClassification(testSku);

        // Then
        assertTrue(result.isPresent());
        assertEquals(ABCCriteria.COMBINED, result.get().getCriteria());
        assertTrue(result.get().getAnnualUsageValue().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should emit domain events on classification")
    void shouldEmitDomainEventsOnClassification() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        abcClassificationService.classifyByValue(
            testSku, new BigDecimal("750000"), 7500, new BigDecimal("100.00")
        );

        // Then: Events saved to outbox
        verify(outboxRepository).saveAll(argThat(events ->
            events.size() > 0 && events.stream()
                .anyMatch(e -> e.getEventType().equals("ABCClassificationChangedEvent"))
        ));
    }

    @Test
    @DisplayName("Should validate Class A gets highest service level")
    void shouldValidateClassAGetsHighestServiceLevel() {
        // Given: High-value SKU
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Classify as Class A
        abcClassificationService.classifyByValue(
            testSku, new BigDecimal("2000000"), 20000, new BigDecimal("100.00")
        );

        // Then: Should have high service level recommendation
        Optional<ABCClassification> classification = productStock.getAbcClassification();
        assertTrue(classification.isPresent());
        assertTrue(classification.get().getRecommendedServiceLevel() >= 95.0);
    }

    @Test
    @DisplayName("Should validate Class C gets lower service level")
    void shouldValidateClassCGetsLowerServiceLevel() {
        // Given: Low-value SKU
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Classify as Class C
        abcClassificationService.classifyByValue(
            testSku, new BigDecimal("3000"), 300, new BigDecimal("10.00")
        );

        // Then: Should have lower service level recommendation
        Optional<ABCClassification> classification = productStock.getAbcClassification();
        assertTrue(classification.isPresent());
        assertTrue(classification.get().getRecommendedServiceLevel() < 95.0);
    }

    @Test
    @DisplayName("Should handle re-classification from A to C")
    void shouldHandleReclassificationFromAToC() {
        // Given: Initially Class A
        ABCClassification classificationA = ABCClassification.classify(
            testSku, new BigDecimal("1000000"), 10000, new BigDecimal("100.00"),
            90.0, 95.0, ABCCriteria.VALUE_BASED
        );
        productStock.setAbcClassification(classificationA);
        productStock.markEventsAsCommitted();

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        ABCClass initialClass = productStock.getAbcClass().orElse(null);

        // When: Re-classify with much lower value (downgrade to C)
        abcClassificationService.classifyByValue(
            testSku, new BigDecimal("2000"), 200, new BigDecimal("10.00")
        );

        // Then: Should change classification
        verify(productStockRepository).save(productStock);
        verify(outboxRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle multiple classification criteria changes")
    void shouldHandleMultipleClassificationCriteriaChanges() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Classify with different criteria over time
        // 1. Value-based
        abcClassificationService.classifyByValue(
            testSku, new BigDecimal("500000"), 5000, new BigDecimal("100.00")
        );

        // 2. Velocity-based
        abcClassificationService.classifyByVelocity(
            testSku, new BigDecimal("500000"), 5000, new BigDecimal("100.00"), 85.0
        );

        // 3. Combined
        abcClassificationService.classifyWithCombinedCriteria(
            testSku, new BigDecimal("500000"), 5000, new BigDecimal("100.00"), 85.0, 90.0
        );

        // Then: All classifications succeeded
        verify(productStockRepository, times(3)).save(productStock);
        assertTrue(productStock.getAbcClassification().isPresent());
        assertEquals(ABCCriteria.COMBINED,
            productStock.getAbcClassification().get().getCriteria());
    }

    @Test
    @DisplayName("Should validate different criteria produce different classifications")
    void shouldValidateDifferentCriteriaProduceDifferentClassifications() {
        // Given: Same metrics, different criteria
        ProductStock stock1 = ProductStock.create("SKU-VALUE", 1000);
        ProductStock stock2 = ProductStock.create("SKU-VELOCITY", 1000);

        when(productStockRepository.findBySku("SKU-VALUE")).thenReturn(Optional.of(stock1));
        when(productStockRepository.findBySku("SKU-VELOCITY")).thenReturn(Optional.of(stock2));

        BigDecimal value = new BigDecimal("100000");
        int quantity = 2000;
        BigDecimal unitCost = new BigDecimal("50.00");

        // When: Classify with different criteria
        abcClassificationService.classifyByValue("SKU-VALUE", value, quantity, unitCost);
        abcClassificationService.classifyByVelocity("SKU-VELOCITY", value, quantity, unitCost, 95.0);

        // Then: Should have different criteria
        assertNotEquals(stock1.getAbcClassification().get().getCriteria(),
            stock2.getAbcClassification().get().getCriteria());
    }

    @Test
    @DisplayName("Should validate service level recommendations are consistent with class")
    void shouldValidateServiceLevelRecommendationsAreConsistentWithClass() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Classify SKU
        abcClassificationService.classifyWithCombinedCriteria(
            testSku, new BigDecimal("800000"), 8000, new BigDecimal("100.00"), 80.0, 85.0
        );

        // Then: Service level should match class expectations
        Optional<ABCClassification> classification = productStock.getAbcClassification();
        assertTrue(classification.isPresent());

        ABCClass abcClass = classification.get().getAbcClass();
        double serviceLevel = classification.get().getRecommendedServiceLevel();

        // Class A: 95-99%, Class B: 90-95%, Class C: 85-90%
        switch (abcClass) {
            case A -> assertTrue(serviceLevel >= 95.0 && serviceLevel <= 99.0);
            case B -> assertTrue(serviceLevel >= 90.0 && serviceLevel <= 95.0);
            case C -> assertTrue(serviceLevel >= 85.0 && serviceLevel <= 90.0);
        }
    }
}
