package com.bloodbuddy.config;

import com.bloodbuddy.model.*;
import com.bloodbuddy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seeds the demo dataset so a fresh database shows the same scenario the
 * frontend demo ships (bb-store.js SEED_* constants). Runs once per boot;
 * every write is "exists?"-guarded so re-running never duplicates.
 *
 * Disable with {@code bloodbuddy.seed=false} (application.properties).
 *
 * Two deliberate corrections against the demo seeds — both originals are
 * impossible under Appendix A with the registered donors:
 *   BB-8F3K2M  recipient O+ → B+   (donor Arun Shrestha is B+; O+ accepts
 *                                   only O+/O-, but B+ accepts B+ exact)
 *   BB-7HD4LN  recipient AB- → AB+ (donor Priya Karki is B+; AB- rejects Rh+,
 *                                   AB+ is the universal recipient)
 * The backend matching service (B2) would reject the original pairs, so the
 * demo data must not ship them.
 *
 * All seeded accounts share the demo password {@code BloodBuddy#2026} (BCrypt
 * — BR-9: no plaintext anywhere). B6 replaces this with per-user registration.
 *
 * The demo's {@code compat} flag and "4 months ago" last-donation strings are
 * the fields that DON'T survive the move: compatibility is recomputed from the
 * chart at request time (never stored as data), and last-donation becomes a
 * real date.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DemoDataSeeder {

    private final UserRepository users;
    private final DonorRepository donors;
    private final HospitalRepository hospitals;
    private final BloodInventoryRepository inventory;
    private final BloodRequestRepository requests;
    private final RequestDeclineRepository declines;
    private final EmailNotificationRepository notifications;

    private final BCryptPasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();
    private final TransactionTemplate tx;
    private String demoPasswordHash;

    /** application.properties: bloodbuddy.seed=true (default) — set false for a clean database. */
    @Value("${bloodbuddy.seed:true}")
    private boolean seedEnabled;

    /**
     * Ordered before every other runner: the B2 smoke run ({@code @Order(10)})
     * asserts against the seeded hospitals/donors, and it must see the fully
     * seeded database — an un-ordered ApplicationRunner is treated as
     * LOWEST_PRECEDENCE, which put seeding AFTER the smoke run and made the
     * hospital-label resolution look broken when it was only late.
     */
    @Bean
    @Order(0)
    public ApplicationRunner seedDemoData() {
        // TransactionTemplate (not @Transactional): an anonymous ApplicationRunner
        // is not reliably proxied, so the transaction is explicit.
        return (ApplicationArguments args) -> {
            if (!seedEnabled) {
                log.info("Demo data seeding disabled (bloodbuddy.seed=false)");
                return;
            }
            tx.executeWithoutResult(status -> seed());
        };
    }

    private void seed() {
        demoPasswordHash = encoder.encode("BloodBuddy#2026");

        List<Hospital> hs = seedHospitals();
        List<User> us = seedUsers(hs);
        seedDonors(us);
        seedAffiliations(hs);
        seedInventory(hs);
        seedRequests(us, hs);
        seedNotifications(us);

        log.info("Demo data ready: {} users, {} donors, {} hospitals, {} inventory rows, {} requests, {} notifications",
                users.count(), donors.count(), hospitals.count(), inventory.count(),
                requests.count(), notifications.count());
    }

    /* ----------------------------- users -----------------------------
       Index map (used below): 0 Arun, 1 Sunita, 2 Bikash, 3 Sita, 4 Ramesh,
       5 TUTH, 6 Bir, 7 Patan, 8 Anita, 9 Admin, 10 Kiran, 11 Priya. */

    private List<User> seedUsers(List<Hospital> hs) {
        User arun = user("Arun Shrestha", "arun.s@example.com", Role.DONOR, "Kathmandu");
        User sunita = user("Sunita Maharjan", "sunita.m@example.com", Role.DONOR, "Lalitpur");
        User bikash = user("Bikash Tamang", "bikash.t@example.com", Role.DONOR, "Kathmandu");
        bikash.setStatus(UserStatus.SUSPENDED);   // demo: admin suspend flow
        User sita = user("Sita Gurung", "sita.g@example.com", Role.REQUESTER, "Kathmandu");
        User ramesh = user("Ramesh Shrestha", "ramesh.s@example.com", Role.REQUESTER, "Bhaktapur");
        User tuth = user("TUTH Blood Bank", "bloodbank@tuth.edu.np", Role.HOSPITAL, "Kathmandu");
        User bir = user("Bir Hospital", "admin@birhospital.org.np", Role.HOSPITAL, "Kathmandu");
        User patan = user("Patan Hospital", "bb@patanhospital.org.np", Role.HOSPITAL, "Lalitpur");
        tuth.setStaffedHospital(hs.get(0));       // BR-5: staff session ↔ one hospital
        bir.setStaffedHospital(hs.get(1));
        patan.setStaffedHospital(hs.get(2));
        User anita = user("Anita Rai", "anita.r@example.com", Role.DONOR, "Pokhara");
        User admin = user("Admin · Saksham", "saksham@bloodbuddy.np", Role.ADMIN, "Lalitpur");
        User kiran = user("Kiran Adhikari", "kiran.a@example.com", Role.REQUESTER, "Kathmandu");
        User priya = user("Priya Karki", "priya.k@example.com", Role.DONOR, "Kathmandu");

        return users.saveAll(List.of(arun, sunita, bikash, sita, ramesh, tuth, bir, patan,
                                     anita, admin, kiran, priya));
    }

    private User user(String name, String email, Role role, String district) {
        // Idempotency guard: a re-boot must never duplicate an account.
        User existing = users.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            return existing;
        }
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(demoPasswordHash);
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setDistrict(district);
        return u;
    }

    /* ----------------------------- donors -----------------------------
       The 14-donor registry from bb-store.js SEED_DONORS. "area" strings
       carry the neighbourhood the search page shows. */

    private void seedDonors(List<User> us) {
        //        user        bloodGroup        area                       available  months-since-donation
        donor(us.get(0),  BloodGroup.B_PLUS,  "Baneshwor, Kathmandu",    true,  4);
        donor(us.get(1),  BloodGroup.B_PLUS,  "Thamel, Kathmandu",       true,  6);
        donor(us.get(2),  BloodGroup.B_MINUS, "Koteshwor, Kathmandu",    false, 2);
        donor(us.get(11), BloodGroup.B_PLUS,  "Baluwatar, Kathmandu",    true,  5);
        donor(fresh("Rajan Thapa", "rajan.t@example.com", "Kathmandu"),
                          BloodGroup.B_PLUS,  "Chabahil, Kathmandu",     true,  8);
        donor(fresh("Deepa Rana", "deepa.r@example.com", "Kathmandu"),
                          BloodGroup.B_PLUS,  "Lazimpat, Kathmandu",     true,  12);
        donor(fresh("Kiran Gurung", "kiran.g@example.com", "Kathmandu"),
                          BloodGroup.B_MINUS, "Gongabu, Kathmandu",      false, 3);
        donor(fresh("Mina Shrestha", "mina.s@example.com", "Kathmandu"),
                          BloodGroup.B_PLUS,  "Maharajgunj, Kathmandu",  true,  7);
        donor(fresh("Dipesh Limbu", "dipesh.l@example.com", "Kathmandu"),
                          BloodGroup.A_PLUS,  "Sorakhutte, Kathmandu",   true,  1);
        donor(fresh("Anjali Karki", "anjali.k@example.com", "Kathmandu"),
                          BloodGroup.AB_PLUS, "Swayambhu, Kathmandu",    false, 7);
        donor(fresh("Kabita Gurung", "kabita.g@example.com", "Lalitpur"),
                          BloodGroup.B_MINUS, "Kupondole, Lalitpur",     true,  3);
        donor(fresh("Nabin Chaudhary", "nabin.c@example.com", "Lalitpur"),
                          BloodGroup.O_PLUS,  "Pulchowk, Lalitpur",      true,  2);
        donor(us.get(8),  BloodGroup.O_MINUS, "Lakeside, Pokhara",       true,  5);
        donor(fresh("Roshan Bhattarai", "roshan.b@example.com", "Chitwan"),
                          BloodGroup.O_PLUS,  "Zero KM, Chitwan",        true,  4);
    }

    /**
     * A registry account that is NOT part of the users list above, so
     * {@code seedUsers()}'s saveAll never persists it — this method must. The
     * first four donors reuse users from that list, so their FK is fine; these
     * ten would otherwise be transient, and {@code Donor.user} is a non-nullable
     * FK: Hibernate throws TransientPropertyValueException at insert time and
     * the ApplicationRunner failure takes the whole boot down.
     */
    private User fresh(String name, String email, String district) {
        return users.save(user(name, email, Role.DONOR, district));
    }

    private void donor(User u, BloodGroup group, String area, boolean available, int monthsSinceDonation) {
        if (donors.findByUser_UserId(u.getUserId()).isPresent()) {
            return;
        }
        Donor d = new Donor();
        d.setUser(u);
        d.setBloodGroup(group);
        d.setArea(area);
        d.setAvailable(available);
        d.setLastDonationDate(LocalDate.now().minusMonths(monthsSinceDonation));
        donors.save(d);
    }

    /* ---------------------------- hospitals ---------------------------- */

    /* -------------------- donor ↔ hospital affiliation (M:N) --------------------
       CPJ119 §2.3's many-to-many, and the one the proposal's §3.5 promised. The
       rule that fills it is derived from data already in the demo rather than
       invented: a donor is registered with the partner hospitals in their OWN
       district — "the blood banks near you". A donor whose district has no partner
       hospital (Pokhara, Chitwan) simply has none, which is the honest outcome of
       the rule AND the reason the relationship cannot be 1:N.

       Idempotent: only donors with no affiliation yet are filled, so re-boots do
       not duplicate join rows. Nothing in the request lifecycle reads this — BR-1
       is blood group and BR-5 is the staff account's hospital — so the M:N cannot
       move a rule by being populated differently. */

    private void seedAffiliations(List<Hospital> hs) {
        int linked = 0;
        for (Donor d : donors.findAll()) {
            if (!d.getAffiliatedHospitals().isEmpty()) {
                continue;
            }
            String district = d.getUser() == null ? null : d.getUser().getDistrict();
            if (district == null || district.isBlank()) {
                continue;
            }
            Set<Hospital> near = hs.stream()
                    .filter(h -> district.equalsIgnoreCase(h.getDistrict()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (near.isEmpty()) {
                continue;
            }
            d.setAffiliatedHospitals(near);
            donors.save(d);
            linked++;
        }
        if (linked > 0) {
            log.info("Seeded {} donor↔hospital affiliation(s) (M:N — same-district rule)", linked);
        }
    }

    private List<Hospital> seedHospitals() {
        Hospital tuth  = hospital("Tribhuvan University Teaching Hospital", "TUTH", "Maharajgunj", "Kathmandu", "Teaching hospital");
        Hospital bir   = hospital("Bir Hospital", "Bir Hospital", "Kathmandu", "Kathmandu", "Government hospital");
        Hospital patan = hospital("Patan Hospital", "Patan Hospital", "Lalitpur", "Lalitpur", "Teaching hospital");
        Hospital manmo = hospital("Manmohan Memorial Community Hospital", "Manmohan Memorial", "Kathmandu", "Kathmandu", "Community hospital");
        Hospital nmc   = hospital("Nepal Medical College Teaching Hospital", "NMC", "Kathmandu", "Kathmandu", "Teaching hospital");
        return hospitals.saveAll(List.of(tuth, bir, patan, manmo, nmc));
    }

    private Hospital hospital(String name, String shortName, String area, String district, String type) {
        // Idempotency guard: a re-boot must never duplicate a hospital.
        Hospital existing = hospitals.findByNameIgnoreCase(name).orElse(null);
        if (existing != null) {
            // Backfill the short name for rows seeded before B2 added the column
            // (the seeder is exists-guarded, so an existing row is not re-written).
            if (existing.getShortName() == null) {
                existing.setShortName(shortName);
                hospitals.save(existing);
            }
            return existing;
        }
        Hospital h = new Hospital();
        h.setName(name);
        h.setShortName(shortName);
        h.setArea(area);
        h.setDistrict(district);
        h.setFacilityType(type);
        h.setOpeningHours("24/7");
        h.setBloodBankOpen(true);
        return h;
    }

    /* ---------------------------- inventory ----------------------------
       Bir carries the demo's SEED_INVENTORY numbers exactly; the other four
       hospitals get plausible variants (Manmohan/NMC sparse, with zero-stock
       rows so the "out of stock" state is demoable everywhere). */

    private void seedInventory(List<Hospital> hs) {
        stock(hs.get(0), 24, 3, 18, 6, 12, 2, 8, 5);   // TUTH
        stock(hs.get(1), 24, 3, 18, 6, 12, 2, 8, 5);   // Bir — demo numbers
        stock(hs.get(2), 30, 4, 20, 5, 15, 3, 10, 4);  // Patan
        stock(hs.get(3), 6, 0, 4, 0, 3, 0, 1, 0);      // Manmohan (sparse)
        stock(hs.get(4), 9, 1, 7, 1, 5, 0, 3, 1);      // NMC
    }

    private void stock(Hospital h, int oP, int oM, int aP, int aM, int bP, int bM, int abP, int abM) {
        Long id = h.getHospitalId();
        units(h, id, BloodGroup.O_PLUS, oP);
        units(h, id, BloodGroup.O_MINUS, oM);
        units(h, id, BloodGroup.A_PLUS, aP);
        units(h, id, BloodGroup.A_MINUS, aM);
        units(h, id, BloodGroup.B_PLUS, bP);
        units(h, id, BloodGroup.B_MINUS, bM);
        units(h, id, BloodGroup.AB_PLUS, abP);
        units(h, id, BloodGroup.AB_MINUS, abM);
    }

    private void units(Hospital h, Long hospitalId, BloodGroup g, int u) {
        if (inventory.findByHospital_HospitalIdAndBloodGroup(hospitalId, g).isEmpty()) {
            BloodInventory row = new BloodInventory();
            row.setHospital(h);
            row.setBloodGroup(g);
            row.setUnits(u);
            inventory.save(row);
        }
    }

    /* ---------------------------- requests ----------------------------
       The four demo scenarios, with public codes pinned so the harness /
       Postman collection can target them. */

    private void seedRequests(List<User> us, List<Hospital> hs) {
        User arun = us.get(0), sita = us.get(3), ramesh = us.get(4), anita = us.get(8), kiran = us.get(10), priya = us.get(11);

        request("BB-8F3K2M", false, sita, hs.get(1), donorOf(arun),
                BloodGroup.B_PLUS, 2, Urgency.URGENT, RequestStatus.ACCEPTED, true,
                date("2026-09-15"), "Kathmandu",
                at("2026-09-12T09:24:00"), at("2026-09-12T11:02:00"), null,
                List.of(entry("2026-09-12 09:24", "Request submitted — 14 compatible donors notified"),
                        entry("2026-09-12 11:02", "Arun Shrestha accepted the request")));

        request("BB-2QX9P1", false, ramesh, hs.get(2), null,
                BloodGroup.B_PLUS, 1, Urgency.NORMAL, RequestStatus.PENDING, false,
                date("2026-09-20"), "Lalitpur",
                at("2026-09-13T08:10:00"), null, null,
                List.of(entry("2026-09-13 08:10", "Request submitted — 22 compatible donors notified")));

        // A donor requesting for a relative (the demo also lists Anita, a donor, as this requester).
        request("BB-7HD4LN", false, anita, hs.get(0), donorOf(priya),
                BloodGroup.AB_PLUS, 1, Urgency.NORMAL, RequestStatus.FULFILLED, true,
                date("2026-08-30"), "Kathmandu",
                at("2026-08-28T14:45:00"), at("2026-08-28T15:30:00"), at("2026-08-30T10:15:00"),
                List.of(entry("2026-08-28 14:45", "Request submitted — 6 compatible donors notified"),
                        entry("2026-08-28 15:30", "Priya Karki accepted the request"),
                        entry("2026-08-30 10:15", "Donation completed & verified")));

        // Unrouted: the admin forward flow needs at least one PENDING request with no hospital.
        request("BB-5MN8VX", false, kiran, null, null,
                BloodGroup.O_MINUS, 2, Urgency.URGENT, RequestStatus.PENDING, false,
                date("2026-09-16"), "Kathmandu",
                at("2026-09-13T10:05:00"), null, null,
                List.of(entry("2026-09-13 10:05", "Request submitted — 9 compatible donors notified")));
    }

    private Donor donorOf(User u) {
        return donors.findByUser_UserId(u.getUserId()).orElse(null);
    }

    private BloodRequest.RequestTimelineEntry entry(String when, String what) {
        return new BloodRequest.RequestTimelineEntry(
                LocalDateTime.parse(when, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).toInstant(ZoneOffset.UTC),
                what);
    }

    private Instant at(String iso) {
        return LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC);
    }

    private LocalDate date(String iso) {
        return LocalDate.parse(iso);
    }

    private void request(String publicCode, boolean guest, User requester, Hospital hospital, Donor donor,
                         BloodGroup group, int unitsNeeded, Urgency urgency, RequestStatus status, boolean compatOk,
                         LocalDate neededBy, String district, Instant created, Instant matchedAt, Instant fulfilledAt,
                         List<BloodRequest.RequestTimelineEntry> timeline) {
        if (requests.findByPublicCode(publicCode).isPresent()) {
            return;
        }
        BloodRequest r = new BloodRequest();
        r.setPublicCode(publicCode);
        r.setGuest(guest);
        r.setRequester(requester);
        r.setHospital(hospital);
        r.setDonor(donor);
        r.setBloodGroup(group);
        r.setUnits(unitsNeeded);
        r.setUrgency(urgency);
        r.setStatus(status);
        r.setCompatOk(compatOk);
        r.setNeededBy(neededBy);
        r.setDistrict(district);
        r.setHospitalLabel(hospital == null ? "—" : hospital.getLabel());
        r.setContactPhone("98" + String.format("%08d", 10000000 + random.nextInt(89999999)));
        r.setCreatedAt(created);
        r.setMatchedAt(matchedAt);
        r.setFulfilledAt(fulfilledAt);
        r.setTimeline(new ArrayList<>(timeline));
        requests.save(r);
    }

    /* --------------------------- notifications ---------------------------
       A few audit rows so §2.4's trail is visible on first boot (B4 sends the
       real mail and flips QUEUED → SENT/FAILED). */

    private void seedNotifications(List<User> us) {
        User arun = us.get(0);
        notify(arun, NotificationType.REGISTRATION, null,
                "Welcome to BloodBuddy — confirm your registration", DeliveryStatus.SENT, at("2026-09-01T10:00:00"));
        notify(arun, NotificationType.URGENT_REQUEST, requests.findByPublicCode("BB-8F3K2M").orElse(null),
                "Urgent B+ request near you (Kathmandu)", DeliveryStatus.SENT, at("2026-09-12T09:24:00"));
        notify(us.get(2), NotificationType.URGENT_REQUEST, requests.findByPublicCode("BB-2QX9P1").orElse(null),
                "New B+ request in Lalitpur", DeliveryStatus.FAILED, at("2026-09-13T08:10:00"),
                "Recipient account suspended — notification withheld");
    }

    private void notify(User recipient, NotificationType type, BloodRequest request,
                        String subject, DeliveryStatus status, Instant createdAt) {
        notify(recipient, type, request, subject, status, createdAt, null);
    }

    private void notify(User recipient, NotificationType type, BloodRequest request,
                        String subject, DeliveryStatus status, Instant createdAt, String errorText) {
        // Idempotency guard: skip if this recipient already has this notification
        // (same type + same triggering request).
        if (request != null) {
            boolean already = !notifications
                    .findByRequest_RequestIdOrderByCreatedAtDesc(request.getRequestId()).stream()
                    .filter(n -> n.getRecipient() != null && n.getRecipient().equals(recipient))
                    .filter(n -> n.getType() == type)
                    .findFirst().isEmpty();
            if (already) {
                return;
            }
        } else if (notifications.existsByRecipient_UserIdAndTypeAndRequestIsNull(recipient.getUserId(), type)) {
            // Request-less mail (the registration confirmation) used to fall
            // through this guard entirely, so every boot added another welcome
            // row for the same account. Found by the B2 smoke run.
            return;
        }
        EmailNotification n = new EmailNotification();
        n.setRecipient(recipient);
        // Denormalized address, exactly as a live notification stores it, so the
        // audit table reads the same whether a row came from the seeder or from
        // NotificationService. Note these rows carry no `body`: they are demo
        // HISTORY (written directly, never sent), and inventing the words a past
        // mail "said" would fabricate evidence rather than record it.
        n.setRecipientEmail(recipient == null ? null : recipient.getEmail());
        n.setType(type);
        n.setRequest(request);
        n.setSubject(subject);
        n.setDeliveryStatus(status);
        n.setCreatedAt(createdAt);
        if (status == DeliveryStatus.SENT) {
            n.setSentAt(createdAt);
        }
        n.setErrorText(errorText);
        notifications.save(n);
    }
}
