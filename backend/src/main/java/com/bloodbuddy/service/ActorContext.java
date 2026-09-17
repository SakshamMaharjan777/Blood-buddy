package com.bloodbuddy.service;

import com.bloodbuddy.model.Donor;
import com.bloodbuddy.model.Role;
import com.bloodbuddy.model.User;

/**
 * WHO is asking — the identity a service needs to enforce a role-sensitive rule.
 *
 * <p>B2 has no authentication layer yet (B6 adds Spring Security), so the
 * services take the actor as a parameter instead of reading a
 * {@code SecurityContext} that does not exist. B3 builds one of these from the
 * session and B6 fills it from the authenticated principal — the service
 * signatures do not change, which is the point.
 *
 * <p>Nothing here is trusted from the wire: an actor with {@code donorId == null}
 * cannot accept a request (see {@code RequestService.acceptByDonor}), so a caller
 * cannot hand itself a donor id in a PATCH body and match a request.
 */
public record ActorContext(
        Long userId,
        Role role,
        /** Set only for a DONOR actor. */
        Long donorId,
        /** Set only for a HOSPITAL actor — the hospital their writes are scoped to (BR-5). */
        Long hospitalId
) {

    /** Nobody signed in (guest emergency flow). */
    public static ActorContext anonymous() {
        return new ActorContext(null, null, null, null);
    }

    /** Build from the loaded entities (B3/B6 will call this). */
    public static ActorContext of(User user, Donor donor) {
        if (user == null) {
            return anonymous();
        }
        return new ActorContext(
                user.getUserId(),
                user.getRole(),
                donor == null ? null : donor.getDonorId(),
                user.getStaffedHospital() == null ? null : user.getStaffedHospital().getHospitalId());
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isDonor() {
        return role == Role.DONOR;
    }

    public boolean isHospital() {
        return role == Role.HOSPITAL;
    }
}
