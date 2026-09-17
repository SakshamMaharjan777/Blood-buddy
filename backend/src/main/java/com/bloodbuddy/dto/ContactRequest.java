package com.bloodbuddy.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/contact} — the site's contact form.
 *
 * <p>There is no CONTACT table in the proposal ERD and nothing is persisted: the
 * endpoint validates, logs and acknowledges. Keeping it explicit is deliberate —
 * a form that silently accepts a message nobody will ever read is worse than a
 * form that says so. Routing the message into the §2.4 mail pipeline is a B4
 * decision, not an accident.
 */
public record ContactRequest(
        @NotBlank String name,
        @NotBlank String email,
        @NotBlank String message
) {
}
