package com.paklog.inventory.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// Using a record for a simple, immutable DTO
public record UpdateStockLevelRequest(
    @NotNull
    Integer quantityChange,

    @NotEmpty
    String reasonCode,

    String comment
) {}
