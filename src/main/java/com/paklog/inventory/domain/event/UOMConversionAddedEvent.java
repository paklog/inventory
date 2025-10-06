package com.paklog.inventory.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain event published when a UOM conversion is added for a SKU.
 */
public record UOMConversionAddedEvent(
    String sku,
    String fromUOM,
    String toUOM,
    BigDecimal conversionFactor,
    boolean isReversible,
    LocalDateTime occurredAt
) {
    public UOMConversionAddedEvent(String sku, String fromUOM, String toUOM,
                                  BigDecimal conversionFactor, boolean isReversible) {
        this(sku, fromUOM, toUOM, conversionFactor, isReversible, LocalDateTime.now());
    }
}
