package com.paklog.inventory.domain.event;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain event published when kits are disassembled into components.
 */
public record KitDisassembledEvent(
    String assemblyOrderId,
    String kitSku,
    int kitQuantityDisassembled,
    Map<String, Integer> componentsRecovered,  // componentSku → quantity
    String assemblyLocation,
    String completedBy,
    LocalDateTime occurredAt
) {
    public KitDisassembledEvent(String assemblyOrderId, String kitSku,
                               int kitQuantityDisassembled,
                               Map<String, Integer> componentsRecovered,
                               String assemblyLocation, String completedBy) {
        this(assemblyOrderId, kitSku, kitQuantityDisassembled, componentsRecovered,
            assemblyLocation, completedBy, LocalDateTime.now());
    }
}
