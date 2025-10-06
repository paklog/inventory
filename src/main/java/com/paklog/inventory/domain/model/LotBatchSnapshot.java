package com.paklog.inventory.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Value object capturing lot/batch state at snapshot time.
 */
public class LotBatchSnapshot {

    private final String lotNumber;
    private final int quantity;
    private final LocalDate expiryDate;
    private final LocalDate manufactureDate;
    private final String batchStatus;

    private LotBatchSnapshot(String lotNumber, int quantity, LocalDate expiryDate,
                            LocalDate manufactureDate, String batchStatus) {
        this.lotNumber = lotNumber;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.manufactureDate = manufactureDate;
        this.batchStatus = batchStatus;
    }

    public static LotBatchSnapshot of(String lotNumber, int quantity, LocalDate expiryDate,
                                     LocalDate manufactureDate, String batchStatus) {
        return new LotBatchSnapshot(lotNumber, quantity, expiryDate, manufactureDate, batchStatus);
    }

    public static LotBatchSnapshot fromLotBatch(LotBatch lotBatch) {
        return new LotBatchSnapshot(
            lotBatch.getLotNumber(),
            lotBatch.getQuantity(),
            lotBatch.getExpiryDate(),
            lotBatch.getManufactureDate(),
            lotBatch.getStatus().name()
        );
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LotBatchSnapshot that = (LotBatchSnapshot) o;
        return Objects.equals(lotNumber, that.lotNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lotNumber);
    }
}
