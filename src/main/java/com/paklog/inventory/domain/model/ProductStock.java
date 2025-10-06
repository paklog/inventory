package com.paklog.inventory.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.paklog.inventory.domain.event.ABCClassificationChangedEvent;
import com.paklog.inventory.domain.event.DomainEvent;
import com.paklog.inventory.domain.event.InventoryHoldPlacedEvent;
import com.paklog.inventory.domain.event.InventoryHoldReleasedEvent;
import com.paklog.inventory.domain.event.InventoryValuationChangedEvent;
import com.paklog.inventory.domain.event.StockLevelChangedEvent;
import com.paklog.inventory.domain.event.StockStatusChangedEvent;
import com.paklog.inventory.domain.exception.InsufficientStockException;
import com.paklog.inventory.domain.exception.InvalidQuantityException;
import com.paklog.inventory.domain.exception.StockLevelInvariantViolationException;

import java.math.BigDecimal;

public class ProductStock {

    private String sku;
    private StockLevel stockLevel;
    private LocalDateTime lastUpdated;
    private Long version; // Optimistic locking version
    private List<LotBatch> lotBatches; // Lot/batch tracking for FEFO

    // Phase 1 enhancements
    private Map<StockStatus, StockStatusQuantity> stockByStatus; // Stock status segregation
    private List<InventoryHold> holds; // Inventory holds/blocks
    private boolean serialTracked; // Flag for serial number tracking
    private List<SerialNumber> serialNumbers; // Serial numbers (if serial tracked)

    // Phase 2 enhancements
    private InventoryValuation valuation; // Cost and valuation tracking
    private ABCClassification abcClassification; // ABC classification

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    // Private constructor for internal use and reconstruction from persistence
    private ProductStock(String sku, StockLevel stockLevel, LocalDateTime lastUpdated) {
        this.sku = sku;
        this.stockLevel = stockLevel;
        this.lastUpdated = lastUpdated;
        this.lotBatches = new ArrayList<>();
        this.stockByStatus = new HashMap<>();
        this.holds = new ArrayList<>();
        this.serialTracked = false;
        this.serialNumbers = new ArrayList<>();
        this.valuation = null; // Initialized when needed
        this.abcClassification = null; // Initialized when needed
        validateInvariants();
    }

    // Factory method for creating new ProductStock instances
    public static ProductStock create(String sku, int initialQuantity) {
        if (initialQuantity < 0) {
            throw new InvalidQuantityException("create", initialQuantity, "Initial quantity cannot be negative");
        }
        ProductStock productStock = new ProductStock(sku, StockLevel.of(initialQuantity, 0), LocalDateTime.now());
        // Initialize with all stock as AVAILABLE
        if (initialQuantity > 0) {
            productStock.stockByStatus.put(StockStatus.AVAILABLE, StockStatusQuantity.of(StockStatus.AVAILABLE, initialQuantity));
        }
        productStock.addEvent(new StockLevelChangedEvent(productStock.getSku(), StockLevel.of(0, 0), productStock.getStockLevel(), "INITIAL_STOCK"));
        return productStock;
    }

    // Factory method for creating ProductStock with lot tracking
    public static ProductStock createWithLotTracking(String sku) {
        ProductStock productStock = new ProductStock(sku, StockLevel.of(0, 0), LocalDateTime.now());
        productStock.lotBatches = new ArrayList<>();
        return productStock;
    }

    // Factory method for creating ProductStock with serial tracking
    public static ProductStock createWithSerialTracking(String sku) {
        ProductStock productStock = new ProductStock(sku, StockLevel.of(0, 0), LocalDateTime.now());
        productStock.serialTracked = true;
        productStock.serialNumbers = new ArrayList<>();
        return productStock;
    }

    // Factory method for loading from persistence
    public static ProductStock load(String sku, int quantityOnHand, int quantityAllocated, LocalDateTime lastUpdated) {
        return new ProductStock(sku, StockLevel.of(quantityOnHand, quantityAllocated), lastUpdated);
    }

    public Long getVersion() {
        return version;
    }

    public String getSku() {
        return sku;
    }

    public List<LotBatch> getLotBatches() {
        return lotBatches != null ? Collections.unmodifiableList(lotBatches) : Collections.emptyList();
    }

    public boolean isLotTracked() {
        return lotBatches != null && !lotBatches.isEmpty();
    }

    public boolean isSerialTracked() {
        return serialTracked;
    }

    public List<SerialNumber> getSerialNumbers() {
        return serialNumbers != null ? Collections.unmodifiableList(serialNumbers) : Collections.emptyList();
    }

    public List<InventoryHold> getHolds() {
        return holds != null ? Collections.unmodifiableList(holds) : Collections.emptyList();
    }

    public Map<StockStatus, StockStatusQuantity> getStockByStatus() {
        return stockByStatus != null ? Collections.unmodifiableMap(stockByStatus) : Collections.emptyMap();
    }

    public Optional<InventoryValuation> getValuation() {
        return Optional.ofNullable(valuation);
    }

    public Optional<ABCClassification> getAbcClassification() {
        return Optional.ofNullable(abcClassification);
    }

    public int getQuantityOnHand() {
        return stockLevel.getQuantityOnHand();
    }

    public int getQuantityAllocated() {
        return stockLevel.getQuantityAllocated();
    }

    public int getQuantityReserved() {
        // Get total quantity under active holds
        return holds != null ? holds.stream()
            .filter(InventoryHold::isActive)
            .mapToInt(InventoryHold::getQuantity)
            .sum() : 0;
    }

    public int getAvailableToPromise() {
        // Enhanced ATP calculation considering status and holds
        int baseATP = stockLevel.getAvailableToPromise();

        // Get quantity in AVAILABLE status
        int availableStatusQty = stockByStatus.getOrDefault(StockStatus.AVAILABLE,
            StockStatusQuantity.of(StockStatus.AVAILABLE, 0)).getQuantity();

        // Subtract active holds
        int totalHolds = getQuantityReserved();

        // ATP is the minimum of base ATP and (available status - holds)
        return Math.min(baseATP, Math.max(0, availableStatusQty - totalHolds));
    }

    public StockLevel getStockLevel() { // Added getter for StockLevel
        return stockLevel;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Allocates a given quantity of stock.
     *
     * @param quantity The quantity to allocate.
     * @throws IllegalArgumentException if quantity is negative or if allocation exceeds available to promise.
     */
    public void allocate(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("allocate", quantity, "Allocation quantity must be positive");
        }
        if (stockLevel.getAvailableToPromise() < quantity) {
            throw new InsufficientStockException(sku, quantity, stockLevel.getAvailableToPromise());
        }

        StockLevel previousStockLevel = this.stockLevel;
        this.stockLevel = stockLevel.withAllocation(quantity);
        this.lastUpdated = LocalDateTime.now();
        validateInvariants();
        addEvent(new StockLevelChangedEvent(sku, previousStockLevel, this.stockLevel, "ALLOCATION"));
    }

    /**
     * Deallocates a given quantity of stock.
     *
     * @param quantity The quantity to deallocate.
     * @throws IllegalArgumentException if quantity is negative or if deallocation exceeds allocated quantity.
     */
    public void deallocate(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("deallocate", quantity, "Deallocation quantity must be positive");
        }
        if (stockLevel.getQuantityAllocated() < quantity) {
            throw new InsufficientStockException(sku, quantity, stockLevel.getQuantityAllocated());
        }

        StockLevel previousStockLevel = this.stockLevel;
        this.stockLevel = stockLevel.withDeallocation(quantity);
        this.lastUpdated = LocalDateTime.now();
        validateInvariants();
        addEvent(new StockLevelChangedEvent(sku, previousStockLevel, this.stockLevel, "DEALLOCATION"));
    }

    /**
     * Adjusts the quantity on hand. Can be positive (stock intake) or negative (damage, loss, pick).
     *
     * @param change The quantity change (positive or negative).
     * @param reason The reason for the adjustment.
     * @throws IllegalArgumentException if the adjustment results in negative quantity on hand.
     */
    public void adjustQuantityOnHand(int change, String reason) {
        if (stockLevel.getQuantityOnHand() + change < 0) {
            throw new InvalidQuantityException("adjust", change, 
                String.format("Would result in negative quantity on hand: %d + %d < 0", 
                             stockLevel.getQuantityOnHand(), change));
        }

        StockLevel previousStockLevel = this.stockLevel;
        this.stockLevel = stockLevel.withQuantityChange(change);
        this.lastUpdated = LocalDateTime.now();
        validateInvariants();
        addEvent(new StockLevelChangedEvent(sku, previousStockLevel, this.stockLevel, reason));
    }

    /**
     * Receives new stock, increasing quantity on hand.
     *
     * @param quantity The quantity of stock received.
     * @throws IllegalArgumentException if quantity is negative.
     */
    public void receiveStock(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("receive", quantity, "Received quantity must be positive");
        }
        adjustQuantityOnHand(quantity, "STOCK_RECEIPT");
    }

    public void increaseQuantityOnHand(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("increase", quantity, "Increased quantity must be positive");
        }
        adjustQuantityOnHand(quantity, "PHYSICAL_STOCK_ADDED");
    }

    public void decreaseQuantityOnHand(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("decrease", quantity, "Decreased quantity must be positive");
        }
        adjustQuantityOnHand(-quantity, "PHYSICAL_STOCK_REMOVED");
    }

    /**
     * Receive stock with lot tracking
     */
    public void receiveLot(LotBatch lot) {
        if (lot == null) {
            throw new IllegalArgumentException("Lot batch cannot be null");
        }
        if (lotBatches == null) {
            lotBatches = new ArrayList<>();
        }

        // Check if lot already exists
        if (lotBatches.stream().anyMatch(l -> l.getLotNumber().equals(lot.getLotNumber()))) {
            throw new IllegalStateException("Lot number already exists: " + lot.getLotNumber());
        }

        lotBatches.add(lot);
        adjustQuantityOnHand(lot.getQuantityOnHand(), "LOT_RECEIVED: " + lot.getLotNumber());
    }

    /**
     * Allocate with FEFO strategy (First Expired First Out)
     */
    public void allocateWithFEFO(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("allocate", quantity, "Allocation quantity must be positive");
        }

        if (!isLotTracked()) {
            // Fall back to regular allocation if no lot tracking
            allocate(quantity);
            return;
        }

        // Sort lots by expiry date (FEFO)
        List<LotBatch> availableLots = lotBatches.stream()
                .filter(LotBatch::canBePicked)
                .sorted((l1, l2) -> l1.getExpiryDate().compareTo(l2.getExpiryDate()))
                .toList();

        int remainingToAllocate = quantity;
        for (LotBatch lot : availableLots) {
            if (remainingToAllocate <= 0) break;

            int allocateFromLot = Math.min(remainingToAllocate, lot.getAvailableQuantity());
            lot.allocate(allocateFromLot);
            remainingToAllocate -= allocateFromLot;
        }

        if (remainingToAllocate > 0) {
            throw new InsufficientStockException(sku, quantity, quantity - remainingToAllocate);
        }

        // Update aggregate stock level
        StockLevel previousStockLevel = this.stockLevel;
        this.stockLevel = stockLevel.withAllocation(quantity);
        this.lastUpdated = LocalDateTime.now();
        validateInvariants();
        addEvent(new StockLevelChangedEvent(sku, previousStockLevel, this.stockLevel, "FEFO_ALLOCATION"));
    }

    /**
     * Get lots near expiry
     */
    public List<LotBatch> getLotsNearExpiry(int daysThreshold) {
        if (lotBatches == null) {
            return Collections.emptyList();
        }
        return lotBatches.stream()
                .filter(lot -> lot.isNearExpiry(daysThreshold))
                .sorted((l1, l2) -> l1.getExpiryDate().compareTo(l2.getExpiryDate()))
                .toList();
    }

    /**
     * Get expired lots
     */
    public List<LotBatch> getExpiredLots() {
        if (lotBatches == null) {
            return Collections.emptyList();
        }
        return lotBatches.stream()
                .filter(LotBatch::isExpired)
                .toList();
    }

    // ========== Stock Status Management ==========

    /**
     * Change stock status for a given quantity
     */
    public void changeStockStatus(int quantity, StockStatus fromStatus, StockStatus toStatus, String reason) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("changeStatus", quantity, "Quantity must be positive");
        }

        // Get current quantity in source status
        StockStatusQuantity fromStatusQty = stockByStatus.get(fromStatus);
        if (fromStatusQty == null || fromStatusQty.getQuantity() < quantity) {
            throw new InsufficientStockException(sku, quantity,
                fromStatusQty != null ? fromStatusQty.getQuantity() : 0);
        }

        // Decrease from status
        stockByStatus.put(fromStatus, fromStatusQty.removeQuantity(fromStatus, quantity));

        // Increase to status
        StockStatusQuantity toStatusQty = stockByStatus.getOrDefault(toStatus,
            StockStatusQuantity.of(toStatus, 0));
        stockByStatus.put(toStatus, toStatusQty.addQuantity(toStatus, quantity));

        // Update timestamp
        this.lastUpdated = LocalDateTime.now();

        // Publish event
        addEvent(new StockStatusChangedEvent(sku, fromStatus, toStatus, quantity, reason, null));
    }

    /**
     * Get quantity for a specific stock status
     */
    public int getQuantityByStatus(StockStatus status) {
        return stockByStatus.getOrDefault(status, StockStatusQuantity.of(status, 0)).getQuantity();
    }

    /**
     * Receive stock with specific status
     */
    public void receiveStockWithStatus(int quantity, StockStatus status) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("receive", quantity, "Received quantity must be positive");
        }

        // Update aggregate stock level
        adjustQuantityOnHand(quantity, "STOCK_RECEIPT_" + status);

        // Update status-specific quantity
        StockStatusQuantity statusQty = stockByStatus.getOrDefault(status,
            StockStatusQuantity.of(status, 0));
        stockByStatus.put(status, statusQty.addQuantity(status, quantity));
    }

    // ========== Inventory Holds Management ==========

    /**
     * Place a hold on inventory
     */
    public void placeHold(HoldType holdType, int quantity, String reason, String placedBy) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("placeHold", quantity, "Hold quantity must be positive");
        }

        // Verify available quantity
        int availableQty = getQuantityByStatus(StockStatus.AVAILABLE);
        int currentlyHeld = holds.stream()
            .filter(InventoryHold::isActive)
            .mapToInt(InventoryHold::getQuantity)
            .sum();

        if (availableQty - currentlyHeld < quantity) {
            throw new InsufficientStockException(sku, quantity, availableQty - currentlyHeld);
        }

        InventoryHold hold = InventoryHold.create(holdType, quantity, reason, placedBy);
        holds.add(hold);
        this.lastUpdated = LocalDateTime.now();

        addEvent(new InventoryHoldPlacedEvent(sku, hold.getHoldId(), holdType, quantity, reason, placedBy));
    }

    /**
     * Release a hold
     */
    public void releaseHold(String holdId, String releasedBy, String releaseReason) {
        InventoryHold hold = holds.stream()
            .filter(h -> h.getHoldId().equals(holdId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + holdId));

        if (!hold.isActive()) {
            throw new IllegalStateException("Hold is not active: " + holdId);
        }

        // Replace with released hold
        holds.remove(hold);
        holds.add(hold.release());
        this.lastUpdated = LocalDateTime.now();

        addEvent(new InventoryHoldReleasedEvent(sku, holdId,
            hold.getQuantity(), releasedBy));
    }

    /**
     * Get total quantity under active holds
     */
    public int getTotalHeldQuantity() {
        return holds.stream()
            .filter(InventoryHold::isActive)
            .mapToInt(InventoryHold::getQuantity)
            .sum();
    }

    // ========== Serial Number Management ==========

    /**
     * Receive a serial-tracked item
     */
    public void receiveSerialNumber(SerialNumber serialNumber) {
        if (!serialTracked) {
            throw new IllegalStateException("SKU " + sku + " is not configured for serial tracking");
        }

        if (!serialNumber.getSku().equals(this.sku)) {
            throw new IllegalArgumentException("Serial number SKU mismatch");
        }

        // Check for duplicate
        if (serialNumbers.stream().anyMatch(s -> s.getSerialNumber().equals(serialNumber.getSerialNumber()))) {
            throw new IllegalStateException("Serial number already exists: " + serialNumber.getSerialNumber());
        }

        serialNumbers.add(serialNumber);
        receiveStock(1); // Increase quantity by 1
    }

    /**
     * Allocate a specific serial number
     */
    public void allocateSerialNumber(String serialNumber, String customerId) {
        if (!serialTracked) {
            throw new IllegalStateException("SKU " + sku + " is not configured for serial tracking");
        }

        SerialNumber serial = serialNumbers.stream()
            .filter(s -> s.getSerialNumber().equals(serialNumber))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Serial number not found: " + serialNumber));

        // Replace with allocated serial (allocate() doesn't take customerId, use ship() for that)
        serialNumbers.remove(serial);
        serialNumbers.add(serial.allocate());

        // Allocate at aggregate level
        allocate(1);
    }

    /**
     * Get available serial numbers
     */
    public List<SerialNumber> getAvailableSerialNumbers() {
        return serialNumbers.stream()
            .filter(s -> s.getStatus().canBeAllocated())
            .toList();
    }

    // ========== Inventory Valuation Management ==========

    /**
     * Initialize valuation for this SKU
     */
    public void initializeValuation(ValuationMethod method, BigDecimal initialUnitCost, String currency) {
        if (valuation != null) {
            throw new IllegalStateException("Valuation already initialized for SKU: " + sku);
        }

        this.valuation = InventoryValuation.create(sku, method, initialUnitCost,
            stockLevel.getQuantityOnHand(), currency);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Update valuation when stock is received
     */
    public void updateValuationOnReceipt(int quantityReceived, BigDecimal unitCostReceived) {
        if (valuation == null) {
            throw new IllegalStateException("Valuation not initialized for SKU: " + sku);
        }

        InventoryValuation previousValuation = this.valuation;
        this.valuation = valuation.adjustForQuantityChange(quantityReceived, unitCostReceived);
        this.lastUpdated = LocalDateTime.now();

        addEvent(new InventoryValuationChangedEvent(
            sku,
            valuation.getValuationMethod(),
            previousValuation.getUnitCost(),
            valuation.getUnitCost(),
            previousValuation.getTotalValue(),
            valuation.getTotalValue(),
            valuation.getQuantity(),
            "STOCK_RECEIPT"
        ));
    }

    /**
     * Update valuation when stock is issued
     */
    public void updateValuationOnIssue(int quantityIssued) {
        if (valuation == null) {
            throw new IllegalStateException("Valuation not initialized for SKU: " + sku);
        }

        InventoryValuation previousValuation = this.valuation;
        BigDecimal cogs = valuation.getCostOfGoods(quantityIssued);

        this.valuation = valuation.adjustForQuantityChange(-quantityIssued, valuation.getUnitCost());
        this.lastUpdated = LocalDateTime.now();

        addEvent(new InventoryValuationChangedEvent(
            sku,
            valuation.getValuationMethod(),
            previousValuation.getUnitCost(),
            valuation.getUnitCost(),
            previousValuation.getTotalValue(),
            valuation.getTotalValue(),
            valuation.getQuantity(),
            String.format("STOCK_ISSUE (COGS: %s)", cogs)
        ));
    }

    /**
     * Get current unit cost
     */
    public Optional<BigDecimal> getUnitCost() {
        return Optional.ofNullable(valuation).map(InventoryValuation::getUnitCost);
    }

    /**
     * Get total inventory value
     */
    public Optional<BigDecimal> getTotalValue() {
        return Optional.ofNullable(valuation).map(InventoryValuation::getTotalValue);
    }

    // ========== ABC Classification Management ==========

    /**
     * Set ABC classification
     */
    public void setAbcClassification(ABCClassification newClassification) {
        if (newClassification == null) {
            throw new IllegalArgumentException("ABC classification cannot be null");
        }

        if (!newClassification.getSku().equals(this.sku)) {
            throw new IllegalArgumentException("ABC classification SKU mismatch");
        }

        ABCClass previousClass = abcClassification != null ? abcClassification.getAbcClass() : null;
        this.abcClassification = newClassification;
        this.lastUpdated = LocalDateTime.now();

        addEvent(new ABCClassificationChangedEvent(
            sku,
            previousClass,
            newClassification.getAbcClass(),
            newClassification.getClassificationCriteria(),
            "ABC_CLASSIFICATION_UPDATE"
        ));
    }

    /**
     * Check if ABC classification needs refresh
     */
    public boolean requiresAbcReclassification() {
        return abcClassification == null || abcClassification.requiresReclassification();
    }

    /**
     * Get current ABC class
     */
    public Optional<ABCClass> getAbcClass() {
        return Optional.ofNullable(abcClassification).map(ABCClassification::getAbcClass);
    }

    /**
     * Get recommended cycle count frequency based on ABC class
     */
    public int getRecommendedCycleCountFrequencyDays() {
        return abcClassification != null ?
            abcClassification.getRecommendedCountFrequency() :
            180; // Default to semi-annual for unclassified items
    }

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        this.uncommittedEvents.clear();
    }

    private void addEvent(DomainEvent event) {
        this.uncommittedEvents.add(event);
    }

    /* package-private */ void validateInvariants() {
        if (stockLevel.getQuantityOnHand() < 0) {
            throw new StockLevelInvariantViolationException(
                "Quantity on hand cannot be negative",
                stockLevel.getQuantityOnHand(),
                stockLevel.getQuantityAllocated());
        }
        if (stockLevel.getQuantityAllocated() < 0) {
            throw new StockLevelInvariantViolationException(
                "Quantity allocated cannot be negative",
                stockLevel.getQuantityOnHand(),
                stockLevel.getQuantityAllocated());
        }
        if (stockLevel.getQuantityAllocated() > stockLevel.getQuantityOnHand()) {
            throw new StockLevelInvariantViolationException(
                "Allocated quantity cannot exceed quantity on hand",
                stockLevel.getQuantityOnHand(),
                stockLevel.getQuantityAllocated());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductStock that = (ProductStock) o;
        return Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }
}