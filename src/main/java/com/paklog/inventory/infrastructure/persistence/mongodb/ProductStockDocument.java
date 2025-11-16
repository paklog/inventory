package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Document(collection = "product_stocks")
@CompoundIndexes({
    @CompoundIndex(name = "abc_class_idx", def = "{'abcClassification.abcClass': 1}"),
    @CompoundIndex(name = "stock_status_idx", def = "{'stockByStatus.status': 1}"),
    @CompoundIndex(name = "valuation_method_idx", def = "{'valuation.valuationMethod': 1}")
})
public class ProductStockDocument {

    @Id
    private String sku;

    private int quantityAllocated;
    private LocalDateTime lastUpdated;

    @Version
    private Long version; // Optimistic locking version

    // stockByStatus is the SOURCE OF TRUTH for quantity on hand
    private Map<String, Integer> stockByStatus; // StockStatus -> quantity (SOURCE OF TRUTH)
    private List<InventoryHoldDocument> holds;
    private boolean serialTracked;
    private List<String> serialNumberIds; // References to SerialNumber collection

    // Phase 2 fields
    private InventoryValuationDocument valuation;
    private ABCClassificationDocument abcClassification;

    // Existing lot tracking (already in domain)
    private List<LotBatchDocument> lotBatches;

    // Multi-location support
    @Indexed
    private String locationId;

    public ProductStockDocument() {
        this.stockByStatus = new HashMap<>();
        this.holds = new ArrayList<>();
        this.serialNumberIds = new ArrayList<>();
        this.lotBatches = new ArrayList<>();
    }

    public ProductStockDocument(String sku, Map<String, Integer> stockByStatus, int quantityAllocated, LocalDateTime lastUpdated) {
        this.sku = sku;
        this.stockByStatus = stockByStatus != null ? new HashMap<>(stockByStatus) : new HashMap<>();
        this.quantityAllocated = quantityAllocated;
        this.lastUpdated = lastUpdated;
        this.holds = new ArrayList<>();
        this.serialNumberIds = new ArrayList<>();
        this.lotBatches = new ArrayList<>();
    }

    public static ProductStockDocument fromDomain(ProductStock productStock) {
        // Map stock status quantities - stockByStatus is SOURCE OF TRUTH
        Map<String, Integer> statusMap = productStock.getStockByStatus().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> e.getValue().getQuantity()
                ));

        // If stockByStatus is empty but we have quantity, default to AVAILABLE
        if (statusMap.isEmpty() && productStock.getQuantityOnHand() > 0) {
            statusMap.put("AVAILABLE", productStock.getQuantityOnHand());
        }

        ProductStockDocument doc = new ProductStockDocument(
                productStock.getSku(),
                statusMap,
                productStock.getQuantityAllocated(),
                productStock.getLastUpdated()
        );
        doc.setVersion(productStock.getVersion());

        // Map holds
        doc.holds = productStock.getHolds().stream()
                .map(InventoryHoldDocument::fromDomain)
                .collect(Collectors.toList());

        // Map serial tracking
        doc.serialTracked = productStock.isSerialTracked();
        doc.serialNumberIds = productStock.getSerialNumbers().stream()
                .map(SerialNumber::getSerialNumber)
                .collect(Collectors.toList());

        // Map valuation
        productStock.getValuation().ifPresent(val ->
                doc.valuation = InventoryValuationDocument.fromDomain(val));

        // Map ABC classification
        productStock.getAbcClassification().ifPresent(abc ->
                doc.abcClassification = ABCClassificationDocument.fromDomain(abc));

        // Map lot batches
        doc.lotBatches = productStock.getLotBatches().stream()
                .map(LotBatchDocument::fromDomain)
                .collect(Collectors.toList());

        // Map location
        doc.locationId = productStock.getLocationId();

        return doc;
    }

    public ProductStock toDomain() {
        // Convert stock by status map - SOURCE OF TRUTH
        Map<StockStatus, StockStatusQuantity> domainStockByStatus = new HashMap<>();
        if (this.stockByStatus != null && !this.stockByStatus.isEmpty()) {
            for (Map.Entry<String, Integer> entry : this.stockByStatus.entrySet()) {
                StockStatus status = StockStatus.valueOf(entry.getKey());
                domainStockByStatus.put(status, StockStatusQuantity.of(status, entry.getValue()));
            }
        }

        // Derive quantityOnHand from stockByStatus (source of truth)
        int derivedQuantityOnHand = domainStockByStatus.values().stream()
                .mapToInt(StockStatusQuantity::getQuantity)
                .sum();

        // Convert holds
        List<InventoryHold> domainHolds = new ArrayList<>();
        if (this.holds != null) {
            domainHolds = this.holds.stream()
                    .map(InventoryHoldDocument::toDomain)
                    .collect(Collectors.toList());
        }

        // Convert lot batches
        List<LotBatch> domainLotBatches = new ArrayList<>();
        if (this.lotBatches != null) {
            domainLotBatches = this.lotBatches.stream()
                    .map(LotBatchDocument::toDomain)
                    .collect(Collectors.toList());
        }

        // Convert valuation (if present)
        InventoryValuation domainValuation = null;
        if (this.valuation != null) {
            domainValuation = this.valuation.toDomain(this.sku);
        }

        // Convert ABC classification (if present)
        ABCClassification domainAbcClassification = null;
        if (this.abcClassification != null) {
            domainAbcClassification = this.abcClassification.toDomain(this.sku);
        }

        // Note: Serial numbers are stored as IDs only (references to SerialNumber collection)
        // Full serial number reconstruction requires a separate repository lookup
        List<SerialNumber> domainSerialNumbers = new ArrayList<>();

        return ProductStock.loadComplete(
                this.sku,
                derivedQuantityOnHand,  // Derived from stockByStatus
                this.quantityAllocated,
                this.lastUpdated,
                this.version,
                domainStockByStatus,
                domainHolds,
                this.serialTracked,
                domainSerialNumbers,
                domainValuation,
                domainAbcClassification,
                domainLotBatches,
                this.locationId
        );
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    /**
     * Get quantity on hand - derived from stockByStatus (source of truth).
     */
    public int getQuantityOnHand() {
        if (stockByStatus == null || stockByStatus.isEmpty()) {
            return 0;
        }
        return stockByStatus.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int getQuantityAllocated() {
        return quantityAllocated;
    }

    public void setQuantityAllocated(int quantityAllocated) {
        this.quantityAllocated = quantityAllocated;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Map<String, Integer> getStockByStatus() {
        return stockByStatus;
    }

    public void setStockByStatus(Map<String, Integer> stockByStatus) {
        this.stockByStatus = stockByStatus;
    }

    public List<InventoryHoldDocument> getHolds() {
        return holds;
    }

    public void setHolds(List<InventoryHoldDocument> holds) {
        this.holds = holds;
    }

    public boolean isSerialTracked() {
        return serialTracked;
    }

    public void setSerialTracked(boolean serialTracked) {
        this.serialTracked = serialTracked;
    }

    public List<String> getSerialNumberIds() {
        return serialNumberIds;
    }

    public void setSerialNumberIds(List<String> serialNumberIds) {
        this.serialNumberIds = serialNumberIds;
    }

    public InventoryValuationDocument getValuation() {
        return valuation;
    }

    public void setValuation(InventoryValuationDocument valuation) {
        this.valuation = valuation;
    }

    public ABCClassificationDocument getAbcClassification() {
        return abcClassification;
    }

    public void setAbcClassification(ABCClassificationDocument abcClassification) {
        this.abcClassification = abcClassification;
    }

    public List<LotBatchDocument> getLotBatches() {
        return lotBatches;
    }

    public void setLotBatches(List<LotBatchDocument> lotBatches) {
        this.lotBatches = lotBatches;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }
}
