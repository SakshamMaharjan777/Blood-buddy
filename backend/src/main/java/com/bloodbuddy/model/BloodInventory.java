package com.bloodbuddy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One hospital's stock of one blood group (report Table 3-1: "Hospital →
 * BloodInventory, 1:N, per-hospital stock rows, one per blood group"; data
 * dictionary row inventoryId). The demo's SEED_INVENTORY object (one map per
 * hospital) becomes one row per group here — which is also what makes
 * hospital-scoped endpoints (/api/hospitals/{hospitalId}/inventory, §2.6)
 * natural.
 *
 * BR-4: fulfilment decrements units for the request's blood group and never
 * below zero (CHECK-style guard enforced in the inventory service; the DB
 * column is unsigned-validated here).
 */
@Entity
@Table(name = "blood_inventory",
       uniqueConstraints = @UniqueConstraint(name = "uk_inventory_hospital_group",
                                             columnNames = {"hospital_id", "bloodGroup"}))
@Getter @Setter @NoArgsConstructor
public class BloodInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryId;

    /** The owning hospital (FK hospital_id). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "bloodGroup", nullable = false, length = 5)
    private BloodGroup bloodGroup;

    @PositiveOrZero
    @Column(nullable = false)
    private int units = 0;

    private Instant updatedAt = Instant.now();
}
