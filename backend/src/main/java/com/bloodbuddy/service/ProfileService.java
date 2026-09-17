package com.bloodbuddy.service;

import com.bloodbuddy.dto.AccountResponse;
import com.bloodbuddy.dto.ProfileDto;
import com.bloodbuddy.model.BloodGroup;
import com.bloodbuddy.model.Donor;
import com.bloodbuddy.model.Hospital;
import com.bloodbuddy.model.Role;
import com.bloodbuddy.model.User;
import com.bloodbuddy.repository.DonorRepository;
import com.bloodbuddy.repository.HospitalRepository;
import com.bloodbuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The four profile editors ({@code GET}/{@code PUT /api/profile/{role}}), which
 * the demo served from {@code localStorage} keys {@code bb_<role>_profile}.
 *
 * <p>Two mappings are worth knowing because they are where the ERD and the forms
 * disagree (both documented on {@link ProfileDto}):
 * <ul>
 *   <li>one {@code users.name} column vs. a first/last pair in the forms —
 *       joined on save, split on the first space on read;</li>
 *   <li>{@code address} / {@code preferredHospital} / {@code urgentAlerts} are
 *       profile-editor fields the ERD never modelled, persisted on
 *       {@code users} so {@code PUT} does not merely echo them back unsaved.</li>
 * </ul>
 *
 * <p>The §2.5 photo is written to the donor BLOB and read back as a Base64 data
 * URL — the same round trip the pages already render. All four editors show a
 * photo picker (session #10 put the stub on every profile page), but the BLOB
 * column lives on {@code Donor}, so the photo is only PERSISTED for donors; for
 * the other three roles the field is accepted and returned unchanged. Storing
 * theirs needs a column each — noted rather than silently pretended.
 *
 * <p>Scope: a caller may only edit the account they are signed in as; the user id
 * is a parameter the controller takes from the session, never from the body.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository users;
    private final DonorRepository donors;
    private final HospitalRepository hospitals;

    /** Load the signed-in account's editor. */
    @Transactional(readOnly = true)
    public ProfileDto get(String roleLabel, Long userId) {
        User u = require(userId);
        Role role = requireOwnRole(roleLabel, u);
        Donor donor = role == Role.DONOR ? donors.findByUser_UserId(u.getUserId()).orElse(null) : null;
        Hospital h = u.getStaffedHospital();
        String[] name = splitName(u.getName());
        return new ProfileDto(
                name[0], name[1], u.getEmail(), u.getPhone(), u.getDistrict(), u.getAddress(),
                donor == null || donor.getBloodGroup() == null ? null : donor.getBloodGroup().getLabel(),
                donor == null ? null : donor.isAvailable(),
                u.getPreferredHospital(), u.isUrgentAlerts(),
                h == null ? null : h.getName(), h == null ? null : h.getFacilityType(),
                h == null ? null : h.getOpeningHours(), h == null ? null : h.isBloodBankOpen(),
                donor == null ? null : PhotoCodec.encode(donor.getPhoto()));
    }

    /**
     * Persist an editor's form and return what is now STORED (not what was sent)
     * — so the page's save bar cannot end up showing unsaved values as saved.
     */
    @Transactional
    public ProfileDto save(String roleLabel, Long userId, ProfileDto form) {
        User u = require(userId);
        Role role = requireOwnRole(roleLabel, u);

        if (form.fname() != null || form.lname() != null) {
            String joined = joinName(form.fname(), form.lname());
            if (!joined.isBlank()) {
                u.setName(joined);
            }
        }
        if (!isBlank(form.email()) && !form.email().trim().equalsIgnoreCase(u.getEmail())) {
            String email = form.email().trim().toLowerCase();
            if (users.existsByEmailIgnoreCase(email)) {
                throw new BusinessException("Another account already uses that email address.");
            }
            u.setEmail(email);
        }
        u.setPhone(orNull(form.phone()));
        u.setDistrict(orNull(form.district()));
        u.setAddress(orNull(form.address()));

        switch (role) {
            case DONOR -> {
                Donor donor = donors.findByUser_UserId(u.getUserId()).orElseGet(() -> {
                    Donor fresh = new Donor();
                    fresh.setUser(u);
                    return fresh;
                });
                if (!isBlank(form.blood())) {
                    try {
                        donor.setBloodGroup(BloodGroup.fromLabel(form.blood()));
                    } catch (IllegalArgumentException e) {
                        throw new BusinessException(e.getMessage());
                    }
                }
                if (form.address() != null && !form.address().isBlank()) {
                    donor.setArea(form.address().trim());   // donors keep their neighbourhood here
                }
                if (form.available() != null) {
                    donor.setAvailable(form.available());
                }
                if (form.photo() != null) {
                    donor.setPhoto(form.photo().isBlank() ? null : PhotoCodec.decode(form.photo()));
                }
                donors.save(donor);
            }
            case REQUESTER -> {
                if (form.hospital() != null) {
                    u.setPreferredHospital(orNull(form.hospital()));
                }
                if (form.alerts() != null) {
                    u.setUrgentAlerts(form.alerts());
                }
            }
            case HOSPITAL -> {
                Hospital h = u.getStaffedHospital();
                if (h != null) {
                    if (!isBlank(form.hname())) {
                        h.setName(form.hname().trim());
                    }
                    if (form.type() != null) {
                        h.setFacilityType(orNull(form.type()));
                    }
                    if (form.hours() != null) {
                        h.setOpeningHours(orNull(form.hours()));
                    }
                    if (form.open() != null) {
                        h.setBloodBankOpen(form.open());
                    }
                    if (!isBlank(form.address())) {
                        h.setArea(form.address().trim());   // the hospital's own area column
                    }
                    if (!isBlank(form.district())) {
                        h.setDistrict(form.district().trim());
                    }
                    hospitals.save(h);
                }
            }
            case ADMIN -> {
                // own details only — role and member-since stay server-owned
            }
        }

        users.save(u);
        return get(roleLabel, userId);
    }

    private User require(Long userId) {
        if (userId == null) {
            throw new BusinessException("Sign in to view your profile.");
        }
        return users.findById(userId).orElseThrow(() -> new NotFoundException("Unknown account."));
    }

    private static Role parseRole(String label) {
        if (isBlank(label)) {
            throw new BusinessException("No profile type given.");
        }
        try {
            return Role.valueOf(label.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unknown profile type: " + label);
        }
    }

    /**
     * The editor asked for must be the account's OWN role. Without this, a requester
     * could open {@code /api/profile/donor} and be handed a donor-shaped form with
     * empty fields (and a PUT could try to write donor fields onto an account that
     * has no donor row). Wrong role is a 409 with the mismatch named, not a
     * half-populated editor.
     */
    private static Role requireOwnRole(String label, User u) {
        Role role = parseRole(label);
        if (role != u.getRole()) {
            throw new BusinessException("This account is a " + AccountResponse.label(u.getRole())
                    + " account — open the " + AccountResponse.label(u.getRole()) + " profile instead.");
        }
        return role;
    }

    /** "Saksham Maharjan" → {"Saksham", "Maharjan"}; a single word yields an empty last name. */
    private static String[] splitName(String name) {
        if (name == null) {
            return new String[]{"", ""};
        }
        int space = name.indexOf(' ');
        return space < 0
                ? new String[]{name, ""}
                : new String[]{name.substring(0, space), name.substring(space + 1).trim()};
    }

    private static String joinName(String first, String last) {
        String f = first == null ? "" : first.trim();
        String l = last == null ? "" : last.trim();
        return (f + " " + l).trim();
    }

    private static String orNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
