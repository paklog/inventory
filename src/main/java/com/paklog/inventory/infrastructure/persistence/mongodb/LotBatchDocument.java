package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.BatchStatus;
import com.paklog.inventory.domain.model.LotBatch;

import java.time.LocalDate;

/**
 * Embedded document for lot batches within ProductStockDocument
 */
public class LotBatchDocument {

    private String lotNumber;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private int quantityOnHand;
    private int quantityAllocated;
    private String batchStatus; // BatchStatus enum as string

    public LotBatchDocument() {
    }

    public static LotBatchDocument fromDomain(LotBatch lot) {
        LotBatchDocument doc = new LotBatchDocument();
        doc.lotNumber = lot.getLotNumber();
        doc.manufactureDate = lot.getManufactureDate();
        doc.expiryDate = lot.getExpiryDate();
        doc.quantityOnHand = lot.getQuantityOnHand();
        doc.quantityAllocated = lot.getQuantityAllocated();
        doc.batchStatus = lot.getStatus().name();
        return doc;
    }

    public LotBatch toDomain() {
        return LotBatch.load(
            lotNumber,
            manufactureDate,
            expiryDate,
            null, // supplierId
            BatchStatus.valueOf(batchStatus),
            quantityOnHand,
            quantityAllocated
        );
    }

    // Getters and setters
    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(LocalDate manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
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

    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }
}
