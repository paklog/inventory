package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.ABCCriteria;

import java.math.BigDecimal;

public record ClassifySKURequest(
    String sku,
    BigDecimal annualUsageValue,
    int annualUsageQuantity,
    BigDecimal unitCost,
    double velocityScore,
    double criticalityScore,
    ABCCriteria criteria
) {}
