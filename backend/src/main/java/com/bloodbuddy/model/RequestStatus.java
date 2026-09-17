package com.bloodbuddy.model;

/**
 * Request lifecycle statuses (report Table 3-2 requestId row, business rule BR-2).
 *
 * Allowed transitions (enforced by the request service in B2, listed here so
 * the states have one documented home):
 *
 *   PENDING ──admin forward──▶ ACCEPTED(=Matched) ──hospital fulfil──▶ FULFILLED
 *      │                            │
 *      ├──admin reject──▶ REJECTED  └──(future)──▶ CANCELLED by requester
 *      └──requester cancel──▶ CANCELLED
 *
 * The demo labels "Pending/Matched/Fulfilled/Cancelled/Rejected" map onto
 * PENDING/ACCEPTED/FULFILLED/CANCELLED/REJECTED.
 */
public enum RequestStatus {
    PENDING,
    ACCEPTED,   // demo label: "Matched"
    FULFILLED,
    CANCELLED,  // terminal (BR-2)
    REJECTED;   // terminal (BR-2)

    /**
     * The label every queue prints — the same strings {@code STATUS_META} keys
     * in the frontend, so the DTO layer (B2) does not invent a third vocabulary.
     */
    public String label() {
        return switch (this) {
            case PENDING -> "Pending";
            case ACCEPTED -> "Matched";
            case FULFILLED -> "Fulfilled";
            case CANCELLED -> "Cancelled";
            case REJECTED -> "Rejected";
        };
    }

    /**
     * Parse a label coming from a client (PATCH body, query filter). The demo
     * uses a few extra display words for the same states, deliberately kept
     * working here so the real endpoint accepts the pages' own payloads:
     *
     * <pre>
     * "Forwarded"  → PENDING   (routed to a hospital, still awaiting a donor)
     * "Accepted"   → ACCEPTED  (hospital/admin took it on; no donor attached)
     * "Completed"  → FULFILLED (the demo's older word)
     * </pre>
     *
     * @throws IllegalArgumentException for anything else — the caller turns that
     *         into a 400 rather than silently ignoring a bad transition.
     */
    public static RequestStatus fromLabel(String label) {
        String v = label == null ? "" : label.trim();
        return switch (v.toLowerCase()) {
            case "pending", "forwarded" -> PENDING;
            case "accepted", "matched" -> ACCEPTED;
            case "fulfilled", "completed" -> FULFILLED;
            case "cancelled", "canceled" -> CANCELLED;
            case "rejected" -> REJECTED;
            default -> throw new IllegalArgumentException("Unknown request status: " + label);
        };
    }

    /** True for the states BR-2 makes terminal (no transition out of them). */
    public boolean isTerminal() {
        return this == FULFILLED || this == CANCELLED || this == REJECTED;
    }
}
