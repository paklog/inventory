package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.ValuationMethod;

import java.math.BigDecimal;

public record InitializeValuationRequest(
    String sku,
    ValuationMethod valuationMethod,
    BigDecimal initialUnitCost,
    String currency
) {}
