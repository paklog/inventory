package com.paklog.inventory.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request DTO for setting absolute stock quantity.
 * Used for physical counts and inventory corrections.
 */
public record SetStockLevelRequest(
    @NotNull
    @Min(0)
    Integer quantity,

    @NotEmpty
    String reasonCode,

    String comment,

    String locationId,

    LocalDateTime countDate,

    // Source system tracking
    String sourceSystem,
    String sourceTransactionId,
    String sourceOperatorId
) {}
