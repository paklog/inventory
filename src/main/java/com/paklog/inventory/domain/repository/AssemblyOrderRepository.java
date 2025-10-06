package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.AssemblyOrder;
import com.paklog.inventory.domain.model.AssemblyStatus;
import com.paklog.inventory.domain.model.AssemblyType;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AssemblyOrder aggregate.
 */
public interface AssemblyOrderRepository {

    /**
     * Save assembly order
     */
    AssemblyOrder save(AssemblyOrder order);

    /**
     * Find by order ID
     */
    Optional<AssemblyOrder> findById(String assemblyOrderId);

    /**
     * Find by kit SKU
     */
    List<AssemblyOrder> findByKitSku(String kitSku);

    /**
     * Find by status
     */
    List<AssemblyOrder> findByStatus(AssemblyStatus status);

    /**
     * Find by type
     */
    List<AssemblyOrder> findByType(AssemblyType type);

    /**
     * Find by type and status
     */
    List<AssemblyOrder> findByTypeAndStatus(AssemblyType type, AssemblyStatus status);

    /**
     * Find in-progress orders
     */
    List<AssemblyOrder> findInProgressOrders();

    /**
     * Count by status
     */
    long countByStatus(AssemblyStatus status);
}
