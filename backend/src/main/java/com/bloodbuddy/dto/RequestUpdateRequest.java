package com.bloodbuddy.dto;

import java.util.List;

/**
 * Body of {@code PATCH /api/requests/{id}} — the demo's generic update. In mock
 * mode the pages write whatever their optimistic UI needs
 * ({@code status}, {@code donor}, {@code donorAt}, {@code declinedBy},
 * {@code timeline}); the real endpoint must NOT trust that shape, so B3 routes
 * this DTO through {@code RequestService}'s explicit, business-rule-checked
 * operations instead of writing fields.
 *
 * <p>Two things are deliberately ignored, and that is the point of B2:
 * <ul>
 *   <li>{@code timeline} — the client's copy is optimistic display data; the
 *       server appends its own entries on every transition. Accepting the
 *       client's would let a caller rewrite history.</li>
 *   <li>{@code donor} (a display name) — matching a donor needs an id. The
 *       donor is taken from the authenticated session; when neither
 *       {@code donorId} nor a session donor exists the service REFUSES the
 *       accept rather than guessing which "Arun Shrestha" meant it.</li>
 * </ul>
 *
 * <p>{@code declinedBy} is the demo's decline shape (a list of names appended to
 * the request). The real behaviour is BR-7: a {@code RequestDecline} row for the
 * signed-in donor, and the request itself untouched — so the field is only used
 * to recognise the intent.
 */
public record RequestUpdateRequest(
        /** Target demo label: "Pending", "Matched"/"Accepted", "Fulfilled", "Cancelled", "Rejected". */
        String status,
        /** Hospital label the request is forwarded to ("TUTH, Maharajgunj"). */
        String hospital,
        Long hospitalId,
        /** Display name from the optimistic UI (unused for matching — see above). */
        String donor,
        Long donorId,
        /** Demo decline marker; presence without a status means "this donor declines". */
        List<String> declinedBy,
        /** Requester-authored note (the only free-text field a patch may change). */
        String notes,
        String contactPhone,
        /** Ignored: the server owns the audit trail. */
        List<RequestResponse.TimelineEntry> timeline
) {
}
