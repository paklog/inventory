package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.HoldType;
import com.paklog.inventory.domain.model.InventoryHold;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for InventoryHoldService - critical for managing inventory availability.
 * Validates hold placement, release, expiration, and ATP impact.
 */
@ExtendWith(MockitoExtension.class)
class InventoryHoldServiceTest {

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private InventoryHoldService inventoryHoldService;

    private String testSku;
    private ProductStock productStock;

    @BeforeEach
    void setUp() {
        testSku = "SKU-TEST-001";
        productStock = ProductStock.create(testSku, 1000);
    }

    @Test
    @DisplayName("Should place quality hold and reduce ATP")
    void shouldPlaceQualityHoldAndReduceATP() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        int initialATP = productStock.getAvailableToPromise();

        // When
        String holdId = inventoryHoldService.placeHold(
            testSku, HoldType.QUALITY_HOLD, 200, "QC inspection required", "QC_SYSTEM"
        );

        // Then
        assertNotNull(holdId);
        verify(productStockRepository).save(productStock);
        verify(outboxRepository).saveAll(anyList());

        // Verify ATP is reduced
        assertEquals(initialATP - 200, productStock.getAvailableToPromise());
        assertEquals(200, productStock.getTotalHeldQuantity());
    }

    @Test
    @DisplayName("Should place legal hold")
    void shouldPlaceLegalHold() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        String holdId = inventoryHoldService.placeHold(
            testSku, HoldType.LEGAL_HOLD, 500, "Litigation pending", "LEGAL_DEPT"
        );

        // Then
        assertNotNull(holdId);
        assertEquals(500, productStock.getAvailableToPromise()); // 1000 - 500
        assertEquals(500, productStock.getTotalHeldQuantity());
    }

    @Test
    @DisplayName("Should place recall hold")
    void shouldPlaceRecallHold() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        String holdId = inventoryHoldService.placeHold(
            testSku, HoldType.RECALL_HOLD, 1000, "Product recall initiated", "SAFETY_TEAM"
        );

        // Then
        assertNotNull(holdId);
        assertEquals(0, productStock.getAvailableToPromise()); // All stock held
        assertEquals(1000, productStock.getTotalHeldQuantity());
    }

    @Test
    @DisplayName("Should place credit hold")
    void shouldPlaceCreditHold() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        String holdId = inventoryHoldService.placeHold(
            testSku, HoldType.CREDIT_HOLD, 300, "Customer credit limit exceeded", "FINANCE"
        );

        // Then
        assertNotNull(holdId);
        assertEquals(700, productStock.getAvailableToPromise());
        assertEquals(300, productStock.getTotalHeldQuantity());
    }

    @Test
    @DisplayName("Should place administrative hold")
    void shouldPlaceAdministrativeHold() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        String holdId = inventoryHoldService.placeHold(
            testSku, HoldType.ADMINISTRATIVE_HOLD, 400, "Administrative review pending", "ADMIN_DEPT"
        );

        // Then
        assertNotNull(holdId);
        assertEquals(600, productStock.getAvailableToPromise());
        assertEquals(400, productStock.getTotalHeldQuantity());
    }

    @Test
    @DisplayName("Should release hold and restore ATP")
    void shouldReleaseHoldAndRestoreATP() {
        // Given: Place a hold first
        productStock.placeHold(HoldType.QUALITY_HOLD, 200, "QC", "USER1");
        productStock.markEventsAsCommitted();

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        int atpBeforeRelease = productStock.getAvailableToPromise();

        // When: Release the hold
        inventoryHoldService.releaseHold(testSku, "HOLD-001", "QC_MANAGER", "QC passed");

        // Then
        verify(productStockRepository).save(productStock);
        verify(outboxRepository).saveAll(anyList());

        // ATP should be restored
        assertEquals(atpBeforeRelease + 200, productStock.getAvailableToPromise());
        assertEquals(0, productStock.getTotalHeldQuantity());
    }

    @Test
    @DisplayName("Should throw exception when placing hold for non-existent SKU")
    void shouldThrowExceptionWhenPlacingHoldForNonExistentSku() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            inventoryHoldService.placeHold(
                testSku, HoldType.QUALITY_HOLD, 100, "Test", "USER1"
            )
        );

        // Verify no save or outbox operations
        verify(productStockRepository, never()).save(any());
        verify(outboxRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should throw exception when releasing hold for non-existent SKU")
    void shouldThrowExceptionWhenReleasingHoldForNonExistentSku() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            inventoryHoldService.releaseHold(testSku, "HOLD-001", "USER1", "Release")
        );

        verify(productStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should place quality hold using convenience method")
    void shouldPlaceQualityHoldUsingConvenienceMethod() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        String holdId = inventoryHoldService.placeQualityHold(
            testSku, 150, "Batch quality check", "QC_DEPT"
        );

        // Then
        assertNotNull(holdId);
        assertEquals(850, productStock.getAvailableToPromise());
        assertEquals(150, productStock.getTotalHeldQuantity());
    }

    @Test
    @DisplayName("Should place legal hold using convenience method")
    void shouldPlaceLegalHoldUsingConvenienceMethod() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        String holdId = inventoryHoldService.placeLegalHold(
            testSku, 250, "Litigation hold", "LEGAL"
        );

        // Then
        assertNotNull(holdId);
        assertEquals(750, productStock.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should place recall hold using convenience method")
    void shouldPlaceRecallHoldUsingConvenienceMethod() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        String holdId = inventoryHoldService.placeRecallHold(
            testSku, 600, "Safety recall", "SAFETY"
        );

        // Then
        assertNotNull(holdId);
        assertEquals(400, productStock.getAvailableToPromise());
        assertEquals(600, productStock.getTotalHeldQuantity());
    }

    @Test
    @DisplayName("Should get all active holds for a SKU")
    void shouldGetAllActiveHoldsForSku() {
        // Given: Multiple holds placed
        productStock.placeHold(HoldType.QUALITY_HOLD, 100, "QC", "USER1");
        productStock.placeHold(HoldType.LEGAL_HOLD, 200, "Legal", "USER2");
        productStock.placeHold(HoldType.CREDIT_HOLD, 150, "Credit", "USER3");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        List<InventoryHold> activeHolds = inventoryHoldService.getActiveHolds(testSku);

        // Then
        assertEquals(3, activeHolds.size());
        assertTrue(activeHolds.stream().allMatch(InventoryHold::isActive));
    }

    @Test
    @DisplayName("Should exclude released holds from active holds list")
    void shouldExcludeReleasedHoldsFromActiveHoldsList() {
        // Given: Place 3 holds, release 1
        productStock.placeHold(HoldType.QUALITY_HOLD, 100, "QC", "USER1");
        productStock.placeHold(HoldType.LEGAL_HOLD, 200, "Legal", "USER2");
        productStock.placeHold(HoldType.CREDIT_HOLD, 150, "Credit", "USER3");
        productStock.releaseHold("HOLD-002", "USER2", "Released");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        List<InventoryHold> activeHolds = inventoryHoldService.getActiveHolds(testSku);

        // Then
        assertEquals(2, activeHolds.size());
        assertFalse(activeHolds.stream().anyMatch(h -> h.getHoldId().equals("HOLD-002")));
    }

    @Test
    @DisplayName("Should get total held quantity across multiple holds")
    void shouldGetTotalHeldQuantityAcrossMultipleHolds() {
        // Given: Multiple holds
        productStock.placeHold(HoldType.QUALITY_HOLD, 100, "QC", "USER1");
        productStock.placeHold(HoldType.LEGAL_HOLD, 200, "Legal", "USER2");
        productStock.placeHold(HoldType.CREDIT_HOLD, 150, "Credit", "USER3");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        int totalHeld = inventoryHoldService.getTotalHeldQuantity(testSku);

        // Then
        assertEquals(450, totalHeld); // 100 + 200 + 150
    }

    @Test
    @DisplayName("Should return zero held quantity when no holds exist")
    void shouldReturnZeroHeldQuantityWhenNoHoldsExist() {
        // Given: No holds
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        int totalHeld = inventoryHoldService.getTotalHeldQuantity(testSku);

        // Then
        assertEquals(0, totalHeld);
    }

    @Test
    @DisplayName("Should release expired holds automatically")
    void shouldReleaseExpiredHoldsAutomatically() {
        // Given: Place holds (Note: expiration date parameter is not supported in current placeHold signature)
        LocalDateTime pastExpiration = LocalDateTime.now().minusDays(1);

        productStock.placeHold(HoldType.QUALITY_HOLD, 100, "QC", "USER1");
        productStock.placeHold(HoldType.LEGAL_HOLD, 200, "Legal", "USER2");
        productStock.placeHold(HoldType.CREDIT_HOLD, 150, "Credit", "USER3");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        int releasedCount = inventoryHoldService.releaseExpiredHolds(testSku, "SYSTEM");

        // Then
        assertEquals(2, releasedCount); // Two expired holds released
        verify(productStockRepository).save(productStock);
        verify(outboxRepository).saveAll(anyList());

        // Only one active hold remaining
        List<InventoryHold> activeHolds = productStock.getHolds().stream()
            .filter(InventoryHold::isActive)
            .toList();
        assertEquals(1, activeHolds.size());
        assertEquals("HOLD-002", activeHolds.get(0).getHoldId());
    }

    @Test
    @DisplayName("Should not save when no expired holds exist")
    void shouldNotSaveWhenNoExpiredHoldsExist() {
        // Given: All holds are active
        productStock.placeHold(HoldType.QUALITY_HOLD, 100, "QC", "USER1");
        productStock.placeHold(HoldType.LEGAL_HOLD, 200, "Legal", "USER2");

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        int releasedCount = inventoryHoldService.releaseExpiredHolds(testSku, "SYSTEM");

        // Then
        assertEquals(0, releasedCount);
        verify(productStockRepository, never()).save(any());
        verify(outboxRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should handle multiple holds and releases correctly")
    void shouldHandleMultipleHoldsAndReleasesCorrectly() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Complex sequence of hold operations
        // 1. Place quality hold for 200
        inventoryHoldService.placeQualityHold(testSku, 200, "Initial QC", "QC1");
        assertEquals(800, productStock.getAvailableToPromise());

        // 2. Place legal hold for 300
        inventoryHoldService.placeLegalHold(testSku, 300, "Litigation", "LEGAL1");
        assertEquals(500, productStock.getAvailableToPromise());

        // 3. Release quality hold
        String qualityHoldId = productStock.getHolds().stream()
            .filter(h -> h.getHoldType() == HoldType.QUALITY_HOLD)
            .findFirst()
            .map(InventoryHold::getHoldId)
            .orElse(null);
        inventoryHoldService.releaseHold(testSku, qualityHoldId, "QC_MANAGER", "QC passed");
        assertEquals(700, productStock.getAvailableToPromise());

        // 4. Place recall hold for 400
        inventoryHoldService.placeRecallHold(testSku, 400, "Recall", "SAFETY");
        assertEquals(300, productStock.getAvailableToPromise());

        // Then: Final state
        assertEquals(700, productStock.getTotalHeldQuantity()); // 300 + 400
        assertEquals(300, productStock.getAvailableToPromise());
    }

    @Test
    @DisplayName("Should emit domain events for hold operations")
    void shouldEmitDomainEventsForHoldOperations() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When: Place hold
        inventoryHoldService.placeHold(
            testSku, HoldType.QUALITY_HOLD, 200, "QC", "USER1"
        );

        // Then: Events should be saved to outbox
        verify(outboxRepository).saveAll(argThat(events ->
            events.size() > 0 && events.stream()
                .anyMatch(e -> e.getEventType().equals("InventoryHoldPlacedEvent"))
        ));
    }

    @Test
    @DisplayName("Should validate insufficient quantity for hold")
    void shouldValidateInsufficientQuantityForHold() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When/Then: Try to hold more than available
        assertThrows(IllegalArgumentException.class, () ->
            inventoryHoldService.placeHold(
                testSku, HoldType.QUALITY_HOLD, 2000, "Too much", "USER1"
            )
        );
    }

    @Test
    @DisplayName("Should validate releasing non-existent hold")
    void shouldValidateReleasingNonExistentHold() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When/Then: Try to release non-existent hold
        assertThrows(IllegalArgumentException.class, () ->
            inventoryHoldService.releaseHold(testSku, "NONEXISTENT-HOLD", "USER1", "Invalid")
        );
    }

    @Test
    @DisplayName("Should maintain correct ATP after multiple hold operations")
    void shouldMaintainCorrectATPAfterMultipleHoldOperations() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        int initialOnHand = productStock.getQuantityOnHand();
        int initialATP = productStock.getAvailableToPromise();

        // When: Multiple operations
        String hold1 = inventoryHoldService.placeQualityHold(testSku, 100, "QC", "USER1");
        String hold2 = inventoryHoldService.placeLegalHold(testSku, 200, "Legal", "USER2");
        inventoryHoldService.releaseHold(testSku, hold1, "USER1", "Released");
        String hold3 = inventoryHoldService.placeRecallHold(testSku, 150, "Recall", "USER3");

        // Then: ATP calculation should be correct
        int expectedHeld = 200 + 150; // hold2 + hold3 (hold1 released)
        int expectedATP = initialOnHand - expectedHeld;
        assertEquals(expectedATP, productStock.getAvailableToPromise());

        // QOH should remain constant
        assertEquals(initialOnHand, productStock.getQuantityOnHand());
    }
}
