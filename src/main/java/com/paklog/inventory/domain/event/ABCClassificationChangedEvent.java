package com.paklog.inventory.domain.event;

import com.paklog.inventory.domain.model.ABCClass;
import com.paklog.inventory.domain.model.ABCCriteria;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain event published when SKU ABC classification changes.
 * Important for cycle counting schedules and inventory policies.
 */
public class ABCClassificationChangedEvent extends DomainEvent {

    private String sku;
    private ABCClass previousClass;
    private ABCClass newClass;
    private ABCCriteria criteria;
    private String reason;

    public ABCClassificationChangedEvent(String sku,
                                        ABCClass previousClass,
                                        ABCClass newClass,
                                        ABCCriteria criteria,
                                        String reason) {
        super(sku);
        this.sku = sku;
        this.previousClass = previousClass;
        this.newClass = newClass;
        this.criteria = criteria;
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return CloudEventType.ABC_CLASSIFICATION_CHANGED.getType();
    }

    @Override
    public java.util.Map<String, Object> getEventData() {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("sku", sku);
        data.put("previousClass", previousClass != null ? previousClass.name() : null);
        data.put("newClass", newClass != null ? newClass.name() : null);
        data.put("criteria", criteria.name());
        data.put("reason", reason);
        return java.util.Collections.unmodifiableMap(data);
    }

    public LocalDateTime occurredOn() {
        return getOccurredOn();
    }

    public String getSku() {
        return sku;
    }

    public ABCClass getPreviousClass() {
        return previousClass;
    }

    public ABCClass getNewClass() {
        return newClass;
    }

    public ABCCriteria getCriteria() {
        return criteria;
    }

    public String getReason() {
        return reason;
    }

    /**
     * Check if classification was upgraded (C->B, B->A, C->A)
     */
    public boolean isUpgrade() {
        if (previousClass == null || newClass == null) {
            return false;
        }
        return newClass.ordinal() < previousClass.ordinal();
    }

    /**
     * Check if classification was downgraded (A->B, B->C, A->C)
     */
    public boolean isDowngrade() {
        if (previousClass == null || newClass == null) {
            return false;
        }
        return newClass.ordinal() > previousClass.ordinal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ABCClassificationChangedEvent that = (ABCClassificationChangedEvent) o;
        return Objects.equals(sku, that.sku) &&
               previousClass == that.previousClass &&
               newClass == that.newClass &&
               Objects.equals(occurredOn, that.occurredOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, previousClass, newClass, occurredOn);
    }

    @Override
    public String toString() {
        return String.format("ABCClassificationChangedEvent{sku='%s', %s->%s, criteria=%s, reason='%s'}",
                sku, previousClass, newClass, criteria, reason);
    }



    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String sku;
        private ABCClass previousClass;
        private ABCClass newClass;
        private ABCCriteria criteria;
        private String reason;

        public Builder sku(final String sku) { this.sku = sku; return this; }
        public Builder previousClass(final ABCClass previousClass) { this.previousClass = previousClass; return this; }
        public Builder newClass(final ABCClass newClass) { this.newClass = newClass; return this; }
        public Builder criteria(final ABCCriteria criteria) { this.criteria = criteria; return this; }
        public Builder reason(final String reason) { this.reason = reason; return this; }

        public ABCClassificationChangedEvent build() {
            return new ABCClassificationChangedEvent(sku, previousClass, newClass, criteria, reason);
        }
    }
}

