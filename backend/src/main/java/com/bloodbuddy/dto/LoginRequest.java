package com.bloodbuddy.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/auth/login} (proposal Appendix B). {@code role} is
 * the role chip the login page shows; the service checks it against the stored
 * account so a user who picks the wrong portal is told, instead of landing in a
 * dashboard their account cannot use.
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password,
        /** The portal picked on the form: "donor", "requester", "hospital", "admin" (nullable = any). */
        String role
) {
}
