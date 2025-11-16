package com.paklog.inventory.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object representing the source system that originated a stock transaction.
 * Used for integration tracking and audit trail purposes.
 */
public class SourceSystemReference {

    private final String sourceSystem;
    private final String sourceTransactionId;
    private final LocalDateTime integrationTimestamp;
    private final String sourceOperatorId;

    private SourceSystemReference(String sourceSystem, String sourceTransactionId,
                                  LocalDateTime integrationTimestamp, String sourceOperatorId) {
        this.sourceSystem = sourceSystem;
        this.sourceTransactionId = sourceTransactionId;
        this.integrationTimestamp = integrationTimestamp;
        this.sourceOperatorId = sourceOperatorId;
    }

    /**
     * Create a source system reference
     *
     * @param sourceSystem Name of the source system (e.g., "WMS", "ERP", "POS", "MANUAL")
     * @param sourceTransactionId Transaction ID in the source system
     * @param integrationTimestamp When the transaction was integrated
     * @param sourceOperatorId User/operator in the source system
     * @return SourceSystemReference instance
     */
    public static SourceSystemReference of(String sourceSystem, String sourceTransactionId,
                                           LocalDateTime integrationTimestamp, String sourceOperatorId) {
        if (sourceSystem == null || sourceSystem.trim().isEmpty()) {
            throw new IllegalArgumentException("Source system cannot be null or empty");
        }

        return new SourceSystemReference(
                sourceSystem.toUpperCase(),
                sourceTransactionId,
                integrationTimestamp != null ? integrationTimestamp : LocalDateTime.now(),
                sourceOperatorId
        );
    }

    /**
     * Create a source system reference for manual operations
     */
    public static SourceSystemReference manual(String operatorId) {
        return new SourceSystemReference("MANUAL", null, LocalDateTime.now(), operatorId);
    }

    /**
     * Create a source system reference from external WMS
     */
    public static SourceSystemReference fromWMS(String transactionId, String operatorId) {
        return new SourceSystemReference("WMS", transactionId, LocalDateTime.now(), operatorId);
    }

    /**
     * Create a source system reference from ERP system
     */
    public static SourceSystemReference fromERP(String transactionId, String operatorId) {
        return new SourceSystemReference("ERP", transactionId, LocalDateTime.now(), operatorId);
    }

    /**
     * Create a source system reference from POS system
     */
    public static SourceSystemReference fromPOS(String transactionId, String operatorId) {
        return new SourceSystemReference("POS", transactionId, LocalDateTime.now(), operatorId);
    }

    /**
     * Create a source system reference from e-commerce platform
     */
    public static SourceSystemReference fromEcommerce(String transactionId) {
        return new SourceSystemReference("ECOMMERCE", transactionId, LocalDateTime.now(), "SYSTEM");
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getSourceTransactionId() {
        return sourceTransactionId;
    }

    public LocalDateTime getIntegrationTimestamp() {
        return integrationTimestamp;
    }

    public String getSourceOperatorId() {
        return sourceOperatorId;
    }

    public boolean isManual() {
        return "MANUAL".equals(sourceSystem);
    }

    public boolean isAutomated() {
        return !isManual();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceSystemReference that = (SourceSystemReference) o;
        return Objects.equals(sourceSystem, that.sourceSystem) &&
               Objects.equals(sourceTransactionId, that.sourceTransactionId) &&
               Objects.equals(integrationTimestamp, that.integrationTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceSystem, sourceTransactionId, integrationTimestamp);
    }

    @Override
    public String toString() {
        return "SourceSystemReference{" +
               "sourceSystem='" + sourceSystem + '\'' +
               ", sourceTransactionId='" + sourceTransactionId + '\'' +
               ", integrationTimestamp=" + integrationTimestamp +
               ", sourceOperatorId='" + sourceOperatorId + '\'' +
               '}';
    }
}
