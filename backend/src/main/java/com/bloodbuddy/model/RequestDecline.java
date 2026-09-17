package com.bloodbuddy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A donor's "no" to one request (business rule BR-7: a decline hides the
 * request for that donor only and never mutates the request). Not part of the
 * proposal's ER diagram — it is the persistence behind the donor dashboard's
 * Decline button, which session #10 added to the demo.
 */
@Entity
@Table(name = "request_declines",
       uniqueConstraints = @UniqueConstraint(name = "uk_decline_donor_request",
                                             columnNames = {"donor_id", "request_id"}))
@Getter @Setter @NoArgsConstructor
public class RequestDecline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long declineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private BloodRequest request;

    @Column(nullable = false, updatable = false)
    private Instant declinedAt = Instant.now();
}
