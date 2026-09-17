package com.bloodbuddy.dto;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Body of {@code PUT /api/hospitals/inventory/{bloodGroup}} — the adjust-stock
 * modal, which sets an ABSOLUTE value (not a delta) for one group of exactly one
 * hospital: the hospital comes from the authenticated staff account (BR-5), never
 * from the body.
 */
public record InventoryUpdateRequest(
        @PositiveOrZero int units
) {
}
