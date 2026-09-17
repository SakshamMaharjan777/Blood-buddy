package com.bloodbuddy.dto;

/**
 * The one row the adjust-stock modal just wrote —
 * {@code PUT /api/hospitals/inventory/{bloodGroup}} answers
 * {@code { bloodGroup, units }}, which is exactly what the demo's mock returned,
 * so the page's save path needs no change when MOCK flips.
 */
public record InventoryRow(String bloodGroup, int units) {
}
