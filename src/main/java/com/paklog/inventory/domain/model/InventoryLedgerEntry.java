package com.paklog.inventory.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class InventoryLedgerEntry {

    private String id;
    private String sku;
    private LocalDateTime timestamp;
    private int quantityChange;
    private ChangeType changeType;
    private String sourceReference; // e.g., order_id, receipt_id
    private String reason; // e.g., damaged, cycle_count, PICKED
    private String operatorId; // User or system performing the change

    // Private constructor for internal use and reconstruction from persistence
    private InventoryLedgerEntry(String id, String sku, LocalDateTime timestamp, int quantityChange,
                                 ChangeType changeType, String sourceReference, String reason, String operatorId) {
        this.id = id;
        this.sku = sku;
        this.timestamp = timestamp;
        this.quantityChange = quantityChange;
        this.changeType = changeType;
        this.sourceReference = sourceReference;
        this.reason = reason;
        this.operatorId = operatorId;
    }

    // Factory methods for different types of ledger entries
    public static InventoryLedgerEntry forAllocation(String sku, int quantity, String orderId) {
        return new InventoryLedgerEntry(UUID.randomUUID().toString(), sku, LocalDateTime.now(),
                quantity, ChangeType.ALLOCATION, orderId, "Order Allocation", "System");
    }

    public static InventoryLedgerEntry forDeallocation(String sku, int quantity, String orderId) {
        return new InventoryLedgerEntry(UUID.randomUUID().toString(), sku, LocalDateTime.now(),
                -quantity, ChangeType.DEALLOCATION, orderId, "Order Deallocation", "System");
    }

    public static InventoryLedgerEntry forPick(String sku, int quantity, String orderId) {
        return new InventoryLedgerEntry(UUID.randomUUID().toString(), sku, LocalDateTime.now(),
                -quantity, ChangeType.PICK, orderId, "Item Picked", "System");
    }

    public static InventoryLedgerEntry forAdjustment(String sku, int quantityChange, String reason, String operatorId) {
        ChangeType type = quantityChange > 0 ? ChangeType.ADJUSTMENT_POSITIVE : ChangeType.ADJUSTMENT_NEGATIVE;
        return new InventoryLedgerEntry(UUID.randomUUID().toString(), sku, LocalDateTime.now(),
                quantityChange, type, null, reason, operatorId);
    }

    public static InventoryLedgerEntry forReceipt(String sku, int quantity, String receiptId) {
        return new InventoryLedgerEntry(UUID.randomUUID().toString(), sku, LocalDateTime.now(),
                quantity, ChangeType.RECEIPT, receiptId, "Stock Receipt", "System");
    }

    public static InventoryLedgerEntry load(String id, String sku, LocalDateTime timestamp, int quantityChange,
                                             ChangeType changeType, String sourceReference, String reason, String operatorId) {
        return new InventoryLedgerEntry(id, sku, timestamp, quantityChange, changeType, sourceReference, reason, operatorId);
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public String getReason() {
        return reason;
    }

    public String getOperatorId() {
        return operatorId;
    }
}