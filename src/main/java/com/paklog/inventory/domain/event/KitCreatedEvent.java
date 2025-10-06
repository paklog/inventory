package com.paklog.inventory.domain.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain event published when a kit definition is created.
 */
public record KitCreatedEvent(
    String kitSku,
    String kitDescription,
    String kitType,
    List<String> componentSkus,
    int componentCount,
    LocalDateTime occurredAt
) {
    public KitCreatedEvent(String kitSku, String kitDescription, String kitType,
                          List<String> componentSkus, int componentCount) {
        this(kitSku, kitDescription, kitType, componentSkus, componentCount, LocalDateTime.now());
    }
}
