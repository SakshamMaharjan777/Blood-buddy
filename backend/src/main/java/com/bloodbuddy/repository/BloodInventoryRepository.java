package com.bloodbuddy.repository;

import com.bloodbuddy.model.BloodGroup;
import com.bloodbuddy.model.BloodInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link BloodInventory} (§2.3). Hospital-scoped
 * by construction (BR-5: a hospital's staff may write only that hospital's
 * stock) — every query takes the hospital, matching the §2.6 endpoint shape
 * /api/hospitals/{hospitalId}/inventory.
 */
public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long> {

    /** One hospital's whole board (8 rows, one per group). */
    List<BloodInventory> findByHospital_HospitalIdOrderByBloodGroupAsc(Long hospitalId);

    /** The one row a stock adjust / fulfilment decrement targets. */
    Optional<BloodInventory> findByHospital_HospitalIdAndBloodGroup(Long hospitalId, BloodGroup bloodGroup);

    /** Platform-wide count for one group (admin analytics). */
    List<BloodInventory> findByBloodGroup(BloodGroup bloodGroup);
}
