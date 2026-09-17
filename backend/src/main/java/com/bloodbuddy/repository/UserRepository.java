package com.bloodbuddy.repository;

import com.bloodbuddy.model.Role;
import com.bloodbuddy.model.User;
import com.bloodbuddy.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} (§2.3). Derived queries only —
 * Spring generates the SQL from the method names.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Login + registration uniqueness check; also "resolve a requester by email". */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Resolve a requester by DISPLAY NAME. The demo's tracker calls
     * {@code requests.list({ requester: session.name })}, so the real endpoint has
     * to answer that shape until B6/B7 pass the session's user id instead. Kept
     * deliberately narrow — names are not unique in the real world, and the first
     * match is why this is a bridge and not a lookup mechanism.
     */
    Optional<User> findFirstByNameIgnoreCase(String name);

    boolean existsByEmailIgnoreCase(String email);

    /** Admin user-management table (role filter). */
    List<User> findByRole(Role role);

    /** Admin user-management table (status filter / suspend flow). */
    List<User> findByStatus(UserStatus status);

    List<User> findByRoleAndStatus(Role role, UserStatus status);

    List<User> findAllByOrderByCreatedAtDesc();
}
