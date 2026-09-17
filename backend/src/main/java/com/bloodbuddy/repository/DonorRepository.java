package com.bloodbuddy.repository;

import com.bloodbuddy.model.BloodGroup;
import com.bloodbuddy.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Donor} (§2.3). The search-page filters
 * (blood group + district + availability) become derived queries here; the
 * widen-fallback ("no donors of this group in this district → show the group
 * anywhere") is service-layer logic on top (B2).
 */
public interface DonorRepository extends JpaRepository<Donor, Long> {

    Optional<Donor> findByUser_UserId(Long userId);

    /** The registry behind GET /api/donors/search (§2.6 / proposal Appendix B). */
    List<Donor> findByBloodGroupAndAvailableTrue(BloodGroup bloodGroup);

    /** Full filter: group + district + available (the search page's three chips). */
    List<Donor> findByBloodGroupAndAvailableTrueAndUser_DistrictIgnoreCase(
            BloodGroup bloodGroup, String district);

    /** Full filter ignoring availability (the search page's "available only" toggle is off). */
    List<Donor> findByBloodGroupAndUser_DistrictIgnoreCase(BloodGroup bloodGroup, String district);

    /** Widen fallback: this group, any district. */
    List<Donor> findByBloodGroup(BloodGroup bloodGroup);

    /** District filter ignoring availability. */
    List<Donor> findByUser_DistrictIgnoreCase(String district);

    List<Donor> findByAvailableTrue();

    /** District-only filter (no group chosen) — the search page allows that. */
    List<Donor> findByUser_DistrictIgnoreCaseAndAvailableTrue(String district);

    /**
     * BR-1 fan-out: every available donor whose group may donate to a recipient.
     * The caller passes {@code BloodGroup.getCompatibleDonorGroups()} of the
     * RECIPIENT group — the chart's own direction. Reading it backwards here is
     * the single most damaging mistake this codebase can make, so the parameter
     * is always the chart's output, never a donor's group.
     */
    List<Donor> findByBloodGroupInAndAvailableTrue(Collection<BloodGroup> bloodGroups);

    /** Donor-dashboard "requests near you" needs the donor row of the logged-in user. */
    Optional<Donor> findByUser_EmailIgnoreCase(String email);
}
