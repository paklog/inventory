package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.StockStatus;

public record StockStatusChangeRequest(
    String sku,
    int quantity,
    StockStatus fromStatus,
    StockStatus toStatus,
    String reason
) {}
