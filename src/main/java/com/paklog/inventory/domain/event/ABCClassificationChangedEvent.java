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

    private final String sku;
    private final ABCClass previousClass;
    private final ABCClass newClass;
    private final ABCCriteria criteria;
    private final String reason;

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
}
