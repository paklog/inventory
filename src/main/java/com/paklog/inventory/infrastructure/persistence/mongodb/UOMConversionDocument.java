package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.UOMConversion;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/**
 * MongoDB document for UOM conversions.
 * Stores conversion factors between units of measure for a SKU.
 */
@Document(collection = "uom_conversions")
@CompoundIndexes({
    @CompoundIndex(name = "sku_uoms_idx", def = "{'sku': 1, 'fromUOM.code': 1, 'toUOM.code': 1}", unique = true),
    @CompoundIndex(name = "sku_idx", def = "{'sku': 1}")
})
public class UOMConversionDocument {

    @Id
    private String id;
    private String sku;  // SKU this conversion applies to
    private UnitOfMeasureDocument fromUOM;
    private UnitOfMeasureDocument toUOM;
    private BigDecimal conversionFactor;
    private boolean reversible;

    public UOMConversionDocument() {
    }

    public UOMConversionDocument(String sku, UnitOfMeasureDocument fromUOM,
                                UnitOfMeasureDocument toUOM, BigDecimal conversionFactor,
                                boolean reversible) {
        this.sku = sku;
        this.fromUOM = fromUOM;
        this.toUOM = toUOM;
        this.conversionFactor = conversionFactor;
        this.reversible = reversible;
    }

    public static UOMConversionDocument fromDomain(String sku, UOMConversion conversion) {
        return new UOMConversionDocument(
            sku,
            UnitOfMeasureDocument.fromDomain(conversion.getFromUOM()),
            UnitOfMeasureDocument.fromDomain(conversion.getToUOM()),
            conversion.getConversionFactor(),
            conversion.isReversible()
        );
    }

    public UOMConversion toDomain() {
        if (reversible) {
            return UOMConversion.twoWay(fromUOM.toDomain(), toUOM.toDomain(), conversionFactor);
        } else {
            return UOMConversion.oneWay(fromUOM.toDomain(), toUOM.toDomain(), conversionFactor);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public UnitOfMeasureDocument getFromUOM() {
        return fromUOM;
    }

    public void setFromUOM(UnitOfMeasureDocument fromUOM) {
        this.fromUOM = fromUOM;
    }

    public UnitOfMeasureDocument getToUOM() {
        return toUOM;
    }

    public void setToUOM(UnitOfMeasureDocument toUOM) {
        this.toUOM = toUOM;
    }

    public BigDecimal getConversionFactor() {
        return conversionFactor;
    }

    public void setConversionFactor(BigDecimal conversionFactor) {
        this.conversionFactor = conversionFactor;
    }

    public boolean isReversible() {
        return reversible;
    }

    public void setReversible(boolean reversible) {
        this.reversible = reversible;
    }
}
