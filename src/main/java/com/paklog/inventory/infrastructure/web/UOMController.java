package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.AddUOMConversionRequest;
import com.paklog.inventory.application.dto.ConvertQuantityRequest;
import com.paklog.inventory.application.service.UOMManagementService;
import com.paklog.inventory.domain.model.UOMType;
import com.paklog.inventory.domain.model.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for UOM management.
 */
@RestController
@RequestMapping("/api/v1/inventory/uom")
public class UOMController {

    private static final Logger logger = LoggerFactory.getLogger(UOMController.class);

    private final UOMManagementService uomManagementService;

    public UOMController(UOMManagementService uomManagementService) {
        this.uomManagementService = uomManagementService;
    }

    /**
     * Add UOM conversion
     * POST /api/v1/inventory/uom/conversions
     */
    @PostMapping("/conversions")
    public ResponseEntity<Void> addConversion(@RequestBody AddUOMConversionRequest request) {
        logger.info("Adding UOM conversion: SKU={}, {} → {}",
                request.sku(), request.fromUOMCode(), request.toUOMCode());

        UnitOfMeasure fromUOM = createUOM(request.fromUOMCode(), request.fromUOMDescription(),
                request.fromUOMType(), request.fromUOMDecimalPrecision());
        UnitOfMeasure toUOM = createUOM(request.toUOMCode(), request.toUOMDescription(),
                request.toUOMType(), request.toUOMDecimalPrecision());

        uomManagementService.addConversion(request.sku(), fromUOM, toUOM,
                request.conversionFactor(), request.reversible());

        return ResponseEntity.ok().build();
    }

    /**
     * Convert quantity between UOMs
     * POST /api/v1/inventory/uom/convert
     */
    @PostMapping("/convert")
    public ResponseEntity<BigDecimal> convertQuantity(@RequestBody ConvertQuantityRequest request) {
        logger.debug("Converting quantity: SKU={}, {} {} → {}",
                request.sku(), request.quantity(), request.fromUOMCode(), request.toUOMCode());

        // For simplicity, assuming discrete UOMs - in production, load from database
        UnitOfMeasure fromUOM = UnitOfMeasure.discrete(request.fromUOMCode(), request.fromUOMCode());
        UnitOfMeasure toUOM = UnitOfMeasure.discrete(request.toUOMCode(), request.toUOMCode());

        BigDecimal result = uomManagementService.convert(request.sku(), request.quantity(),
                fromUOM, toUOM);

        return ResponseEntity.ok(result);
    }

    /**
     * Remove UOM conversion
     * DELETE /api/v1/inventory/uom/conversions/{sku}/{fromUOM}/{toUOM}
     */
    @DeleteMapping("/conversions/{sku}/{fromUOM}/{toUOM}")
    public ResponseEntity<Void> removeConversion(@PathVariable String sku,
                                                @PathVariable String fromUOM,
                                                @PathVariable String toUOM) {
        logger.info("Removing UOM conversion: SKU={}, {} → {}", sku, fromUOM, toUOM);

        UnitOfMeasure fromUnitOfMeasure = UnitOfMeasure.discrete(fromUOM, fromUOM);
        UnitOfMeasure toUnitOfMeasure = UnitOfMeasure.discrete(toUOM, toUOM);

        uomManagementService.removeConversion(sku, fromUnitOfMeasure, toUnitOfMeasure);

        return ResponseEntity.ok().build();
    }

    private UnitOfMeasure createUOM(String code, String description, String type, int decimalPrecision) {
        UOMType uomType = UOMType.valueOf(type);
        return switch (uomType) {
            case DISCRETE -> UnitOfMeasure.discrete(code, description);
            case WEIGHT -> UnitOfMeasure.weight(code, description, decimalPrecision);
            case VOLUME -> UnitOfMeasure.volume(code, description, decimalPrecision);
            case LENGTH -> UnitOfMeasure.length(code, description, decimalPrecision);
        };
    }
}
