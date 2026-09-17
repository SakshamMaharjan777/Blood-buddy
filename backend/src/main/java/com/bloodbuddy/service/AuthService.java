package com.bloodbuddy.service;

import com.bloodbuddy.dto.AccountResponse;
import com.bloodbuddy.dto.LoginRequest;
import com.bloodbuddy.dto.LoginResponse;
import com.bloodbuddy.dto.RegisterRequest;
import com.bloodbuddy.model.*;
import com.bloodbuddy.repository.DonorRepository;
import com.bloodbuddy.repository.HospitalRepository;
import com.bloodbuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Accounts: register, log in, and the "I forgot my password" acknowledgement.
 *
 * <p>BR-9 — no plaintext credential ever: the submitted password is hashed with
 * BCrypt before the row is written, and the hash is never part of a response
 * ({@link AccountResponse} has no such field).
 *
 * <p>Login failures are deliberately indistinguishable ("Invalid email or
 * password." for a wrong address AND a wrong password) so the endpoint cannot be
 * used to discover which addresses have accounts. A SUSPENDED account is the one
 * case that says more, because the user has a real action to take.
 *
 * <p>Registering is the §2.4 hook point: the mandatory registration-confirmation
 * mail is queued here (delivery itself is B4).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Minimum, checked server-side as well as in the browser (CPJ119 §4). */
    private static final int MIN_PASSWORD = 8;

    private final UserRepository users;
    private final DonorRepository donors;
    private final HospitalResolver hospitalResolver;
    private final BCryptPasswordEncoder encoder;
    private final NotificationService notifications;

    /**
     * Create an account. Donors additionally get their {@link Donor} extension
     * (blood group is required for them — that is the whole point of the role);
     * hospital staff are scoped to the hospital they belong to (BR-5).
     *
     * <p>Administrator self-registration is refused on purpose: an account with
     * platform-wide powers is provisioned, never signed up for.
     */
    @Transactional
    public AccountResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("An account with that email already exists — try logging in.");
        }
        Role role = parseRole(req.role());
        if (req.password() == null || req.password().length() < MIN_PASSWORD) {
            throw new BusinessException("Choose a password of at least " + MIN_PASSWORD + " characters.");
        }

        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setPhone(isBlank(req.phone()) ? null : req.phone().trim());
        u.setDistrict(isBlank(req.district()) ? null : req.district().trim());
        if (role == Role.HOSPITAL) {
            // The staff account's hospital — from the id when the client knows the
            // row, or from the NAME the register form actually collects ("TUTH",
            // "Bir Hospital"). Without this link BR-5 has nothing to scope to, so
            // the account exists but cannot touch any stock; see the warning below
            // for the case where the name matched no partner hospital.
            u.setStaffedHospital(hospitalResolver.resolve(req.hospitalId(), req.hospital()));
        }
        users.save(u);

        Donor donor = null;
        if (role == Role.DONOR) {
            donor = new Donor();
            donor.setUser(u);
            donor.setBloodGroup(parseGroup(req.blood()));
            donor.setArea(isBlank(req.area()) ? u.getDistrict() : req.area().trim());
            donor.setAvailable(true);
            if (!isBlank(req.photo())) {
                donor.setPhoto(PhotoCodec.decode(req.photo()));
            }
            donors.save(donor);
        } else if (role == Role.HOSPITAL && u.getStaffedHospital() == null) {
            // BR-5 needs this link. The name the staff member typed matched no
            // partner hospital, so the account exists but is scoped to no queue
            // until an administrator or the profile editor sets it. Say so rather
            // than pretending the account is usable.
            log.warn("Hospital account {} registered without a hospital link — BR-5 scoping is not active for it.", email);
        }

        notifications.registration(u);   // §2.4 mandatory notification (delivery: B4)
        log.info("Registered {} account {}", role, email);
        return AccountResponse.of(u, donor);
    }

    /** Authenticate and return the account summary the session is built from. */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        User u = users.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new BusinessException("Invalid email or password."));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new BusinessException("Invalid email or password.");
        }
        if (u.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException("This account is suspended — please contact the platform administrator.");
        }
        if (!isBlank(req.role()) && !AccountResponse.label(u.getRole()).equalsIgnoreCase(req.role().trim())) {
            throw new BusinessException("That email is registered as a " + AccountResponse.label(u.getRole())
                    + " account — pick that portal instead.");
        }
        Donor donor = u.getRole() == Role.DONOR ? donorOf(u) : null;
        return new LoginResponse(true, "bb-" + UUID.randomUUID(), AccountResponse.of(u, donor));
    }

    /** GET /api/auth/me — the account behind a session (B3 gets the id from the session). */
    @Transactional(readOnly = true)
    public AccountResponse me(Long userId) {
        if (userId == null) {
            throw new BusinessException("Sign in to see your account.");
        }
        User u = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Unknown account."));
        return AccountResponse.of(u, u.getRole() == Role.DONOR ? donorOf(u) : null);
    }

    /**
     * Password-reset acknowledgement. Always succeeds from the caller's point of
     * view (no account enumeration) and only queues a mail when the address is
     * real. The reset link itself belongs to B6 — a link that works before
     * Spring Security exists would be a security hole, not a feature.
     */
    @Transactional
    public void forgotPassword(String email) {
        if (isBlank(email)) {
            return;
        }
        users.findByEmailIgnoreCase(email.trim())
                .ifPresent(u -> notifications.record(NotificationType.PASSWORD_RESET, u, null,
                        "Reset your BloodBuddy password"));
    }

    private Donor donorOf(User u) {
        return donors.findByUser_UserId(u.getUserId()).orElse(null);
    }

    /** The register form's lowercase role string → the enum. Admin is refused. */
    private static Role parseRole(String raw) {
        if (isBlank(raw)) {
            throw new BusinessException("Choose an account type.");
        }
        return switch (raw.trim().toLowerCase()) {
            case "donor" -> Role.DONOR;
            case "requester", "recipient" -> Role.REQUESTER;
            case "hospital", "hospital staff", "staff" -> Role.HOSPITAL;
            case "admin", "administrator" ->
                    throw new BusinessException("Administrator accounts are provisioned, not self-registered.");
            default -> throw new BusinessException("Unknown account type: " + raw);
        };
    }

    private static BloodGroup parseGroup(String blood) {
        if (isBlank(blood) || "-".equals(blood.trim()) || "—".equals(blood.trim())) {
            throw new BusinessException("Donors must choose a blood group.");
        }
        try {
            return BloodGroup.fromLabel(blood);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
