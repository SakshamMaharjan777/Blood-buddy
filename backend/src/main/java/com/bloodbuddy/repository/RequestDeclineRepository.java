package com.bloodbuddy.repository;

import com.bloodbuddy.model.RequestDecline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link RequestDecline} (BR-7 persistence).
 */
public interface RequestDeclineRepository extends JpaRepository<RequestDecline, Long> {

    /** The donor's hidden-request list, for filtering the "requests near you" board. */
    List<RequestDecline> findByDonor_DonorId(Long donorId);

    boolean existsByDonor_DonorIdAndRequest_RequestId(Long donorId, Long requestId);
}
