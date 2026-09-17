package com.bloodbuddy.repository;

import com.bloodbuddy.model.Donor;
import com.bloodbuddy.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Hospital} (§2.3). The demo's hospital
 * *labels* ("TUTH, Maharajgunj") become rows; the alias table in bb-store.js
 * retires once the frontend submits hospital ids.
 */
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    /** Resolve a submitted label ("Bir Hospital, Kathmandu") back to a row. */
    Optional<Hospital> findByNameIgnoreCaseAndAreaIgnoreCase(String name, String area);

    Optional<Hospital> findByNameIgnoreCase(String name);

    /** "TUTH" → the Tribhuvan University Teaching Hospital row. */
    Optional<Hospital> findByShortNameIgnoreCase(String shortName);

    Optional<Hospital> findByShortNameIgnoreCaseAndAreaIgnoreCase(String shortName, String area);

    /** Hospitals in a district (request form's hospital dropdown). */
    List<Hospital> findByDistrictIgnoreCase(String district);

    List<Hospital> findAllByOrderByNameAsc();

    /* --- the Donor ↔ Hospital affiliation, read from the HOSPITAL side -------
       The inverse direction of the M:N (the owning side is
       Donor.affiliatedHospitals). Queried rather than walked through
       Hospital.getDonors(), for two reasons: that collection is lazy and every
       caller here runs outside a Hibernate session (open-in-view is off), and a
       join answers "how many affiliated donors does this hospital have?" without
       hydrating the whole set to count it. */

    /** The donors affiliated with one hospital. */
    @Query("select d from Donor d join d.affiliatedHospitals h where h.hospitalId = :hospitalId order by d.donorId")
    List<Donor> findAffiliatedDonors(@Param("hospitalId") Long hospitalId);

    /**
     * The same as a count — the hospital portal's donor-base figure.
     *
     * <p>An explicit query rather than a derived name: {@code affiliatedHospitals}
     * is a property of {@code Donor}, not of {@code Hospital}, so
     * {@code countByAffiliatedHospitals_...} cannot be derived from this interface
     * (it fails at context startup with "No property 'affiliatedHospitals' found
     * for type 'Hospital'"). The join above says the same thing in a way the
     * repository actually owns.
     */
    @Query("select count(d) from Donor d join d.affiliatedHospitals h where h.hospitalId = :hospitalId")
    long countAffiliatedDonors(@Param("hospitalId") Long hospitalId);
}
