package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.UOMConversion;
import com.paklog.inventory.domain.model.UnitOfMeasure;
import com.paklog.inventory.domain.repository.UOMConversionRepository;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application service for UOM management.
 * Handles UOM conversions and multi-UOM operations.
 */
@Service
public class UOMManagementService {

    private static final Logger logger = LoggerFactory.getLogger(UOMManagementService.class);

    private final UOMConversionRepository uomConversionRepository;
    private final OutboxRepository outboxRepository;

    public UOMManagementService(UOMConversionRepository uomConversionRepository,
                               OutboxRepository outboxRepository) {
        this.uomConversionRepository = uomConversionRepository;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Add UOM conversion for a SKU
     */
    @Transactional
    public void addConversion(String sku, UnitOfMeasure fromUOM, UnitOfMeasure toUOM,
                             BigDecimal conversionFactor, boolean isReversible) {
        logger.info("Adding UOM conversion: SKU={}, {} → {}, factor={}",
                sku, fromUOM.getCode(), toUOM.getCode(), conversionFactor);

        if (uomConversionRepository.exists(sku, fromUOM, toUOM)) {
            throw new IllegalArgumentException(
                "Conversion already exists for " + sku + ": " + fromUOM + " → " + toUOM);
        }

        UOMConversion conversion = isReversible ?
            UOMConversion.twoWay(fromUOM, toUOM, conversionFactor) :
            UOMConversion.oneWay(fromUOM, toUOM, conversionFactor);

        uomConversionRepository.save(sku, conversion);

        logger.info("UOM conversion added: SKU={}, {} → {}", sku, fromUOM.getCode(), toUOM.getCode());
    }

    /**
     * Convert quantity between UOMs
     */
    @Transactional(readOnly = true)
    public BigDecimal convert(String sku, BigDecimal quantity, UnitOfMeasure fromUOM, UnitOfMeasure toUOM) {
        logger.debug("Converting quantity: SKU={}, {} {} → {}",
                sku, quantity, fromUOM.getCode(), toUOM.getCode());

        if (fromUOM.equals(toUOM)) {
            return quantity;
        }

        UOMConversion conversion = uomConversionRepository.findBySku(sku, fromUOM, toUOM)
            .orElseThrow(() -> new IllegalArgumentException(
                "No conversion found for " + sku + ": " + fromUOM + " → " + toUOM));

        return conversion.convert(quantity);
    }

    /**
     * Get all conversions for a SKU
     */
    @Transactional(readOnly = true)
    public List<UOMConversion> getConversions(String sku) {
        logger.debug("Getting all UOM conversions for SKU={}", sku);
        return uomConversionRepository.findAllBySku(sku);
    }

    /**
     * Remove UOM conversion
     */
    @Transactional
    public void removeConversion(String sku, UnitOfMeasure fromUOM, UnitOfMeasure toUOM) {
        logger.info("Removing UOM conversion: SKU={}, {} → {}",
                sku, fromUOM.getCode(), toUOM.getCode());

        uomConversionRepository.delete(sku, fromUOM, toUOM);

        logger.info("UOM conversion removed: SKU={}, {} → {}",
                sku, fromUOM.getCode(), toUOM.getCode());
    }

    /**
     * Check if conversion exists
     */
    @Transactional(readOnly = true)
    public boolean hasConversion(String sku, UnitOfMeasure fromUOM, UnitOfMeasure toUOM) {
        return uomConversionRepository.exists(sku, fromUOM, toUOM);
    }
}
