package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.exception.InvalidQuantityException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root for cycle counting operations.
 * Cycle counts are used to maintain inventory accuracy without full physical inventories.
 */
public class CycleCount {

    private final String countId;
    private final String sku;
    private final Location location;
    private final CountType countType;
    private final CountStatus status;
    private final String assignedTo;
    private final LocalDateTime scheduledDate;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;

    // Count data
    private final Integer systemQuantity;
    private final Integer countedQuantity;
    private final String lotNumber; // Optional lot tracking
    private final String countedBy;
    private final String notes;

    // Variance handling
    private final Integer variance;
    private final BigDecimal varianceValue; // Monetary impact
    private final VarianceResolution resolution;
    private final String resolutionNotes;
    private final String approvedBy;
    private final LocalDateTime approvedAt;

    private CycleCount(String countId, String sku, Location location, CountType countType,
                      CountStatus status, String assignedTo, LocalDateTime scheduledDate,
                      LocalDateTime startedAt, LocalDateTime completedAt,
                      Integer systemQuantity, Integer countedQuantity, String lotNumber,
                      String countedBy, String notes, Integer variance, BigDecimal varianceValue,
                      VarianceResolution resolution, String resolutionNotes,
                      String approvedBy, LocalDateTime approvedAt) {
this.countId = countId;
        this.sku = sku;
        this.location = location;
        this.countType = countType;
        this.status = status;
        this.assignedTo = assignedTo;
        this.scheduledDate = scheduledDate;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.systemQuantity = systemQuantity;
        this.countedQuantity = countedQuantity;
        this.lotNumber = lotNumber;
        this.countedBy = countedBy;
        this.notes = notes;
        this.variance = variance;
        this.varianceValue = varianceValue;
        this.resolution = resolution;
        this.resolutionNotes = resolutionNotes;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
    }



    public static CycleCount create(String sku, Location location, CountType countType,
                                   String assignedTo, LocalDateTime scheduledDate, int systemQuantity) {
        String countId = UUID.randomUUID().toString();
        return new CycleCount(countId, sku, location, countType, CountStatus.SCHEDULED,
                            assignedTo, scheduledDate, null, null,
                            systemQuantity, null, null, null, null,
                            null, null, null, null, null, null);
    }

    public static CycleCount load(String countId, String sku, Location location, CountType countType,
                                 CountStatus status, String assignedTo, LocalDateTime scheduledDate,
                                 LocalDateTime startedAt, LocalDateTime completedAt,
                                 Integer systemQuantity, Integer countedQuantity, String lotNumber,
                                 String countedBy, String notes, Integer variance, BigDecimal varianceValue,
                                 VarianceResolution resolution, String resolutionNotes,
                                 String approvedBy, LocalDateTime approvedAt) {
        return new CycleCount(countId, sku, location, countType, status, assignedTo, scheduledDate,
                            startedAt, completedAt, systemQuantity, countedQuantity, lotNumber,
                            countedBy, notes, variance, varianceValue, resolution, resolutionNotes,
                            approvedBy, approvedAt);
    }

    public CycleCount start(String countedBy) {
        if (status != CountStatus.SCHEDULED) {
            throw new IllegalStateException("Can only start scheduled counts");
        }
        return new CycleCount(countId, sku, location, countType, CountStatus.IN_PROGRESS,
                            assignedTo, scheduledDate, LocalDateTime.now(), null,
                            systemQuantity, null, null, countedBy, null,
                            null, null, null, null, null, null);
    }

    public CycleCount complete(int countedQuantity, String lotNumber, String notes) {
        if (status != CountStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete counts that are in progress");
        }
        if (countedQuantity < 0) {
            throw new InvalidQuantityException("count", countedQuantity, "Counted quantity cannot be negative");
        }

        int variance = countedQuantity - systemQuantity;
        LocalDateTime now = LocalDateTime.now();

        CountStatus newStatus = variance == 0 ? CountStatus.COMPLETED : CountStatus.PENDING_APPROVAL;

        return new CycleCount(countId, sku, location, countType, newStatus,
                            assignedTo, scheduledDate, startedAt, now,
                            systemQuantity, countedQuantity, lotNumber, countedBy, notes,
                            variance, null, null, null, null, null);
    }

    public CycleCount approve(String approvedBy, VarianceResolution resolution, String resolutionNotes) {
        if (status != CountStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Can only approve counts pending approval");
        }

        return new CycleCount(countId, sku, location, countType, CountStatus.APPROVED,
                            assignedTo, scheduledDate, startedAt, completedAt,
                            systemQuantity, countedQuantity, lotNumber, countedBy, notes,
                            variance, varianceValue, resolution, resolutionNotes,
                            approvedBy, LocalDateTime.now());
    }

    public CycleCount reject(String rejectedBy, String rejectionReason) {
        if (status != CountStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Can only reject counts pending approval");
        }

        return new CycleCount(countId, sku, location, countType, CountStatus.REJECTED,
                            assignedTo, scheduledDate, startedAt, completedAt,
                            systemQuantity, countedQuantity, lotNumber, countedBy, notes,
                            variance, varianceValue, null, rejectionReason,
                            rejectedBy, LocalDateTime.now());
    }

    public boolean isAccurate() {
        return variance != null && variance == 0;
    }

    public boolean requiresApproval(int varianceThreshold) {
        return variance != null && Math.abs(variance) > varianceThreshold;
    }

    public double getAccuracyPercentage() {
        if (systemQuantity == 0 && countedQuantity == 0) {
            return 100.0;
        }
        if (systemQuantity == 0) {
            return 0.0;
        }
        return 100.0 - (Math.abs(variance) * 100.0 / systemQuantity);
    }

    // Getters
    public String getCountId() {
        return countId;
    }

    public String getSku() {
        return sku;
    }

    public Location getLocation() {
        return location;
    }

    public CountType getCountType() {
        return countType;
    }

    public CountStatus getStatus() {
        return status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public Integer getSystemQuantity() {
        return systemQuantity;
    }

    public Integer getCountedQuantity() {
        return countedQuantity;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public String getCountedBy() {
        return countedBy;
    }

    public String getNotes() {
        return notes;
    }

    public Integer getVariance() {
        return variance;
    }

    public BigDecimal getVarianceValue() {
        return varianceValue;
    }

    public VarianceResolution getResolution() {
        return resolution;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CycleCount that = (CycleCount) o;
        return Objects.equals(countId, that.countId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countId);
    }

    @Override
    public String toString() {
        return "CycleCount{" +
                "countId='" + countId + '\'' +
                ", sku='" + sku + '\'' +
                ", location=" + location +
                ", countType=" + countType +
                ", status=" + status +
                ", systemQuantity=" + systemQuantity +
                ", countedQuantity=" + countedQuantity +
                ", variance=" + variance +
                ", accuracyPercentage=" + (countedQuantity != null ? getAccuracyPercentage() : "N/A") +
                '}';
    }
}
