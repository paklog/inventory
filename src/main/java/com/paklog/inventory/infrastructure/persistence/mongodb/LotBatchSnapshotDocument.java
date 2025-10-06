package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.LotBatchSnapshot;

import java.time.LocalDate;

/**
 * Embedded document for LotBatchSnapshot.
 */
public class LotBatchSnapshotDocument {

    private String lotNumber;
    private int quantity;
    private LocalDate expiryDate;
    private LocalDate manufactureDate;
    private String batchStatus;

    public LotBatchSnapshotDocument() {
    }

    public static LotBatchSnapshotDocument fromDomain(LotBatchSnapshot lot) {
        LotBatchSnapshotDocument doc = new LotBatchSnapshotDocument();
        doc.lotNumber = lot.getLotNumber();
        doc.quantity = lot.getQuantity();
        doc.expiryDate = lot.getExpiryDate();
        doc.manufactureDate = lot.getManufactureDate();
        doc.batchStatus = lot.getBatchStatus();
        return doc;
    }

    public LotBatchSnapshot toDomain() {
        return LotBatchSnapshot.of(lotNumber, quantity, expiryDate,
            manufactureDate, batchStatus);
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(LocalDate manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }
}
