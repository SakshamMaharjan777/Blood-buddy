package com.bloodbuddy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A partner hospital / blood bank (report Table 3-2, hospitalId row): one of
 * the five partner institutions or any other hospital a request names.
 *
 * The demo's hospital *labels* ("TUTH, Maharajgunj", "Bir Hospital, Kathmandu",
 * … — bb-store.js's alias table) map onto rows of this table; the label format
 * is "{name}, {area}", which is what getLabel() reconstructs for display.
 */
@Entity
@Table(name = "hospitals")
@Getter @Setter @NoArgsConstructor
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hospitalId;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * The short name people actually type and the forms submit ("TUTH", "Bir
     * Hospital", "NMC"). B2's label resolver matches against this as well as
     * {@link #name}, so the demo's short labels keep resolving to the right row
     * — the server-side replacement for {@code bb-store.js}'s alias table.
     */
    @Size(max = 60)
    @Column(length = 60)
    private String shortName;

    /** Facility type (Teaching hospital, Community hospital, … — profile editor). */
    @Size(max = 60)
    @Column(length = 60)
    private String facilityType;

    @NotBlank
    @Size(max = 60)
    @Column(name = "area", nullable = false, length = 60)
    private String area;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String district;

    /** Opening hours text (profile editor). */
    @Size(max = 60)
    @Column(length = 60)
    private String openingHours;

    /** The blood-bank-open toggle from the hospital profile editor. */
    @NotNull
    @Column(nullable = false)
    private boolean bloodBankOpen = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Per-group stock rows (Hospital → BloodInventory 1:N, one row per blood group). */
    @OneToMany(mappedBy = "hospital", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BloodInventory> inventory = new ArrayList<>();

    /** Requests forwarded/routed into this hospital's queue (Table 3-1, 1:N). */
    @OneToMany(mappedBy = "hospital", fetch = FetchType.LAZY)
    private List<BloodRequest> queuedRequests = new ArrayList<>();

    /**
     * The INVERSE side of the Donor ↔ Hospital affiliation M:N (the owning side is
     * {@code Donor.affiliatedHospitals}, which declares the
     * {@code donor_hospital_affiliation} join table — see that field for why the
     * relationship needs a table of its own).
     *
     * <p>No cascade and no orphanRemoval on purpose: an affiliation is a link, not
     * a parent-child ownership. Deleting a hospital should remove its join rows,
     * not delete the donors who were registered with it.
     */
    @ManyToMany(mappedBy = "affiliatedHospitals", fetch = FetchType.LAZY)
    private Set<Donor> donors = new LinkedHashSet<>();

    /** "TUTH, Maharajgunj" — the label format the demo's request rows carry. */
    public String getLabel() {
        return name + ", " + area;
    }
}
