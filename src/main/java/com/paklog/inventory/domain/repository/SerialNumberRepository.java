package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.SerialNumber;
import com.paklog.inventory.domain.model.SerialStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SerialNumber aggregate.
 * Part of the Inventory bounded context.
 */
public interface SerialNumberRepository {

    /**
     * Save a serial number
     */
    SerialNumber save(SerialNumber serialNumber);

    /**
     * Find by serial number (unique identifier)
     */
    Optional<SerialNumber> findBySerialNumber(String serialNumber);

    /**
     * Find all serials for a SKU
     */
    List<SerialNumber> findBySku(String sku);

    /**
     * Find serials by SKU and status
     */
    List<SerialNumber> findBySkuAndStatus(String sku, SerialStatus status);

    /**
     * Find available serials for a SKU (status = AVAILABLE)
     */
    List<SerialNumber> findAvailableBySku(String sku);

    /**
     * Find serials allocated to a customer
     */
    List<SerialNumber> findByCustomerId(String customerId);

    /**
     * Check if serial number exists
     */
    boolean existsBySerialNumber(String serialNumber);

    /**
     * Delete a serial number
     */
    void delete(SerialNumber serialNumber);

    /**
     * Count serials by SKU and status
     */
    long countBySkuAndStatus(String sku, SerialStatus status);
}
