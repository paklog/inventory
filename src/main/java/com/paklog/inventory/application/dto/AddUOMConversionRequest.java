package com.paklog.inventory.application.dto;

import java.math.BigDecimal;

public record AddUOMConversionRequest(
    String sku,
    String fromUOMCode,
    String fromUOMDescription,
    String fromUOMType,
    int fromUOMDecimalPrecision,
    String toUOMCode,
    String toUOMDescription,
    String toUOMType,
    int toUOMDecimalPrecision,
    BigDecimal conversionFactor,
    boolean reversible
) {}
