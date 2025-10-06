package com.paklog.inventory.domain.repository;

import com.paklog.inventory.domain.model.UOMConversion;
import com.paklog.inventory.domain.model.UnitOfMeasure;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UOM conversions.
 */
public interface UOMConversionRepository {

    /**
     * Save a UOM conversion for a SKU
     */
    void save(String sku, UOMConversion conversion);

    /**
     * Find conversion between two UOMs for a SKU
     */
    Optional<UOMConversion> findBySku(String sku, UnitOfMeasure fromUOM, UnitOfMeasure toUOM);

    /**
     * Find all conversions for a SKU
     */
    List<UOMConversion> findAllBySku(String sku);

    /**
     * Delete conversion
     */
    void delete(String sku, UnitOfMeasure fromUOM, UnitOfMeasure toUOM);

    /**
     * Check if conversion exists
     */
    boolean exists(String sku, UnitOfMeasure fromUOM, UnitOfMeasure toUOM);
}
