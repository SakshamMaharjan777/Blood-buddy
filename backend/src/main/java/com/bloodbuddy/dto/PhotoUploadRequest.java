package com.bloodbuddy.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/donors/{id}/photo} (CPJ119 §2.5 — images live in the
 * database, never as static files).
 *
 * <p>{@code dataUrl} is the Base64 data URL the profile pages already produce
 * from a FileReader ("data:image/png;base64,iVBORw0…"); the service unwraps it
 * to raw bytes for the {@code donors.photo} MEDIUMBLOB column and rejects
 * anything that is not a JPEG/PNG data URL.
 */
public record PhotoUploadRequest(
        @NotBlank String dataUrl
) {
}
