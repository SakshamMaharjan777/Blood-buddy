package com.bloodbuddy.dto;

import com.bloodbuddy.model.Donor;
import com.bloodbuddy.model.Hospital;
import com.bloodbuddy.model.Role;
import com.bloodbuddy.model.User;

/**
 * The signed-in account as the frontend session needs it — the JSON stand-in
 * for the demo's {@code bb_session} ({@code { role, name }}) plus the ids B3/B6
 * need to scope requests. Never contains {@code passwordHash} (BR-9).
 */
public record AccountResponse(
        String id,
        String name,
        String email,
        /** Demo role string ("Donor"/"Requester"/"Hospital"/"Admin") — matches nav.js's ROLE_* tables. */
        String role,
        String district,
        String status,
        /** The Donor extension's id, for donors (else null). */
        String donorId,
        /** The staffed hospital, for hospital accounts (else null). */
        String hospitalId,
        String hospitalLabel
) {

    public static AccountResponse of(User u, Donor donor) {
        Hospital h = u.getStaffedHospital();
        return new AccountResponse(
                String.valueOf(u.getUserId()),
                u.getName(),
                u.getEmail(),
                label(u.getRole()),
                u.getDistrict(),
                u.getStatus() == null ? null : titleCase(u.getStatus().name()),
                donor == null ? null : String.valueOf(donor.getDonorId()),
                h == null ? null : String.valueOf(h.getHospitalId()),
                h == null ? null : h.getLabel());
    }

    /** Role enum → the frontend's role string ("Donor" … — nav.js ROLE_CTA/ROLE_LINKS keys). */
    public static String label(Role role) {
        return titleCase(role.name());
    }

    /** "ADMIN" → "Admin", "HOSPITAL" → "Hospital". */
    private static String titleCase(String raw) {
        return raw.charAt(0) + raw.substring(1).toLowerCase();
    }
}
