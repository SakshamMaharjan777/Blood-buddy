package com.bloodbuddy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Role-scoped extension of a User account for donors (report Table 3-1:
 * "User → Donor, 1:1, a Donor profile is a role-scoped extension of a User
 * account"). Owns the §2.5 photograph as a real DB BLOB.
 */
@Entity
@Table(name = "donors")
@Getter @Setter @NoArgsConstructor
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long donorId;

    /**
     * The owning account. NOT optional: every Donor is an extension of a User.
     * The FK column lives on this side (the owning side of the 1:1); unique
     * enforces one Donor per User.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private BloodGroup bloodGroup;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String area;

    @NotNull
    @Column(nullable = false)
    private boolean available = true;

    /** Date of the most recent donation (drives eligibility later; nullable). */
    private LocalDate lastDonationDate;

    /**
     * §2.5 photograph bytes (JPG/PNG), stored IN the database as BLOB —
     * explicitly not a file path (CPJ119 §2.5). Uploaded via
     * POST /api/donors/{id}/photo and served back by GET …/photo (B3).
     * The frontend's Base64 data URL is unwrapped to raw bytes at the API edge.
     */
    @Lob
    @Column(name = "photo", columnDefinition = "MEDIUMBLOB")
    private byte[] photo;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * Requests this donor has been matched to (Donor → BloodRequest 1:N,
     * Table 3-1 — a donor may serve multiple requests; each request is served
     * by at most one donor, BR-3).
     */
    @OneToMany(mappedBy = "donor", fetch = FetchType.LAZY)
    private List<BloodRequest> matchedRequests = new ArrayList<>();

    /** Declines (BR-7): a decline hides a request for this donor only. */
    @OneToMany(mappedBy = "donor", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestDecline> declines = new ArrayList<>();

    /**
     * The blood banks this donor is registered with — <b>Donor ↔ Hospital, M:N</b>
     * (CPJ119 §2.3 asks for 1:1 / 1:N / M:N; this is the many-to-many, and the
     * proposal's §3.5 promised one too).
     *
     * <p>Neither side owns the other, which is what makes it M:N: one donor can
     * walk into several partner hospitals, and one hospital has many affiliated
     * donors, so the relationship cannot live as a FK on either table — it needs
     * its own. {@code donor_hospital_affiliation} is named rather than left to
     * Hibernate's generated {@code donors_hospitals}, because a generated name
     * says nothing about WHY the two rows are related.
     *
     * <p>This is the OWNING side (it declares the join table);
     * {@link Hospital#getDonors()} is the {@code mappedBy} inverse.
     *
     * <p>Deliberately NOT part of any business rule: BR-1 (who may donate to whom)
     * is blood group, and BR-5 (who may touch a queue) is the staff account's
     * hospital. An affiliation cannot change either, so adding this relationship
     * cannot move a rule. It is what the hospital portal counts as its donor base,
     * and what the donor's own page calls "the blood banks near you".
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "donor_hospital_affiliation",
            joinColumns = @JoinColumn(name = "donor_id"),
            inverseJoinColumns = @JoinColumn(name = "hospital_id"))
    private Set<Hospital> affiliatedHospitals = new LinkedHashSet<>();
}
