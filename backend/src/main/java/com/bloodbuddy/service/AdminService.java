package com.bloodbuddy.service;

import com.bloodbuddy.dto.UserAdminResponse;
import com.bloodbuddy.model.Donor;
import com.bloodbuddy.model.Role;
import com.bloodbuddy.model.User;
import com.bloodbuddy.model.UserStatus;
import com.bloodbuddy.repository.DonorRepository;
import com.bloodbuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Administrator account management (proposal Appendix B
 * {@code GET /api/admin/users} + {@code PATCH /api/admin/users/{id}}): list,
 * filter, suspend and restore.
 *
 * <p>One rule that is easy to forget and expensive to get wrong: an administrator
 * cannot suspend their own account. Without it the last admin can lock the
 * platform out of its own user management, from the UI, with one click.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository users;
    private final DonorRepository donors;

    /**
     * The user table, filtered the way the dashboard's toolbar does it.
     *
     * @param roleLabel  "Donor" / "Requester" / "Hospital" / "Admin", blank = all
     * @param statusLabel "Active" / "Suspended", blank = all
     * @param query      free text matched against name, email and id
     */
    @Transactional(readOnly = true)
    public List<UserAdminResponse> listUsers(String roleLabel, String statusLabel, String query) {
        Role role = isBlank(roleLabel) ? null : parseRole(roleLabel);
        UserStatus status = isBlank(statusLabel) ? null : parseStatus(statusLabel);

        List<User> rows = role != null && status != null ? users.findByRoleAndStatus(role, status)
                : role != null ? users.findByRole(role)
                : status != null ? users.findByStatus(status)
                : users.findAllByOrderByCreatedAtDesc();

        // The blood group column comes from the donor extension; loaded once and
        // keyed by user id rather than one query per row.
        Map<Long, Donor> donorByUser = new LinkedHashMap<>();
        for (Donor d : donors.findAll()) {
            if (d.getUser() != null) {
                donorByUser.put(d.getUser().getUserId(), d);
            }
        }

        String needle = isBlank(query) ? null : query.trim().toLowerCase();
        return rows.stream()
                .filter(u -> needle == null || matches(u, needle))
                .map(u -> UserAdminResponse.of(u, donorByUser.get(u.getUserId())))
                .toList();
    }

    /** Suspend / restore one account (the dashboard's toggle). */
    @Transactional
    public UserAdminResponse setStatus(String userId, String statusLabel, ActorContext actor) {
        User user = requireUser(userId);
        UserStatus target = parseStatus(statusLabel);
        if (target == UserStatus.SUSPENDED && actor != null && actor.userId() != null
                && actor.userId().equals(user.getUserId())) {
            throw new BusinessException("You cannot suspend your own account.");
        }
        user.setStatus(target);
        users.save(user);
        return UserAdminResponse.of(user, donors.findByUser_UserId(user.getUserId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public UserAdminResponse get(String userId) {
        User user = requireUser(userId);
        return UserAdminResponse.of(user, donors.findByUser_UserId(user.getUserId()).orElse(null));
    }

    private User requireUser(String userId) {
        Long id;
        try {
            id = Long.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new NotFoundException("Unknown account: " + userId);
        }
        return users.findById(id).orElseThrow(() -> new NotFoundException("Unknown account: " + userId));
    }

    private static boolean matches(User u, String needle) {
        return u.getName().toLowerCase().contains(needle)
                || u.getEmail().toLowerCase().contains(needle)
                || String.valueOf(u.getUserId()).contains(needle);
    }

    private static Role parseRole(String label) {
        try {
            return Role.valueOf(label.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unknown role filter: " + label);
        }
    }

    /** Accepts both the dashboard's labels ("Suspended") and the enum names. */
    private static UserStatus parseStatus(String label) {
        try {
            return UserStatus.valueOf(label.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unknown account status: " + label);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
