package com.paklog.inventory.application.dto;

import java.math.BigDecimal;

public record ConvertQuantityRequest(
    String sku,
    BigDecimal quantity,
    String fromUOMCode,
    String toUOMCode
) {}
