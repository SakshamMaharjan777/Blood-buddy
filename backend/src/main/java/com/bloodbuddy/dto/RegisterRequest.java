package com.bloodbuddy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/auth/register} (proposal Appendix B). Field names
 * follow what the register page already posts, so B3 is a mapping and not a
 * frontend rewrite: {@code name, email, role, blood, district} plus the
 * role-specific extras.
 *
 * <p>{@code role} arrives as the form's lowercase string
 * ({@code donor} / {@code requester} / {@code hospital}) — {@code admin} is
 * deliberately rejected by the service: administrator accounts are provisioned,
 * never self-registered.
 *
 * <p>{@code photo} is the §2.5 Base64 data URL from the profile/register photo
 * picker; the service unwraps it to raw bytes for the {@code donors.photo} BLOB
 * (never a file path).
 *
 * <p>Since the real-mode flip (B7) {@code auth-register.html} sends {@code password}
 * (required here, min 8, BCrypt-hashed — BR-9) and, for hospital staff, the hospital
 * {@code name} it collected. Nothing in the demo store reaches this DTO any more.
 */
public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String role,
        String phone,
        String district,
        /** Donors: the 8-group label ("B+", "AB-"). */
        String blood,
        /** Donors: the neighbourhood shown on donor cards ("Baneshwor, Kathmandu"). */
        String area,
        /** Hospital staff: the hospital they belong to (row id). */
        Long hospitalId,
        /**
         * Hospital staff: the hospital they belong to, as the NAME the form
         * actually collects ("TUTH", "Bir Hospital, Kathmandu"). Resolved through
         * the same {@code HospitalResolver} the request flow uses, so the two can
         * never disagree about which row a name means. Either field may be sent;
         * the id wins when both are present, and a name that matches no partner
         * hospital leaves the account unlinked (with a server-side warning) rather
         * than failing a registration over a typo.
         */
        String hospital,
        /** §2.5 Base64 data URL (data:image/png;base64,…), optional. */
        String photo
) {
}
