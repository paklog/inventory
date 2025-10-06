package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.Kit;
import com.paklog.inventory.domain.model.KitType;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Kit aggregate.
 */
public interface KitRepository {

    /**
     * Save kit
     */
    Kit save(Kit kit);

    /**
     * Find kit by SKU
     */
    Optional<Kit> findByKitSku(String kitSku);

    /**
     * Find all active kits
     */
    List<Kit> findActiveKits();

    /**
     * Find kits by type
     */
    List<Kit> findByType(KitType kitType);

    /**
     * Find kits containing a specific component
     */
    List<Kit> findKitsContainingComponent(String componentSku);

    /**
     * Check if kit exists
     */
    boolean existsByKitSku(String kitSku);

    /**
     * Delete kit
     */
    void delete(String kitSku);
}
