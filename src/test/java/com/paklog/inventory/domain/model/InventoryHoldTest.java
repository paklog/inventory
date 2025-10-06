package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InventoryHold - validates hold placement, release, and expiration logic.
 */
class InventoryHoldTest {

    @Test
    @DisplayName("Should create quality hold")
    void shouldCreateQualityHold() {
        // When
        InventoryHold hold = InventoryHold.create(HoldType.QUALITY_HOLD,
            150, "QC inspection required", "USER1");

        // Then
        assertNotNull(hold.getHoldId());
        assertEquals(HoldType.QUALITY_HOLD, hold.getHoldType());
        assertEquals(150, hold.getQuantity());
        assertEquals("QC inspection required", hold.getReason());
        assertEquals("USER1", hold.getPlacedBy());
        assertTrue(hold.isActive());
    }

    @Test
    @DisplayName("Should create hold with expiration date")
    void shouldCreateHoldWithExpirationDate() {
        // Given
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);

        // When
        InventoryHold hold = InventoryHold.createWithExpiry(
            HoldType.CREDIT_HOLD, 200, "Credit hold", "FINANCE", expiryDate);

        // Then
        assertEquals(expiryDate, hold.getExpiresAt());
        assertFalse(hold.isExpired());
    }

    @Test
    @DisplayName("Should release hold")
    void shouldReleaseHold() {
        // Given
        InventoryHold hold = InventoryHold.create(HoldType.QUALITY_HOLD,
            150, "QC", "USER1");

        // When
        InventoryHold released = hold.release();

        // Then
        assertFalse(released.isActive());
        assertTrue(released.isExpired());
        assertNotNull(released.getExpiresAt());
    }

    @Test
    @DisplayName("Should detect expired hold")
    void shouldDetectExpiredHold() {
        // Given: Hold with past expiry date
        LocalDateTime pastExpiry = LocalDateTime.now().minusDays(1);
        InventoryHold hold = InventoryHold.createWithExpiry(
            HoldType.LEGAL_HOLD, 100, "Litigation", "LEGAL", pastExpiry);

        // When
        boolean expired = hold.isExpired();

        // Then
        assertTrue(expired);
    }

    @Test
    @DisplayName("Should not be expired for future expiry date")
    void shouldNotBeExpiredForFutureExpiryDate() {
        // Given: Hold with future expiry
        LocalDateTime futureExpiry = LocalDateTime.now().plusDays(30);
        InventoryHold hold = InventoryHold.createWithExpiry(
            HoldType.RECALL_HOLD, 500, "Recall", "SAFETY", futureExpiry);

        // When
        boolean expired = hold.isExpired();

        // Then
        assertFalse(expired);
    }

    @Test
    @DisplayName("Should create different hold types")
    void shouldCreateDifferentHoldTypes() {
        // When
        InventoryHold qualityHold = InventoryHold.create(
            HoldType.QUALITY_HOLD, 100, "QC", "USER1");
        InventoryHold legalHold = InventoryHold.create(
            HoldType.LEGAL_HOLD, 200, "Legal", "LEGAL");
        InventoryHold creditHold = InventoryHold.create(
            HoldType.CREDIT_HOLD, 150, "Credit", "FINANCE");
        InventoryHold recallHold = InventoryHold.create(
            HoldType.RECALL_HOLD, 500, "Recall", "SAFETY");
        InventoryHold adminHold = InventoryHold.create(
            HoldType.ADMINISTRATIVE_HOLD, 300, "Admin", "ADMIN");

        // Then
        assertEquals(HoldType.QUALITY_HOLD, qualityHold.getHoldType());
        assertEquals(HoldType.LEGAL_HOLD, legalHold.getHoldType());
        assertEquals(HoldType.CREDIT_HOLD, creditHold.getHoldType());
        assertEquals(HoldType.RECALL_HOLD, recallHold.getHoldType());
        assertEquals(HoldType.ADMINISTRATIVE_HOLD, adminHold.getHoldType());
    }

    @Test
    @DisplayName("Should validate hold quantity is positive")
    void shouldValidateHoldQuantityIsPositive() {
        // Given: Valid quantity
        InventoryHold hold = InventoryHold.create(
            HoldType.QUALITY_HOLD, 100, "QC", "USER1");

        // Then
        assertTrue(hold.getQuantity() > 0);
    }
}
