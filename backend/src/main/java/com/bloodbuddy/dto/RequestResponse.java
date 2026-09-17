package com.bloodbuddy.dto;

import com.bloodbuddy.model.BloodRequest;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * One blood request as every queue renders it (tracker, hospital queue, admin
 * moderation, donor dashboard). Component names mirror the demo row
 * ({@code bb_db.requests}) because those are the keys the pages read today, so
 * flipping MOCK = false does not touch a page.
 *
 * <p>{@code status} is the canonical demo label (see
 * {@code RequestStatus.label()}): Pending / Matched / Fulfilled / Cancelled /
 * Rejected. Known nuance for B3/B7: the demo also printed "Accepted" (hospital
 * or admin took the request on) and "Forwarded" (routed to a hospital) — the B1
 * enum collapses both into {@code ACCEPTED}/{@code PENDING}, which is recorded
 * in {@code RequestStatus}'s javadoc.
 *
 * <p>{@code donor} carries the matched donor's name only; {@code timeline} is
 * the server's own audit trail, never the client's optimistic copy.
 */
public record RequestResponse(
        String id,
        String blood,
        int units,
        String urgency,
        String status,
        String neededBy,
        String district,
        /** Hospital label ("TUTH, Maharajgunj") or "—" while unrouted. */
        String hospital,
        String requester,
        String created,
        String donor,
        String donorAt,
        String fulfilledAt,
        boolean compatOk,
        boolean guest,
        String notes,
        String contactPhone,
        List<TimelineEntry> timeline
) {

    /** One audit line; {@code when} is pre-formatted for display, like the demo's strings. */
    public record TimelineEntry(String when, String what) {
    }

    /**
     * Instants are formatted in UTC to match how {@code DemoDataSeeder} builds
     * the pinned demo dates ("2026-09-12 09:24"), so a seeded row and a live row
     * read consistently. {@code created}/{@code donorAt}/{@code fulfilledAt} stay
     * ISO-8601 for the page's own date helpers.
     */
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    /** The label the queues show for an unrouted request. */
    public static final String NO_HOSPITAL = "—";

    public static RequestResponse of(BloodRequest r) {
        return new RequestResponse(
                r.getPublicCode(),
                r.getBloodGroup() == null ? "—" : r.getBloodGroup().getLabel(),
                r.getUnits(),
                r.getUrgency() == null ? null : r.getUrgency().label(),
                r.getStatus() == null ? null : r.getStatus().label(),
                r.getNeededBy() == null ? null : r.getNeededBy().toString(),
                r.getDistrict(),
                r.getHospitalLabel() == null || r.getHospitalLabel().isBlank()
                        ? NO_HOSPITAL : r.getHospitalLabel(),
                requesterLabel(r),
                r.getCreatedAt() == null ? null : r.getCreatedAt().toString(),
                r.getDonor() == null || r.getDonor().getUser() == null
                        ? null : r.getDonor().getUser().getName(),
                r.getMatchedAt() == null ? null : r.getMatchedAt().toString(),
                r.getFulfilledAt() == null ? null : r.getFulfilledAt().toString(),
                r.isCompatOk(),
                r.isGuest(),
                r.getNotes(),
                r.getContactPhone(),
                r.getTimeline() == null ? List.of() : r.getTimeline().stream()
                        .map(t -> new TimelineEntry(STAMP.format(t.getOccurredAt()), t.getWhat()))
                        .toList());
    }

    /** Guest requests have no account — the demo shows "Guest (emergency)". */
    public static String requesterLabel(BloodRequest r) {
        return r.getRequester() == null ? "Guest (emergency)" : r.getRequester().getName();
    }

}
