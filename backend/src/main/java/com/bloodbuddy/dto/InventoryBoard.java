package com.bloodbuddy.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A hospital's stock board (proposal Appendix B:
 * {@code GET /api/hospitals/inventory} and
 * {@code GET /api/hospitals/{hospitalId}/inventory}, BR-5).
 *
 * <p>{@code units} is keyed by blood-group label exactly as
 * {@code hospital-dashboard.html} iterates it
 * ({@code Object.keys(inv).forEach(g => INVENTORY[g] = inv[g])}), so B3 returns
 * {@link #units()} as the body. The labels live on the wrapper because the
 * dashboard header names the hospital the scoped board belongs to.
 */
public record InventoryBoard(
        String hospitalId,
        String hospitalLabel,
        /** Blood-group label ("O+") → units on hand. Always all 8 groups. */
        Map<String, Integer> units
) {

    public static InventoryBoard of(String hospitalId, String hospitalLabel, Map<String, Integer> units) {
        return new InventoryBoard(hospitalId, hospitalLabel, new LinkedHashMap<>(units));
    }
}
