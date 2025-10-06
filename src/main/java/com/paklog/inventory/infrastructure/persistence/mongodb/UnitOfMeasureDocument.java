package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.UnitOfMeasure;
import com.paklog.inventory.domain.model.UOMType;

/**
 * Embedded document for UnitOfMeasure value object.
 */
public class UnitOfMeasureDocument {

    private String code;
    private String description;
    private String type;  // UOMType as string
    private int decimalPrecision;

    public UnitOfMeasureDocument() {
    }

    public UnitOfMeasureDocument(String code, String description, String type, int decimalPrecision) {
        this.code = code;
        this.description = description;
        this.type = type;
        this.decimalPrecision = decimalPrecision;
    }

    public static UnitOfMeasureDocument fromDomain(UnitOfMeasure uom) {
        return new UnitOfMeasureDocument(
            uom.getCode(),
            uom.getDescription(),
            uom.getType().name(),
            uom.getDecimalPrecision()
        );
    }

    public UnitOfMeasure toDomain() {
        UOMType uomType = UOMType.valueOf(type);
        return switch (uomType) {
            case DISCRETE -> UnitOfMeasure.discrete(code, description);
            case WEIGHT -> UnitOfMeasure.weight(code, description, decimalPrecision);
            case VOLUME -> UnitOfMeasure.volume(code, description, decimalPrecision);
            case LENGTH -> UnitOfMeasure.length(code, description, decimalPrecision);
        };
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getDecimalPrecision() {
        return decimalPrecision;
    }

    public void setDecimalPrecision(int decimalPrecision) {
        this.decimalPrecision = decimalPrecision;
    }
}
