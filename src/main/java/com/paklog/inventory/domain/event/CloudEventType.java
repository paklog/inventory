package com.paklog.inventory.domain.event;

/**
 * Registry of all CloudEvents types following the naming convention:
 * com.paklog.inventory.fulfillment.v1.&lt;aggregate&gt;.&lt;event-name&gt;
 *
 * This ensures type consistency across the system and enables event catalog generation.
 */
public enum CloudEventType {

    // ProductStock aggregate events
    STOCK_LEVEL_CHANGED("com.paklog.inventory.fulfillment.v1.product-stock.level-changed"),
    STOCK_STATUS_CHANGED("com.paklog.inventory.fulfillment.v1.product-stock.status-changed"),

    // InventoryHold aggregate events
    INVENTORY_HOLD_PLACED("com.paklog.inventory.fulfillment.v1.inventory-hold.placed"),
    INVENTORY_HOLD_RELEASED("com.paklog.inventory.fulfillment.v1.inventory-hold.released"),

    // InventoryValuation aggregate events
    INVENTORY_VALUATION_CHANGED("com.paklog.inventory.fulfillment.v1.inventory-valuation.changed"),

    // ABCClassification aggregate events
    ABC_CLASSIFICATION_CHANGED("com.paklog.inventory.fulfillment.v1.abc-classification.changed"),

    // Kit aggregate events
    KIT_CREATED("com.paklog.inventory.fulfillment.v1.kit.created"),
    KIT_ASSEMBLED("com.paklog.inventory.fulfillment.v1.kit.assembled"),
    KIT_DISASSEMBLED("com.paklog.inventory.fulfillment.v1.kit.disassembled"),

    // StockTransfer aggregate events
    STOCK_TRANSFER_INITIATED("com.paklog.inventory.fulfillment.v1.stock-transfer.initiated"),
    STOCK_TRANSFER_COMPLETED("com.paklog.inventory.fulfillment.v1.stock-transfer.completed"),

    // SerialNumber aggregate events
    SERIAL_NUMBER_RECEIVED("com.paklog.inventory.fulfillment.v1.serial-number.received"),
    SERIAL_NUMBER_ALLOCATED("com.paklog.inventory.fulfillment.v1.serial-number.allocated"),
    SERIAL_NUMBER_SHIPPED("com.paklog.inventory.fulfillment.v1.serial-number.shipped"),

    // InventorySnapshot aggregate events
    INVENTORY_SNAPSHOT_CREATED("com.paklog.inventory.fulfillment.v1.inventory-snapshot.created");

    private final String type;

    CloudEventType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /**
     * Parse event type string back to enum
     */
    public static CloudEventType fromType(String type) {
        for (CloudEventType eventType : values()) {
            if (eventType.type.equals(type)) {
                return eventType;
            }
        }
        throw new IllegalArgumentException("Unknown CloudEvent type: " + type);
    }

    /**
     * Extract aggregate name from event type
     * Example: "com.paklog.inventory.fulfillment.v1.product-stock.level-changed" → "product-stock"
     */
    public String getAggregateName() {
        String[] parts = type.split("\\.");
        if (parts.length >= 6) {
            return parts[5]; // Index 5 is the aggregate name
        }
        throw new IllegalStateException("Invalid event type format: " + type);
    }

    /**
     * Extract event name from event type
     * Example: "com.paklog.inventory.fulfillment.v1.product-stock.level-changed" → "level-changed"
     */
    public String getEventName() {
        String[] parts = type.split("\\.");
        if (parts.length >= 7) {
            return parts[6]; // Index 6 is the event name
        }
        throw new IllegalStateException("Invalid event type format: " + type);
    }

    @Override
    public String toString() {
        return type;
    }
}
