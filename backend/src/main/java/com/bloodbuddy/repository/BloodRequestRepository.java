package com.bloodbuddy.repository;

import com.bloodbuddy.model.BloodGroup;
import com.bloodbuddy.model.BloodRequest;
import com.bloodbuddy.model.RequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link BloodRequest} (§2.3). Covers the four
 * demo readers: requester tracker (own requests), hospital queue (scoped,
 * BR-5), admin moderation queue, and the donor dashboard's open-requests list
 * (BR-1 direction + BR-7 decline filtering happen in the service on top of
 * these).
 */
public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    Optional<BloodRequest> findByPublicCode(String publicCode);

    /** Requester's tracker (demo: requests.list({requester})). */
    List<BloodRequest> findByRequester_UserIdOrderByCreatedAtDesc(Long requesterUserId);

    /** Hospital queue (demo: requests.list({hospital})) — scoped, never global. */
    List<BloodRequest> findByHospital_HospitalIdOrderByCreatedAtDesc(Long hospitalId);

    List<BloodRequest> findByHospital_HospitalIdAndStatusOrderByCreatedAtDesc(
            Long hospitalId, RequestStatus status);

    /** Admin moderation queue. */
    List<BloodRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    /** All open requests (PENDING or ACCEPTED) — donor dashboard's base set. */
    List<BloodRequest> findByStatusInOrderByCreatedAtDesc(List<RequestStatus> statuses);

    /** Open requests for one blood group (the service filters compatibility on top). */
    List<BloodRequest> findByStatusInAndBloodGroupOrderByCreatedAtDesc(
            List<RequestStatus> statuses, BloodGroup bloodGroup);

    /** Paged admin/requester listings (proposal scope: pagination). */
    Page<BloodRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<BloodRequest> findByRequester_UserIdOrderByCreatedAtDesc(Long requesterUserId, Pageable pageable);

    Page<BloodRequest> findByHospital_HospitalIdOrderByCreatedAtDesc(Long hospitalId, Pageable pageable);

    Page<BloodRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status, Pageable pageable);

    /**
     * BR-3 race guard: the accept path re-checks inside the transaction that no
     * donor is attached, so two donors tapping Accept at once cannot both match.
     */
    boolean existsByRequestIdAndDonorIsNotNull(Long requestId);

    /**
     * BR-3, done properly: load the request under a row lock so the "is a donor
     * already attached?" check and the match write cannot interleave with a
     * competing accept. {@code existsBy…} alone is a read-then-write window;
     * this makes the guard atomic in the database rather than in the service's
     * intentions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from BloodRequest r where r.requestId = :id")
    Optional<BloodRequest> findWithLockByRequestId(@Param("id") Long id);

    /** Guest emergency submissions (BR-8 — the only rows a public lookup may see). */
    List<BloodRequest> findByGuestTrueOrderByCreatedAtDesc();

    /** Admin moderation: requests no hospital has been assigned yet. */
    List<BloodRequest> findByHospitalIsNullOrderByCreatedAtDesc();

    /** One requester's tracker filtered by status. */
    List<BloodRequest> findByRequester_UserIdAndStatusOrderByCreatedAtDesc(
            Long requesterUserId, RequestStatus status);
}
