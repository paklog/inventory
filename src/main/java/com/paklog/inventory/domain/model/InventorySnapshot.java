package com.paklog.inventory.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Aggregate root representing a point-in-time snapshot of inventory state.
 * Captures complete inventory state for historical queries and time-travel.
 */
public class InventorySnapshot {

    private final String snapshotId;
    private final LocalDateTime snapshotTimestamp;
    private final SnapshotType snapshotType;
    private final SnapshotReason reason;

    // SKU identification
    private final String sku;

    // Stock levels at snapshot time
    private final int quantityOnHand;
    private final int quantityAllocated;
    private final int quantityReserved;
    private final int quantityAvailable;

    // Stock status breakdown
    private final Map<StockStatus, Integer> stockByStatus;

    // Active holds at snapshot time
    private final List<InventoryHoldSnapshot> activeHolds;

    // Valuation at snapshot time
    private final BigDecimal unitCost;
    private final BigDecimal totalValue;
    private final String valuationMethod;

    // ABC classification at snapshot time
    private final String abcClass;
    private final String abcCriteria;

    // Lot/batch tracking at snapshot time
    private final List<LotBatchSnapshot> lotBatches;

    // Serial numbers at snapshot time (for serial-tracked items)
    private final List<String> serialNumbers;

    // Metadata
    private final String createdBy;
    private final LocalDateTime createdAt;

    // Domain events
    private final List<Object> uncommittedEvents = new ArrayList<>();

    public InventorySnapshot(String snapshotId, LocalDateTime snapshotTimestamp,
                             SnapshotType snapshotType, SnapshotReason reason, String sku,
                             int quantityOnHand, int quantityAllocated, int quantityReserved,
                             int quantityAvailable, Map<StockStatus, Integer> stockByStatus,
                             List<InventoryHoldSnapshot> activeHolds, BigDecimal unitCost,
                             BigDecimal totalValue, String valuationMethod, String abcClass,
                             String abcCriteria, List<LotBatchSnapshot> lotBatches,
                             List<String> serialNumbers, String createdBy) {
        if (snapshotId == null || snapshotId.isBlank()) {
            throw new IllegalArgumentException("Snapshot ID cannot be null or blank");
        }
        if (snapshotTimestamp == null) {
            throw new IllegalArgumentException("Snapshot timestamp cannot be null");
        }
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }

        this.snapshotId = snapshotId;
        this.snapshotTimestamp = snapshotTimestamp;
        this.snapshotType = snapshotType;
        this.reason = reason;
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.quantityAllocated = quantityAllocated;
        this.quantityReserved = quantityReserved;
        this.quantityAvailable = quantityAvailable;
        this.stockByStatus = stockByStatus == null ? new HashMap<>() : new HashMap<>(stockByStatus);
        this.activeHolds = activeHolds == null ? new ArrayList<>() : new ArrayList<>(activeHolds);
        this.unitCost = unitCost;
        this.totalValue = totalValue;
        this.valuationMethod = valuationMethod;
        this.abcClass = abcClass;
        this.abcCriteria = abcCriteria;
        this.lotBatches = lotBatches == null ? new ArrayList<>() : new ArrayList<>(lotBatches);
        this.serialNumbers = serialNumbers == null ? new ArrayList<>() : new ArrayList<>(serialNumbers);
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Create snapshot from ProductStock aggregate
     */
    public static InventorySnapshot fromProductStock(ProductStock productStock,
                                                     LocalDateTime snapshotTimestamp,
                                                     SnapshotType snapshotType,
                                                     SnapshotReason reason,
                                                     String createdBy) {
        String snapshotId = UUID.randomUUID().toString();

        // Extract stock by status
        Map<StockStatus, Integer> stockByStatus = new HashMap<>();
        productStock.getStockByStatus().forEach((status, statusQty) ->
            stockByStatus.put(status, statusQty.getQuantity())
        );

        // Extract active holds
        List<InventoryHoldSnapshot> activeHolds = productStock.getHolds().stream()
            .filter(InventoryHold::isActive)
            .map(InventoryHoldSnapshot::fromInventoryHold)
            .toList();

        // Extract valuation
        BigDecimal unitCost = productStock.getUnitCost().orElse(null);
        BigDecimal totalValue = productStock.getTotalValue().orElse(null);
        String valuationMethod = productStock.getValuation()
            .map(v -> v.getValuationMethod().name())
            .orElse(null);

        // Extract ABC classification
        String abcClass = productStock.getAbcClass().map(ABCClass::name).orElse(null);
        String abcCriteria = productStock.getAbcClassification()
            .map(c -> c.getCriteria().name())
            .orElse(null);

        // Extract lot batches
        List<LotBatchSnapshot> lotBatches = productStock.getLotBatches().stream()
            .map(LotBatchSnapshot::fromLotBatch)
            .toList();

        // Extract serial numbers (if serial tracked)
        List<String> serialNumbers = productStock.isSerialTracked() ?
            productStock.getSerialNumbers().stream()
                .map(SerialNumber::getSerialNumber)
                .toList() : new ArrayList<>();

        return new InventorySnapshot(
            snapshotId, snapshotTimestamp, snapshotType, reason, productStock.getSku(),
            productStock.getQuantityOnHand(), productStock.getQuantityAllocated(),
            productStock.getQuantityReserved(), productStock.getAvailableToPromise(),
            stockByStatus, activeHolds, unitCost, totalValue, valuationMethod,
            abcClass, abcCriteria, lotBatches, serialNumbers, createdBy
        );
    }

    /**
     * Calculate total held quantity
     */
    public int getTotalHeldQuantity() {
        return activeHolds.stream()
            .filter(InventoryHoldSnapshot::isActive)
            .mapToInt(InventoryHoldSnapshot::getQuantity)
            .sum();
    }

    /**
     * Get quantity in specific status
     */
    public int getQuantityInStatus(StockStatus status) {
        return stockByStatus.getOrDefault(status, 0);
    }

    /**
     * Check if snapshot has valuation data
     */
    public boolean hasValuation() {
        return unitCost != null && totalValue != null;
    }

    /**
     * Check if snapshot has ABC classification
     */
    public boolean hasABCClassification() {
        return abcClass != null;
    }

    /**
     * Check if snapshot has lot/batch tracking
     */
    public boolean hasLotTracking() {
        return !lotBatches.isEmpty();
    }

    /**
     * Check if snapshot has serial number tracking
     */
    public boolean hasSerialTracking() {
        return !serialNumbers.isEmpty();
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public LocalDateTime getSnapshotTimestamp() {
        return snapshotTimestamp;
    }

    public SnapshotType getSnapshotType() {
        return snapshotType;
    }

    public SnapshotReason getReason() {
        return reason;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityAllocated() {
        return quantityAllocated;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public Map<StockStatus, Integer> getStockByStatus() {
        return Collections.unmodifiableMap(stockByStatus);
    }

    public List<InventoryHoldSnapshot> getActiveHolds() {
        return Collections.unmodifiableList(activeHolds);
    }

    public Optional<BigDecimal> getUnitCost() {
        return Optional.ofNullable(unitCost);
    }

    public Optional<BigDecimal> getTotalValue() {
        return Optional.ofNullable(totalValue);
    }

    public Optional<String> getValuationMethod() {
        return Optional.ofNullable(valuationMethod);
    }

    public Optional<String> getAbcClass() {
        return Optional.ofNullable(abcClass);
    }

    public Optional<String> getAbcCriteria() {
        return Optional.ofNullable(abcCriteria);
    }

    public List<LotBatchSnapshot> getLotBatches() {
        return Collections.unmodifiableList(lotBatches);
    }

    public List<String> getSerialNumbers() {
        return Collections.unmodifiableList(serialNumbers);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Object> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
}
