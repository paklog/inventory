package com.paklog.inventory.domain.event;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain event published when kits are assembled from components.
 */
public record KitAssembledEvent(
    String assemblyOrderId,
    String kitSku,
    int kitQuantityPlanned,
    int kitQuantityProduced,
    Map<String, Integer> componentsConsumed,  // componentSku → quantity
    String assemblyLocation,
    String completedBy,
    LocalDateTime occurredAt
) {
    public KitAssembledEvent(String assemblyOrderId, String kitSku, int kitQuantityPlanned,
                            int kitQuantityProduced, Map<String, Integer> componentsConsumed,
                            String assemblyLocation, String completedBy) {
        this(assemblyOrderId, kitSku, kitQuantityPlanned, kitQuantityProduced,
            componentsConsumed, assemblyLocation, completedBy, LocalDateTime.now());
    }
}
