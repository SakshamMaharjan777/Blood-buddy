package com.bloodbuddy.service;

import com.bloodbuddy.dto.DonorResponse;
import com.bloodbuddy.dto.DonorSearchResult;
import com.bloodbuddy.dto.HospitalOption;
import com.bloodbuddy.dto.RequestResponse;
import com.bloodbuddy.model.BloodGroup;
import com.bloodbuddy.model.BloodRequest;
import com.bloodbuddy.model.Donor;
import com.bloodbuddy.model.Hospital;
import com.bloodbuddy.model.RequestStatus;
import com.bloodbuddy.repository.BloodRequestRepository;
import com.bloodbuddy.repository.DonorRepository;
import com.bloodbuddy.repository.RequestDeclineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The donor registry: search (proposal Appendix B {@code GET /api/donors/search}),
 * the §2.5 photo, and the BR-1 compatibility math the rest of the backend asks for.
 *
 * <p><b>This class is where the Appendix A chart is applied</b> — the frontend's
 * {@code bb-compat.js} becomes presentation-only once MOCK = false (CPJ119 §2.2:
 * critical logic in the backend). The chart is written recipient → compatible
 * DONOR groups and is used in exactly that direction here: a request's group
 * decides which donors may serve it, never the other way round.
 */
@Service
@RequiredArgsConstructor
public class DonorService {

    private final DonorRepository donors;
    private final BloodRequestRepository requests;
    private final RequestDeclineRepository declines;

    /* ------------------------------------------------- hospital affiliations */

    /**
     * The blood banks a donor is registered with — the Donor ↔ Hospital M:N read
     * from the donor's own side ({@link Donor#getAffiliatedHospitals()} explains
     * why the relationship needs its own table). Transactional because the
     * collection is lazy and {@code open-in-view} is off; sorted by name so the
     * list is stable across calls.
     */
    @Transactional(readOnly = true)
    public List<HospitalOption> affiliatedHospitals(Long donorId) {
        Donor d = donors.findById(donorId)
                .orElseThrow(() -> new NotFoundException("Unknown donor: " + donorId));
        return d.getAffiliatedHospitals().stream()
                .sorted(Comparator.comparing(Hospital::getName))
                .map(HospitalOption::of)
                .toList();
    }

    /* ------------------------------------------------------------------ search */

    /**
     * Donor search with the session #9 widen fallback, now server-side.
     *
     * @param blood         requested (RECIPIENT) group label, or blank for any
     * @param district      district filter, or blank for any
     * @param availableOnly {@code TRUE} = available donors only; {@code null} or
     *                      {@code FALSE} = no availability filter. Null means
     *                      "no filter" because the search page's first load is
     *                      {@code donors.search({})} — it wants the whole
     *                      registry and applies its own chips.
     * @return the hits plus (when the search had to be widened) the reason
     */
    @Transactional(readOnly = true)
    public DonorSearchResult search(String blood, String district, Boolean availableOnly) {
        BloodGroup group = parseGroup(blood);
        String area = blankToNull(district);
        boolean onlyAvailable = Boolean.TRUE.equals(availableOnly);

        List<Donor> hits;
        boolean widened = false;

        if (group != null && area != null) {
            hits = onlyAvailable
                    ? donors.findByBloodGroupAndAvailableTrueAndUser_DistrictIgnoreCase(group, area)
                    : donors.findByBloodGroupAndUser_DistrictIgnoreCase(group, area);
            if (hits.isEmpty() && onlyAvailable) {
                // Widen: no available donor of this group in this district is an
                // emergency dead end, so offer the group anywhere — but say so.
                hits = donors.findByBloodGroupAndAvailableTrue(group);
                widened = !hits.isEmpty();
            }
        } else if (group != null) {
            hits = onlyAvailable ? donors.findByBloodGroupAndAvailableTrue(group) : donors.findByBloodGroup(group);
        } else if (area != null) {
            hits = onlyAvailable
                    ? donors.findByUser_DistrictIgnoreCaseAndAvailableTrue(area)
                    : donors.findByUser_DistrictIgnoreCase(area);
        } else {
            hits = onlyAvailable ? donors.findByAvailableTrue() : donors.findAll();
        }

        List<DonorResponse> cards = hits.stream()
                .sorted(Comparator.comparing(Donor::isAvailable).reversed()
                        .thenComparing(d -> d.getUser() == null ? "" : d.getUser().getName()))
                .map(d -> DonorResponse.of(d, group))
                .toList();

        if (widened) {
            return DonorSearchResult.widened(cards,
                    "No available " + group.getLabel() + " donors in " + area
                            + " — showing " + group.getLabel() + " donors in all districts.");
        }
        return DonorSearchResult.direct(cards);
    }

    @Transactional(readOnly = true)
    public DonorResponse get(Long donorId, BloodGroup recipient) {
        return DonorResponse.of(require(donorId), recipient);
    }

    /** The donor profile of a signed-in account (null when the account has no donor row). */
    @Transactional(readOnly = true)
    public Donor findByEmail(String email) {
        return email == null ? null : donors.findByUser_EmailIgnoreCase(email).orElse(null);
    }

    /* ----------------------------------------------------- BR-1 fan-out (one home) */

    /**
     * Every available donor who MAY donate to a {@code recipient} — the Appendix A
     * chart applied in its printed direction. This is the ONLY place that fan-out
     * is computed: the urgent-notification hook and the donor dashboard both call
     * it, so the two can never disagree about who is eligible.
     */
    @Transactional(readOnly = true)
    public List<Donor> compatible(BloodGroup recipient) {
        if (recipient == null) {
            return List.of();
        }
        return donors.findByBloodGroupInAndAvailableTrue(recipient.getCompatibleDonorGroups());
    }

    /** The same set as donor cards, for the compatible-donor views. */
    @Transactional(readOnly = true)
    public List<DonorResponse> compatibleCards(BloodGroup recipient) {
        return compatible(recipient).stream().map(d -> DonorResponse.of(d, recipient)).toList();
    }

    /* --------------------------------------------------------- donor dashboard (BR-7) */

    /**
     * "Requests near you": the open requests this donor may actually serve,
     * minus the ones they already declined.
     *
     * <p>Two rules in one list —
     * <ul>
     *   <li>BR-1: {@code request.getBloodGroup().acceptsDonor(donorGroup)} — the
     *       request's group (the recipient) must accept the donor's group. Asking
     *       it the other way would offer a donor a patient they must not serve.</li>
     *   <li>BR-7: a decline hides a request for that donor only, without touching
     *       the request itself.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public List<RequestResponse> requestsNearYou(Long donorId) {
        Donor donor = require(donorId);
        Set<Long> declined = new HashSet<>();
        declines.findByDonor_DonorId(donor.getDonorId())
                .forEach(x -> declined.add(x.getRequest().getRequestId()));

        List<BloodRequest> open = requests.findByStatusInOrderByCreatedAtDesc(
                List.of(RequestStatus.PENDING, RequestStatus.ACCEPTED));

        return open.stream()
                .filter(r -> r.getBloodGroup() != null
                        && donor.getBloodGroup() != null
                        && r.getBloodGroup().acceptsDonor(donor.getBloodGroup()))
                .filter(r -> !declined.contains(r.getRequestId()))
                .map(RequestResponse::of)
                .toList();
    }

    /* ------------------------------------------------------------------ mutations */

    /** The donor profile's availability toggle. */
    @Transactional
    public DonorResponse setAvailability(Long donorId, boolean available) {
        Donor donor = require(donorId);
        donor.setAvailable(available);
        donors.save(donor);
        return DonorResponse.of(donor, null);
    }

    /**
     * §2.5 upload: Base64 data URL → the {@code donors.photo} BLOB. Images are
     * stored IN the database — explicitly not as files on disk or a BLOB path.
     */
    @Transactional
    public void savePhoto(Long donorId, String dataUrl) {
        Donor donor = require(donorId);
        donor.setPhoto(PhotoCodec.decode(dataUrl));
        donors.save(donor);
    }

    /** §2.5 retrieval: the stored bytes re-wrapped as a data URL (null when unset). */
    @Transactional(readOnly = true)
    public String photoDataUrl(Long donorId) {
        return PhotoCodec.encode(require(donorId).getPhoto());
    }

    /** §2.5 retrieval, raw: the BLOB bytes as stored (empty when unset) — the image endpoint. */
    @Transactional(readOnly = true)
    public byte[] photoBytes(Long donorId) {
        byte[] photo = require(donorId).getPhoto();
        return photo == null ? new byte[0] : photo;
    }

    /* -------------------------------------------------------------------- helpers */

    private Donor require(Long donorId) {
        if (donorId == null) {
            throw new BusinessException("No donor profile for this account.");
        }
        return donors.findById(donorId)
                .orElseThrow(() -> new NotFoundException("Unknown donor: " + donorId));
    }

    /** Blank → null (no filter); a bad label is a client error, not a silent empty search. */
    private static BloodGroup parseGroup(String blood) {
        if (blood == null || blood.isBlank()) {
            return null;
        }
        try {
            return BloodGroup.fromLabel(blood);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
