package com.bloodbuddy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A blood request (report Table 3-2, requestId row; the demo's bb_db.requests
 * rows). Two ownership FKs, per Table 3-1:
 *   User → BloodRequest 1:N  (the requester owns the request)
 *   Hospital → BloodRequest 1:N (the hospital whose queue it sits in once
 *                                 forwarded/admin-routed; nullable until then)
 * and one optional match:
 *   Donor → BloodRequest 1:N (BR-3: at most one donor stamps the accept).
 *
 * Public identity: member submissions get a "BB-XXXXXX" code, guest emergency
 * submissions an "EM-XXXXXX" code — the id the demo displays everywhere. The
 * lookup guards (BR-8: guest lookups expose EM- codes only) run on this code.
 */
@Entity
@Table(name = "blood_requests",
       uniqueConstraints = @UniqueConstraint(name = "uk_request_public_code", columnNames = "publicCode"))
@Getter @Setter @NoArgsConstructor
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    /** Human-readable id ("BB-5MN8VX", "EM-K3P9QA") — what the UI shows. */
    @NotBlank
    @Size(max = 12)
    @Column(name = "publicCode", nullable = false, length = 12)
    private String publicCode;

    /** True only for guest emergency submissions (EM- codes, public lookup allowed). */
    @Column(nullable = false)
    private boolean guest = false;

    /** The requester. Guests have no account — nullable, guarded by {@link #guest}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id")
    private User requester;

    /** The hospital whose queue holds the request (null until routed/forwarded). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    /** The matched donor (null until a donor accepts — BR-3 single match). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id")
    private Donor donor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private BloodGroup bloodGroup;

    /** Units requested, 1–4 (frontend cap). */
    @Min(1) @Max(4)
    @Column(nullable = false)
    private int units = 1;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgency urgency = Urgency.NORMAL;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    /** True when the recipient group accepts the requested group as compatible
     *  (the demo stores compatOk at submit time; the service recomputes it). */
    @Column(nullable = false)
    private boolean compatOk = false;

    /** When the blood is needed (frontend requires a future date at submit). */
    @NotNull
    private LocalDate neededBy;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String district;

    /** Free-text hospital label as submitted ("Other" hospital on the form). */
    @Size(max = 150)
    @Column(length = 150)
    private String hospitalLabel;

    /** Guest contact phone (emergency form). Members' contact lives on User. */
    @Size(max = 20)
    @Column(length = 20)
    private String contactPhone;

    @Size(max = 500)
    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** When a donor accepted (the demo's donorAt). */
    private Instant matchedAt;

    /** When the request was fulfilled/closed (demo's fulfilledAt). */
    private Instant fulfilledAt;

    /** Lifecycle audit trail — ordered, appended by the service on every transition. */
    @ElementCollection
    @CollectionTable(name = "request_timeline", joinColumns = @JoinColumn(name = "request_id"))
    @OrderColumn(name = "seq")
    private List<RequestTimelineEntry> timeline = new ArrayList<>();

    /** One line of the request's history (demo timeline entries). */
    @Embeddable
    @Getter @Setter @NoArgsConstructor
    public static class RequestTimelineEntry {
        /** When the entry happened (column named occurredAt — "when"/"at" are SQL keywords). */
        @Column(name = "occurred_at", nullable = false)
        private Instant occurredAt;

        @Column(nullable = false, length = 200)
        private String what;

        public RequestTimelineEntry(Instant occurredAt, String what) {
            this.occurredAt = occurredAt;
            this.what = what;
        }
    }
}
