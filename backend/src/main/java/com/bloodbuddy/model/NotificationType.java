package com.bloodbuddy.model;

/**
 * Email notification types (proposal §1.3 objective 2, §2.4).
 * REGISTRATION is the CPJ119-mandatory notification; URGENT_REQUEST is the
 * proposal-level donor alert.
 */
public enum NotificationType {
    REGISTRATION,
    URGENT_REQUEST,
    /**
     * A request the recipient is involved in changed state (matched, fulfilled) —
     * added in B2 so the accept/fulfil hooks do not have to mislabel an update as
     * an "urgent request" alert. §2.4 only mandates REGISTRATION; this one is the
     * proposal-level courtesy mail.
     */
    STATUS_UPDATE,
    /**
     * The "reset your password" mail behind {@code POST /api/auth/forgot}
     * (B2 queues the audit row; the link that actually resets anything is B6/B4).
     */
    PASSWORD_RESET
}
