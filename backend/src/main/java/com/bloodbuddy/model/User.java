package com.bloodbuddy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A platform account (report Table 3-2, userId row): any of the four roles.
 *
 * The abstract-User-inheritance shape of the class diagram (Figure 3-2) is
 * realized as a single users table with a role discriminator — one FK target
 * for Donor/User (1:1), BloodRequest/User (1:N) and EmailNotification/User
 * (1:N), which keeps every relationship from Table 3-1 intact.
 */
@Entity
@Table(name = "users",
       uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter @Setter @NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Email
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String email;

    /** BCrypt hash from B6 — never the raw password, never serialized (BR-9). */
    @Column(nullable = false)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(length = 20)
    private String phone;

    /** The 5-digit Nepali district (Kathmandu, Lalitpur, …). */
    @Column(length = 60)
    private String district;

    /**
     * Street address from the requester/admin profile editors. Not part of the
     * proposal ERD, which only models district; added in B2 because those two
     * forms have an address field and echoing it back unsaved would be a lie
     * (donor/hospital addresses live on their own {@code area} column).
     */
    @Size(max = 200)
    @Column(length = 200)
    private String address;

    /**
     * Requester profile: the hospital they usually request through. A label, not
     * an FK — it is a form preference (the ERD models hospital links for staff
     * and requests, not for requesters preferring one).
     */
    @Size(max = 150)
    @Column(length = 150)
    private String preferredHospital;

    /** Requester profile: "email me about urgent requests near me". */
    @Column(nullable = false)
    private boolean urgentAlerts = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * The hospital this account staffs (HOSPITAL role only — BR-5: a hospital's
     * staff may write only that hospital's inventory and queue). Not part of the
     * proposal ERD; the operational link that lets B6 scope a staff session to
     * one hospitalId.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staffed_hospital_id")
    private Hospital staffedHospital;

    /**
     * The one Donor extension of this account (null for the other three roles).
     *
     * <p>{@code orphanRemoval} was REMOVED here in B2, and the reason is worth
     * keeping: this is the non-owning side of the 1:1 ({@code mappedBy}), and
     * orphan removal on that side means "saving a User whose {@code donor} field
     * is null deletes the donor row". That is exactly what happened —
     * {@code ProfileService.save} persists the User after the Donor, the
     * in-memory {@code donor} field had never been loaded, and Hibernate deleted
     * the just-saved Donor (found by the B2 smoke run: the account survived the
     * profile save with no donor row and the later delete matched nothing).
     * {@code cascade = ALL} stays, so deleting an account still deletes its donor.
     */
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Donor donor;

    /** Requests this user owns (the requester side; empty for admins/hospitals). */
    @OneToMany(mappedBy = "requester", fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<BloodRequest> requests = new ArrayList<>();

    /** Notifications sent to this user. */
    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<EmailNotification> notifications = new ArrayList<>();
}
