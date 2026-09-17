package com.bloodbuddy.dto;

/**
 * Response of {@code POST /api/auth/login}.
 *
 * <p>{@code token} is retained for wire compatibility — the demo's mock login
 * returns {@code { ok, token }} and pages may read it, so removing it would be a
 * frontend change for nothing. It carries no authority. The real session is the
 * {@code JSESSIONID} cookie the login response sets (B6): the server holds the
 * authenticated context, and every later request is identified by that cookie.
 */
public record LoginResponse(
        boolean ok,
        String token,
        AccountResponse user
) {
}
