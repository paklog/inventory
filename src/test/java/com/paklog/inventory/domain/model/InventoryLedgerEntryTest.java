package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InventoryLedgerEntryTest {

    private final String sku = "PROD001";
    private final String orderId = "ORDER001";
    private final String receiptId = "RECEIPT001";
    private final String operatorId = "USER001";

    @Test
    @DisplayName("Should create ledger entry for allocation correctly")
    void forAllocation_CreatesCorrectEntry() {
        // Arrange
        int quantity = 10;

        // Act
        InventoryLedgerEntry entry = InventoryLedgerEntry.forAllocation(sku, quantity, orderId);

        // Assert
        assertNotNull(entry.getId());
        assertEquals(sku, entry.getSku());
        assertNotNull(entry.getTimestamp());
        assertEquals(quantity, entry.getQuantityChange());
        assertEquals(ChangeType.ALLOCATION, entry.getChangeType());
        assertEquals(orderId, entry.getSourceReference());
        assertEquals("Order Allocation", entry.getReason());
        assertEquals("System", entry.getOperatorId());
    }

    @Test
    @DisplayName("Should create ledger entry for deallocation correctly")
    void forDeallocation_CreatesCorrectEntry() {
        // Arrange
        int quantity = 5;

        // Act
        InventoryLedgerEntry entry = InventoryLedgerEntry.forDeallocation(sku, quantity, orderId);

        // Assert
        assertNotNull(entry.getId());
        assertEquals(sku, entry.getSku());
        assertNotNull(entry.getTimestamp());
        assertEquals(-quantity, entry.getQuantityChange()); // Negative change for deallocation
        assertEquals(ChangeType.DEALLOCATION, entry.getChangeType());
        assertEquals(orderId, entry.getSourceReference());
        assertEquals("Order Deallocation", entry.getReason());
        assertEquals("System", entry.getOperatorId());
    }

    @Test
    @DisplayName("Should create ledger entry for pick correctly")
    void forPick_CreatesCorrectEntry() {
        // Arrange
        int quantity = 3;

        // Act
        InventoryLedgerEntry entry = InventoryLedgerEntry.forPick(sku, quantity, orderId);

        // Assert
        assertNotNull(entry.getId());
        assertEquals(sku, entry.getSku());
        assertNotNull(entry.getTimestamp());
        assertEquals(-quantity, entry.getQuantityChange()); // Negative change for pick
        assertEquals(ChangeType.PICK, entry.getChangeType());
        assertEquals(orderId, entry.getSourceReference());
        assertEquals("Item Picked", entry.getReason());
        assertEquals("System", entry.getOperatorId());
    }

    @Test
    @DisplayName("Should create ledger entry for positive adjustment correctly")
    void forAdjustment_PositiveChange_CreatesCorrectEntry() {
        // Arrange
        int quantityChange = 20;
        String reason = "Cycle Count Adjustment";

        // Act
        InventoryLedgerEntry entry = InventoryLedgerEntry.forAdjustment(sku, quantityChange, reason, operatorId);

        // Assert
        assertNotNull(entry.getId());
        assertEquals(sku, entry.getSku());
        assertNotNull(entry.getTimestamp());
        assertEquals(quantityChange, entry.getQuantityChange());
        assertEquals(ChangeType.ADJUSTMENT_POSITIVE, entry.getChangeType());
        assertNull(entry.getSourceReference()); // No source reference for general adjustment
        assertEquals(reason, entry.getReason());
        assertEquals(operatorId, entry.getOperatorId());
    }

    @Test
    @DisplayName("Should create ledger entry for negative adjustment correctly")
    void forAdjustment_NegativeChange_CreatesCorrectEntry() {
        // Arrange
        int quantityChange = -15;
        String reason = "Damaged Stock";

        // Act
        InventoryLedgerEntry entry = InventoryLedgerEntry.forAdjustment(sku, quantityChange, reason, operatorId);

        // Assert
        assertNotNull(entry.getId());
        assertEquals(sku, entry.getSku());
        assertNotNull(entry.getTimestamp());
        assertEquals(quantityChange, entry.getQuantityChange());
        assertEquals(ChangeType.ADJUSTMENT_NEGATIVE, entry.getChangeType());
        assertNull(entry.getSourceReference());
        assertEquals(reason, entry.getReason());
        assertEquals(operatorId, entry.getOperatorId());
    }

    @Test
    @DisplayName("Should create ledger entry for receipt correctly")
    void forReceipt_CreatesCorrectEntry() {
        // Arrange
        int quantity = 100;

        // Act
        InventoryLedgerEntry entry = InventoryLedgerEntry.forReceipt(sku, quantity, receiptId);

        // Assert
        assertNotNull(entry.getId());
        assertEquals(sku, entry.getSku());
        assertNotNull(entry.getTimestamp());
        assertEquals(quantity, entry.getQuantityChange());
        assertEquals(ChangeType.RECEIPT, entry.getChangeType());
        assertEquals(receiptId, entry.getSourceReference());
        assertEquals("Stock Receipt", entry.getReason());
        assertEquals("System", entry.getOperatorId());
    }
}