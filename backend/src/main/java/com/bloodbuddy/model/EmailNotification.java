package com.bloodbuddy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Audit record for every notification email the system sends (report Table 3-2,
 * notificationId row). Registered users get a REGISTRATION confirmation (§2.4's
 * mandatory notification); compatible available donors get URGENT_REQUEST
 * alerts (proposal §1.3 objective 2).
 *
 * The row is written BEFORE delivery and its status updated after (B4), so an
 * SMTP failure is recorded, never silently dropped — and the table doubles as
 * the §2.4 evidence trail: SENT stamps sentAt, FAILED carries the server's own
 * error, and every delivered row keeps the body it sent.
 */
@Entity
@Table(name = "email_notifications")
@Getter @Setter @NoArgsConstructor
public class EmailNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    /** The user whose email address receives the mail (null for guest contacts). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private User recipient;

    /** For guest emergency notifications when no User row exists. */
    @Size(max = 255)
    @Column(length = 255)
    private String recipientEmail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    /** The request that triggered this notification (nullable for REGISTRATION mail). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private BloodRequest request;

    /** Denormalized subject line — keeps the audit row readable without the mail server. */
    @Size(max = 200)
    @Column(length = 200)
    private String subject;

    /**
     * The plain-text body actually sent (B4). The audit row keeps the WHOLE
     * message, not just its subject, so the §2.4 registration confirmation can be
     * read straight out of the database — no SMTP client, no mail server, no
     * screenshots required to prove what was sent to whom.
     */
    @Column(columnDefinition = "TEXT")
    private String body;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeliveryStatus deliveryStatus = DeliveryStatus.QUEUED;

    /** Last send attempt error, if any (FAILED rows). */
    @Column(length = 500)
    private String errorText;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant sentAt;
}
