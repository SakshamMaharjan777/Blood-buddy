package com.bloodbuddy.dto;

import com.bloodbuddy.model.Donor;
import com.bloodbuddy.model.User;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * One row of the admin user-management table
 * (proposal Appendix B: {@code GET /api/admin/users}).
 *
 * <p>Component names match what {@code admin-dashboard.html} reads
 * ({@code id, name, email, role, blood, district, joined, status}) and the
 * status values are its own labels, {@code "Active"} / {@code "Suspended"}
 * ({@code STATUS_META} keys in that page).
 *
 * <p>{@code id} is a STRING because the page calls {@code u.id.toLowerCase()}
 * while searching — a numeric id would throw at runtime in real mode.
 */
public record UserAdminResponse(
        String id,
        String name,
        String email,
        String role,
        String blood,
        String district,
        String joined,
        String status
) {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    public static UserAdminResponse of(User u, Donor donor) {
        return new UserAdminResponse(
                String.valueOf(u.getUserId()),
                u.getName(),
                u.getEmail(),
                AccountResponse.label(u.getRole()),
                donor == null || donor.getBloodGroup() == null ? "—" : donor.getBloodGroup().getLabel(),
                u.getDistrict() == null || u.getDistrict().isBlank() ? "—" : u.getDistrict(),
                u.getCreatedAt() == null ? null : DAY.format(u.getCreatedAt()),
                u.getStatus() == null ? null : titleCase(u.getStatus().name()));
    }

    /** UserStatus "ACTIVE" → the dashboard's label "Active". */
    public static String titleCase(String raw) {
        return raw.charAt(0) + raw.substring(1).toLowerCase();
    }
}
