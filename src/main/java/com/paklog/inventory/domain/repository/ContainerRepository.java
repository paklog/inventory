package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.Container;
import com.paklog.inventory.domain.model.ContainerStatus;
import com.paklog.inventory.domain.model.ContainerType;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Container aggregate.
 * Part of the Inventory bounded context.
 */
public interface ContainerRepository {

    /**
     * Save a container
     */
    Container save(Container container);

    /**
     * Find by LPN (License Plate Number)
     */
    Optional<Container> findByLpn(String lpn);

    /**
     * Find containers by status
     */
    List<Container> findByStatus(ContainerStatus status);

    /**
     * Find containers by type
     */
    List<Container> findByType(ContainerType type);

    /**
     * Find containers by type and status
     */
    List<Container> findByTypeAndStatus(ContainerType type, ContainerStatus status);

    /**
     * Find active containers (status = ACTIVE)
     */
    List<Container> findActiveContainers();

    /**
     * Find containers at a location
     */
    List<Container> findByLocation(String warehouseId, String zoneId);

    /**
     * Find child containers (nested inside parent)
     */
    List<Container> findByParentLpn(String parentLpn);

    /**
     * Find empty containers (no items)
     */
    List<Container> findEmptyContainers();

    /**
     * Check if LPN exists
     */
    boolean existsByLpn(String lpn);

    /**
     * Delete a container
     */
    void delete(Container container);

    /**
     * Count containers by status
     */
    long countByStatus(ContainerStatus status);

    /**
     * Count containers by type
     */
    long countByType(ContainerType type);
}
