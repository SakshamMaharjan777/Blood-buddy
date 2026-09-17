package com.bloodbuddy.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /api/auth/forgot} (the reset-request form). */
public record ForgotRequest(@NotBlank String email) {
}
