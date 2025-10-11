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
    private int quantityOnHand;
    private int quantityAllocated;
    private LocalDateTime lastUpdated;

    @Version
    private Long version; // Optimistic locking version

    // Phase 1 fields
    private Map<String, Integer> stockByStatus; // StockStatus -> quantity
    private List<InventoryHoldDocument> holds;
    private boolean serialTracked;
    private List<String> serialNumberIds; // References to SerialNumber collection

    // Phase 2 fields
    private InventoryValuationDocument valuation;
    private ABCClassificationDocument abcClassification;

    // Existing lot tracking (already in domain)
    private List<LotBatchDocument> lotBatches;

    public ProductStockDocument() {
    }

    public ProductStockDocument(String sku, int quantityOnHand, int quantityAllocated, LocalDateTime lastUpdated) {
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.quantityAllocated = quantityAllocated;
        this.lastUpdated = lastUpdated;
        this.stockByStatus = new HashMap<>();
        this.holds = new ArrayList<>();
        this.serialNumberIds = new ArrayList<>();
        this.lotBatches = new ArrayList<>();
    }

    public static ProductStockDocument fromDomain(ProductStock productStock) {
        ProductStockDocument doc = new ProductStockDocument(
                productStock.getSku(),
                productStock.getQuantityOnHand(),
                productStock.getQuantityAllocated(),
                productStock.getLastUpdated()
        );
        doc.setVersion(productStock.getVersion());

        // Map stock status quantities
        doc.stockByStatus = productStock.getStockByStatus().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> e.getValue().getQuantity()
                ));

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

        return doc;
    }

    public ProductStock toDomain() {
        ProductStock productStock = ProductStock.load(
                this.sku,
                this.quantityOnHand,
                this.quantityAllocated,
                this.lastUpdated
        );

        // Note: Full reconstruction with all fields would require
        // a more comprehensive load method in ProductStock.
        // For now, this provides basic reconstruction.
        // In production, you'd likely add a richer factory method.

        return productStock;
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
}
