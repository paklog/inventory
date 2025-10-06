package com.paklog.inventory.application.dto;

import com.paklog.inventory.domain.model.HoldType;

import java.time.LocalDateTime;

public record PlaceHoldRequest(
    String sku,
    HoldType holdType,
    int quantity,
    String reason,
    String placedBy,
    LocalDateTime expiresAt
) {}
