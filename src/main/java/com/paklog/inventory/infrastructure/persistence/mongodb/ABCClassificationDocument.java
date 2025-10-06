package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.ABCClass;
import com.paklog.inventory.domain.model.ABCClassification;
import com.paklog.inventory.domain.model.ABCCriteria;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Embedded document for ABC classification within ProductStockDocument
 */
public class ABCClassificationDocument {

    private String abcClass; // ABCClass enum as string
    private String classificationCriteria; // ABCCriteria enum as string
    private LocalDate classifiedOn;
    private LocalDate validUntil;
    private BigDecimal annualUsageValue;
    private int annualUsageQuantity;
    private BigDecimal unitCost;
    private double velocityScore;
    private double criticalityScore;

    public ABCClassificationDocument() {
    }

    public static ABCClassificationDocument fromDomain(ABCClassification classification) {
        ABCClassificationDocument doc = new ABCClassificationDocument();
        doc.abcClass = classification.getAbcClass().name();
        doc.classificationCriteria = classification.getClassificationCriteria().name();
        doc.classifiedOn = classification.getClassifiedOn();
        doc.validUntil = classification.getValidUntil();
        doc.annualUsageValue = classification.getAnnualUsageValue();
        doc.annualUsageQuantity = classification.getAnnualUsageQuantity();
        doc.unitCost = classification.getUnitCost();
        doc.velocityScore = classification.getVelocityScore();
        doc.criticalityScore = classification.getCriticalityScore();
        return doc;
    }

    public ABCClassification toDomain(String sku) {
        return ABCClassification.load(
            sku,
            ABCClass.valueOf(abcClass),
            ABCCriteria.valueOf(classificationCriteria),
            classifiedOn,
            validUntil,
            annualUsageValue,
            annualUsageQuantity,
            unitCost,
            velocityScore,
            criticalityScore
        );
    }

    // Getters and setters
    public String getAbcClass() {
        return abcClass;
    }

    public void setAbcClass(String abcClass) {
        this.abcClass = abcClass;
    }

    public String getClassificationCriteria() {
        return classificationCriteria;
    }

    public void setClassificationCriteria(String classificationCriteria) {
        this.classificationCriteria = classificationCriteria;
    }

    public LocalDate getClassifiedOn() {
        return classifiedOn;
    }

    public void setClassifiedOn(LocalDate classifiedOn) {
        this.classifiedOn = classifiedOn;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public BigDecimal getAnnualUsageValue() {
        return annualUsageValue;
    }

    public void setAnnualUsageValue(BigDecimal annualUsageValue) {
        this.annualUsageValue = annualUsageValue;
    }

    public int getAnnualUsageQuantity() {
        return annualUsageQuantity;
    }

    public void setAnnualUsageQuantity(int annualUsageQuantity) {
        this.annualUsageQuantity = annualUsageQuantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public double getVelocityScore() {
        return velocityScore;
    }

    public void setVelocityScore(double velocityScore) {
        this.velocityScore = velocityScore;
    }

    public double getCriticalityScore() {
        return criticalityScore;
    }

    public void setCriticalityScore(double criticalityScore) {
        this.criticalityScore = criticalityScore;
    }
}
