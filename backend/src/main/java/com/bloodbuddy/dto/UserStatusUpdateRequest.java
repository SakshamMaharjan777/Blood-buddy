package com.bloodbuddy.dto;

/**
 * Body of {@code PATCH /api/admin/users/{id}} — the suspend / restore toggle.
 * Accepts the dashboard's labels ("Active"/"Suspended") as well as the enum
 * names ("ACTIVE"/"SUSPENDED"); anything else is rejected by the service.
 */
public record UserStatusUpdateRequest(
        String status
) {
}
