package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MongoDB document for InventorySnapshot aggregate.
 * Time-series collection optimized for historical queries.
 */
@Document(collection = "inventory_snapshots")
@CompoundIndexes({
    @CompoundIndex(name = "sku_timestamp_idx", def = "{'sku': 1, 'snapshotTimestamp': -1}"),
    @CompoundIndex(name = "type_timestamp_idx", def = "{'snapshotType': 1, 'snapshotTimestamp': -1}"),
    @CompoundIndex(name = "timestamp_idx", def = "{'snapshotTimestamp': -1}"),
    @CompoundIndex(name = "sku_type_idx", def = "{'sku': 1, 'snapshotType': 1}")
})
public class InventorySnapshotDocument {

    @Id
    private String id;

    @Indexed
    private String snapshotId;
    private LocalDateTime snapshotTimestamp;
    private String snapshotType;
    private String reason;

    @Indexed
    private String sku;

    // Stock levels
    private int quantityOnHand;
    private int quantityAllocated;
    private int quantityReserved;
    private int quantityAvailable;

    // Stock by status (stored as Map)
    private Map<String, Integer> stockByStatus;

    // Active holds (embedded)
    private List<InventoryHoldSnapshotDocument> activeHolds;

    // Valuation
    private BigDecimal unitCost;
    private BigDecimal totalValue;
    private String valuationMethod;

    // ABC classification
    private String abcClass;
    private String abcCriteria;

    // Lot/batch tracking
    private List<LotBatchSnapshotDocument> lotBatches;

    // Serial numbers
    private List<String> serialNumbers;

    // Metadata
    private String createdBy;
    private LocalDateTime createdAt;

    public InventorySnapshotDocument() {
    }

    public static InventorySnapshotDocument fromDomain(InventorySnapshot snapshot) {
        InventorySnapshotDocument doc = new InventorySnapshotDocument();
        doc.snapshotId = snapshot.getSnapshotId();
        doc.snapshotTimestamp = snapshot.getSnapshotTimestamp();
        doc.snapshotType = snapshot.getSnapshotType().name();
        doc.reason = snapshot.getReason().name();
        doc.sku = snapshot.getSku();
        doc.quantityOnHand = snapshot.getQuantityOnHand();
        doc.quantityAllocated = snapshot.getQuantityAllocated();
        doc.quantityReserved = snapshot.getQuantityReserved();
        doc.quantityAvailable = snapshot.getQuantityAvailable();

        // Convert stock by status
        doc.stockByStatus = snapshot.getStockByStatus().entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().name(),
                Map.Entry::getValue
            ));

        // Convert holds
        doc.activeHolds = snapshot.getActiveHolds().stream()
            .map(InventoryHoldSnapshotDocument::fromDomain)
            .collect(Collectors.toList());

        doc.unitCost = snapshot.getUnitCost().orElse(null);
        doc.totalValue = snapshot.getTotalValue().orElse(null);
        doc.valuationMethod = snapshot.getValuationMethod().orElse(null);
        doc.abcClass = snapshot.getAbcClass().orElse(null);
        doc.abcCriteria = snapshot.getAbcCriteria().orElse(null);

        // Convert lot batches
        doc.lotBatches = snapshot.getLotBatches().stream()
            .map(LotBatchSnapshotDocument::fromDomain)
            .collect(Collectors.toList());

        doc.serialNumbers = snapshot.getSerialNumbers();
        doc.createdBy = snapshot.getCreatedBy();
        doc.createdAt = snapshot.getCreatedAt();

        return doc;
    }

    public InventorySnapshot toDomain() {
        // Convert stock by status
        Map<StockStatus, Integer> stockByStatusMap = new HashMap<>();
        if (stockByStatus != null) {
            stockByStatus.forEach((k, v) ->
                stockByStatusMap.put(StockStatus.valueOf(k), v));
        }

        // Convert holds
        List<InventoryHoldSnapshot> holds = activeHolds != null ?
            activeHolds.stream()
                .map(InventoryHoldSnapshotDocument::toDomain)
                .collect(Collectors.toList()) : List.of();

        // Convert lot batches
        List<LotBatchSnapshot> lots = lotBatches != null ?
            lotBatches.stream()
                .map(LotBatchSnapshotDocument::toDomain)
                .collect(Collectors.toList()) : List.of();

        // Use reflection/builder to create InventorySnapshot since constructor is private
        // For now, using a factory method approach
        return InventorySnapshot.fromProductStock(
            createMockProductStock(),
            snapshotTimestamp,
            SnapshotType.valueOf(snapshotType),
            SnapshotReason.valueOf(reason),
            createdBy
        );
    }

    // Helper method - in production, you'd reconstruct from stored data
    private ProductStock createMockProductStock() {
        ProductStock ps = ProductStock.create(sku, quantityOnHand);
        // Note: Full reconstruction would require more sophisticated approach
        return ps;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public LocalDateTime getSnapshotTimestamp() {
        return snapshotTimestamp;
    }

    public void setSnapshotTimestamp(LocalDateTime snapshotTimestamp) {
        this.snapshotTimestamp = snapshotTimestamp;
    }

    public String getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(String snapshotType) {
        this.snapshotType = snapshotType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public int getQuantityAllocated() {
        return quantityAllocated;
    }

    public void setQuantityAllocated(int quantityAllocated) {
        this.quantityAllocated = quantityAllocated;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(int quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public Map<String, Integer> getStockByStatus() {
        return stockByStatus;
    }

    public void setStockByStatus(Map<String, Integer> stockByStatus) {
        this.stockByStatus = stockByStatus;
    }

    public List<InventoryHoldSnapshotDocument> getActiveHolds() {
        return activeHolds;
    }

    public void setActiveHolds(List<InventoryHoldSnapshotDocument> activeHolds) {
        this.activeHolds = activeHolds;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public String getValuationMethod() {
        return valuationMethod;
    }

    public void setValuationMethod(String valuationMethod) {
        this.valuationMethod = valuationMethod;
    }

    public String getAbcClass() {
        return abcClass;
    }

    public void setAbcClass(String abcClass) {
        this.abcClass = abcClass;
    }

    public String getAbcCriteria() {
        return abcCriteria;
    }

    public void setAbcCriteria(String abcCriteria) {
        this.abcCriteria = abcCriteria;
    }

    public List<LotBatchSnapshotDocument> getLotBatches() {
        return lotBatches;
    }

    public void setLotBatches(List<LotBatchSnapshotDocument> lotBatches) {
        this.lotBatches = lotBatches;
    }

    public List<String> getSerialNumbers() {
        return serialNumbers;
    }

    public void setSerialNumbers(List<String> serialNumbers) {
        this.serialNumbers = serialNumbers;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
