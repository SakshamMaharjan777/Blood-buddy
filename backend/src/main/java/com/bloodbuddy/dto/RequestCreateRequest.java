package com.bloodbuddy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/requests} (proposal Appendix B) — the request form
 * AND the public emergency form (which sets {@code emergency = true} for a
 * guest {@code EM-} request).
 *
 * <p>{@code hospital} is the label the form submitted ("TUTH, Maharajgunj",
 * "Other", or "—" for unrouted); {@code hospitalId} is the authoritative form
 * when the client already knows the row. The service resolves the label through
 * the {@code HospitalRepository} and stores it verbatim for display.
 *
 * <p>{@code id} is an OPTIONAL client-supplied public code. The service always
 * generates one when it is absent; accepting it lets Postman and the QA harness
 * pin a code they can look up afterwards. Uniqueness is enforced either way.
 *
 * <p>The requester is NOT in this body: for members it is the authenticated
 * account (B6), passed to the service as a user id. Guests send
 * {@code emergency = true} and a {@code contactPhone} instead (BR-8).
 *
 * <p>Three fields the pages DO send are deliberately not modelled here, because
 * the server owns them (CPJ119 §2.2): {@code requester} (a display name — the
 * session decides), {@code compatOk} (recomputed from the Appendix A chart) and
 * {@code timeline} (the server's audit trail). Jackson ignores the unknown
 * properties, so the existing page code keeps working unchanged.
 */
public record RequestCreateRequest(
        String id,
        boolean emergency,
        @NotBlank String blood,
        @Min(1) @Max(4) int units,
        String urgency,
        /**
         * ISO date ("2026-09-20"). A STRING, not a {@code LocalDate}, because two
         * pages genuinely submit a blank one (the search modal's direct request
         * has no date field) — Jackson would reject {@code ""} with a generic 400
         * before the rule could explain itself. The service parses it and answers
         * 409 with "a needed-by date is required", which is the message the rule
         * actually has.
         */
        String neededBy,
        String hospital,
        Long hospitalId,
        @NotBlank String district,
        String notes,
        String contactPhone
) {
}
