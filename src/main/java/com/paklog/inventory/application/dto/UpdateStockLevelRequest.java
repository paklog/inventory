package com.paklog.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adjusting stock levels for a specific SKU.
 *
 * This request allows authorized users (e.g., Inventory Managers) to make
 * manual adjustments to the quantity on hand. The adjustment can be:
 * - Positive: Stock intake, found items, returns
 * - Negative: Damage, loss, cycle count corrections
 *
 * All adjustments are recorded with a reason code for audit trail purposes.
 * Corresponds to user story INV-08.
 *
 * @param quantity_change The delta to apply to quantity on hand (positive or negative)
 * @param reason_code Mandatory reason code for the adjustment (e.g., "damaged", "stock_intake")
 * @param comment Optional free-text explanation for the adjustment
 */
public record UpdateStockLevelRequest(
    @NotNull
    @JsonProperty("quantity_change")
    Integer quantityChange,

    @NotEmpty
    @JsonProperty("reason_code")
    String reasonCode,

    String comment
) {}
