package com.bloodbuddy.service;

import com.bloodbuddy.dto.RequestCreateRequest;
import com.bloodbuddy.dto.RequestResponse;
import com.bloodbuddy.dto.RequestUpdateRequest;
import com.bloodbuddy.model.*;
import com.bloodbuddy.repository.BloodRequestRepository;
import com.bloodbuddy.repository.DonorRepository;
import com.bloodbuddy.repository.HospitalRepository;
import com.bloodbuddy.repository.RequestDeclineRepository;
import com.bloodbuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The request lifecycle — the class CPJ119 §2.2 is really about ("all critical
 * application logic must reside in the backend"). Everything the demo used to do
 * in the browser lives here now, once, with the rule attached to it:
 *
 * <table>
 *   <tr><th>Rule</th><th>Method</th></tr>
 *   <tr><td>BR-1 — Appendix A, recipient → donor direction</td><td>{@link #acceptByDonor}, {@code DonorService.compatible}</td></tr>
 *   <tr><td>BR-2 — legal status transitions, terminal states</td><td>{@link #requireTransition}</td></tr>
 *   <tr><td>BR-3 — at most one donor per request, race-safe</td><td>{@link #acceptByDonor}</td></tr>
 *   <tr><td>BR-4 — fulfilment deducts stock, never below zero</td><td>{@link #fulfil}</td></tr>
 *   <tr><td>BR-5 — a hospital touches only its own queue</td><td>{@link #acceptByHospital}, {@link #fulfil}, {@link #byHospital}</td></tr>
 *   <tr><td>BR-7 — a decline hides a request for one donor only</td><td>{@link #declineByDonor}</td></tr>
 *   <tr><td>BR-8 — the public lookup sees EM- guests only</td><td>{@link #guestLookup}</td></tr>
 * </table>
 *
 * <p>Two invariants worth stating because breaking them is easy:
 * <ol>
 *   <li><b>The server owns the timeline.</b> Every transition appends its own
 *       entry; a client-supplied {@code timeline} is ignored (see
 *       {@link RequestUpdateRequest}), so history cannot be rewritten by a PATCH.</li>
 *   <li><b>Identity never comes from the body.</b> The donor who accepts is the
 *       signed-in donor ({@code ActorContext.donorId}) and the hospital that
 *       writes is the staff account's hospital — never a {@code donorId}/
 *       {@code hospitalId} the caller put in the JSON.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    /** Code alphabet without I/O/0/1 — these ids get read aloud and typed by hand. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Pattern PINNED_CODE = Pattern.compile("^(BB|EM)-[A-Z0-9]{3,9}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BloodRequestRepository requests;
    private final DonorRepository donors;
    private final HospitalRepository hospitals;
    private final RequestDeclineRepository declines;
    private final UserRepository users;
    private final DonorService donorService;
    private final InventoryService inventory;
    private final NotificationService notifications;
    private final HospitalResolver hospitalResolver;

    /* ------------------------------------------------------------------- create */

    /**
     * Submit a request — the member form or the public emergency form
     * ({@code emergency = true}, guest, BR-8). Computes compatibility, allocates
     * the public code, queues the donor alerts and opens the timeline.
     */
    @Transactional
    public RequestResponse create(RequestCreateRequest req, ActorContext actor) {
        BloodGroup group = parseGroup(req.blood());
        Urgency urgency = Urgency.fromLabel(req.urgency());
        int units = Math.min(4, Math.max(1, req.units()));

        // Server-side validation (CPJ119 §4 — the browser's check is a courtesy,
        // not the gate): a needed-by date in the past is never acceptable.
        LocalDate neededBy = parseDate(req.neededBy());
        if (neededBy.isBefore(LocalDate.now())) {
            throw new BusinessException("The needed-by date must be today or later.");
        }

        boolean guest = req.emergency();
        User requester = null;
        if (guest) {
            if (isBlank(req.contactPhone())) {
                throw new BusinessException("A contact phone number is required for emergency requests.");
            }
        } else {
            requester = signedIn(actor);
        }

        Hospital hospital = hospitalResolver.resolve(req.hospitalId(), req.hospital());

        BloodRequest r = new BloodRequest();
        r.setPublicCode(resolveCode(req.id(), guest));
        r.setGuest(guest);
        r.setRequester(requester);
        r.setHospital(hospital);
        r.setHospitalLabel(hospitalLabelFor(hospital, req.hospital()));
        r.setBloodGroup(group);
        r.setUnits(units);
        r.setUrgency(urgency);
        r.setStatus(RequestStatus.PENDING);
        r.setNeededBy(neededBy);
        r.setDistrict(req.district().trim());
        r.setContactPhone(!isBlank(req.contactPhone()) ? req.contactPhone().trim()
                : requester == null ? null : requester.getPhone());
        r.setNotes(isBlank(req.notes()) ? null : req.notes().trim());

        // BR-1: who may serve this recipient group — one source of truth.
        List<Donor> compatible = donorService.compatible(group);
        r.setCompatOk(!compatible.isEmpty());
        requests.save(r);   // id assigned before anything references the row

        int queued = notifications.notifyCompatibleDonors(r, compatible);
        stamp(r, "Request submitted — " + queued + " compatible donor" + (queued == 1 ? "" : "s") + " notified");
        requests.save(r);
        return RequestResponse.of(r);
    }

    /* --------------------------------------------------------------------- reads */

    @Transactional(readOnly = true)
    public RequestResponse get(String code) {
        return RequestResponse.of(require(code));
    }

    /** The requester's own tracker (BR-9-adjacent: a member reads only their own rows). */
    @Transactional(readOnly = true)
    public List<RequestResponse> byRequester(Long requesterUserId) {
        if (requesterUserId == null) {
            throw new BusinessException("Sign in to see your requests.");
        }
        return requests.findByRequester_UserIdOrderByCreatedAtDesc(requesterUserId)
                .stream().map(RequestResponse::of).toList();
    }

    /**
     * One hospital's queue (BR-5). The hospital id is required: a "queue" without
     * one would be every request on the platform.
     */
    @Transactional(readOnly = true)
    public List<RequestResponse> byHospital(Long hospitalId, String statusLabel) {
        if (hospitalId == null) {
            throw new BusinessException("No hospital selected.");
        }
        List<BloodRequest> rows = isBlank(statusLabel)
                ? requests.findByHospital_HospitalIdOrderByCreatedAtDesc(hospitalId)
                : requests.findByHospital_HospitalIdAndStatusOrderByCreatedAtDesc(hospitalId, parseStatus(statusLabel));
        return rows.stream().map(RequestResponse::of).toList();
    }

    /**
     * The same queue addressed by the LABEL the pages submit ("TUTH, Maharajgunj")
     * — resolution belongs here, not in the controller, so every caller gets the
     * same short-name/alias handling as request submission.
     */
    @Transactional(readOnly = true)
    public List<RequestResponse> byHospitalLabel(String hospitalLabel, String statusLabel) {
        Hospital h = hospitalResolver.resolve(null, hospitalLabel);
        if (h == null) {
            throw new NotFoundException("Unknown hospital: " + hospitalLabel);
        }
        return byHospital(h.getHospitalId(), statusLabel);
    }

    /**
     * The tracker as the demo's own contract asks for it: a requester NAME or
     * email ({@code requests.list({ requester: session.name })}). Once B6/B7 pass
     * the session's user id this bridge retires — names are not unique, so the
     * first match is a stand-in, never the long-term lookup.
     */
    @Transactional(readOnly = true)
    public List<RequestResponse> byRequesterRef(String nameOrEmail) {
        if (isBlank(nameOrEmail)) {
            throw new BusinessException("Sign in to see your requests.");
        }
        String ref = nameOrEmail.trim();
        User someone = users.findByEmailIgnoreCase(ref)
                .or(() -> users.findFirstByNameIgnoreCase(ref))
                .orElseThrow(() -> new NotFoundException("No account matches " + ref));
        return byRequester(someone.getUserId());
    }

    /** Admin moderation queue, with the demo's two narrowings (guest-only, unrouted-only). */
    @Transactional(readOnly = true)
    public List<RequestResponse> forAdmin(String statusLabel, boolean guestOnly, boolean unroutedOnly) {
        List<BloodRequest> rows;
        if (unroutedOnly) {
            rows = requests.findByHospitalIsNullOrderByCreatedAtDesc();
        } else if (guestOnly) {
            rows = requests.findByGuestTrueOrderByCreatedAtDesc();
        } else if (!isBlank(statusLabel)) {
            rows = requests.findByStatusOrderByCreatedAtDesc(parseStatus(statusLabel));
        } else {
            rows = requests.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return rows.stream().map(RequestResponse::of).toList();
    }

    /** Every request (admin dashboards / the Postman collection's smoke calls). */
    @Transactional(readOnly = true)
    public List<RequestResponse> all() {
        return requests.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(RequestResponse::of).toList();
    }

    /**
     * BR-8 — the public status lookup on the emergency page. Guest requests only,
     * and an unknown-or-member code answers the SAME "not found", so the endpoint
     * cannot be used to confirm that a member's request exists.
     */
    @Transactional(readOnly = true)
    public RequestResponse guestLookup(String code) {
        BloodRequest r = require(code);
        if (!r.isGuest()) {
            throw new NotFoundException("No emergency request with that id.");
        }
        return RequestResponse.of(r);
    }

    /* ---------------------------------------------------------------- transitions */

    /**
     * A donor commits to a request — BR-1 (may this donor serve this recipient?)
     * plus BR-3 (exactly one donor per request).
     *
     * <p>BR-3 is enforced with a pessimistic row lock
     * ({@code findWithLockByRequestId}) rather than a plain read-then-write: two
     * donors tapping Accept at the same instant would both pass an ordinary
     * "is a donor attached?" check and both be written as the match. The lock
     * serialises them, so the second one sees the first and is refused.
     */
    @Transactional
    public RequestResponse acceptByDonor(String code, Long donorId) {
        if (donorId == null) {
            throw new BusinessException("Sign in as a donor to accept a request.");
        }
        BloodRequest r = requireLocked(code);
        Donor donor = donors.findById(donorId)
                .orElseThrow(() -> new NotFoundException("Unknown donor: " + donorId));

        BloodGroup recipient = r.getBloodGroup();
        if (recipient == null || donor.getBloodGroup() == null
                || !recipient.acceptsDonor(donor.getBloodGroup())) {
            // Wording avoids the a/an problem with group labels and states the
            // rule in the direction it is defined (recipient accepts donor).
            throw new BusinessException("Donor group " + donor.getBloodGroup().getLabel()
                    + " cannot donate to a " + (recipient == null ? "?" : recipient.getLabel())
                    + " recipient — see the compatibility chart (BR-1).");
        }

        if (r.getDonor() != null) {
            String who = r.getDonor().getUser() == null ? "another donor" : r.getDonor().getUser().getName();
            throw new BusinessException(r.getPublicCode() + " has already been accepted by " + who + ".");
        }

        requireTransition(r, RequestStatus.ACCEPTED);
        r.setDonor(donor);
        r.setStatus(RequestStatus.ACCEPTED);
        r.setMatchedAt(Instant.now());
        stamp(r, donor.getUser() == null ? "A donor accepted the request" : donor.getUser().getName()
                + " accepted the request" + (r.getHospital() == null ? "" : " — " + r.getHospital().getLabel() + " notified"));
        requests.save(r);

        notifications.notifyRequester(r, NotificationType.STATUS_UPDATE, "a donor has accepted your request");
        return RequestResponse.of(r);
    }

    /**
     * BR-7 — a donor's "no": recorded against the DONOR, and the request is left
     * completely alone (no status change, no timeline entry, no notification).
     * Idempotent, so a double tap does not fail.
     */
    @Transactional
    public void declineByDonor(String code, Long donorId) {
        if (donorId == null) {
            throw new BusinessException("Sign in as a donor to decline a request.");
        }
        BloodRequest r = require(code);
        if (declines.existsByDonor_DonorIdAndRequest_RequestId(donorId, r.getRequestId())) {
            return;
        }
        Donor donor = donors.findById(donorId)
                .orElseThrow(() -> new NotFoundException("Unknown donor: " + donorId));
        RequestDecline decline = new RequestDecline();
        decline.setDonor(donor);
        decline.setRequest(r);
        declines.save(decline);
    }

    /**
     * Hospital staff take a request on (PENDING → ACCEPTED). BR-5: they may only
     * touch a request already in their own queue.
     */
    @Transactional
    public RequestResponse acceptByHospital(String code, Long hospitalId) {
        BloodRequest r = requireLocked(code);
        requireTransition(r, RequestStatus.ACCEPTED);
        if (hospitalId != null) {
            Hospital h = hospitals.findById(hospitalId)
                    .orElseThrow(() -> new NotFoundException("Unknown hospital: " + hospitalId));
            if (r.getHospital() != null && !r.getHospital().getHospitalId().equals(h.getHospitalId())) {
                throw new BusinessException(r.getPublicCode() + " belongs to "
                        + r.getHospital().getLabel() + ", not " + h.getLabel() + ".");
            }
            r.setHospital(h);
            r.setHospitalLabel(h.getLabel());
            stamp(r, h.getLabel() + " accepted the request — blood is being arranged");
        } else {
            stamp(r, "Request accepted — hospital is arranging the blood");
        }
        r.setStatus(RequestStatus.ACCEPTED);
        if (r.getMatchedAt() == null) {
            r.setMatchedAt(Instant.now());
        }
        requests.save(r);
        return RequestResponse.of(r);
    }

    /**
     * Admin routing: put an unrouted request into a hospital's queue. Deliberately
     * NOT a match — the request stays PENDING until a donor or the hospital
     * accepts it (the demo's forward button also sent {@code status: "Accepted"};
     * see {@link #applyUpdate} for why that label is translated rather than
     * trusted).
     */
    @Transactional
    public RequestResponse assignHospital(String code, Long hospitalId, ActorContext actor) {
        if (actor == null || !actor.isAdmin()) {
            throw new BusinessException("Only an administrator can route a request to a hospital.");
        }
        BloodRequest r = require(code);
        if (r.getStatus().isTerminal()) {
            throw new BusinessException("This request is already " + r.getStatus().label().toLowerCase() + ".");
        }
        Hospital h = hospitals.findById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Unknown hospital: " + hospitalId));
        r.setHospital(h);
        r.setHospitalLabel(h.getLabel());
        stamp(r, "Forwarded to " + h.getLabel() + " by platform admin");
        requests.save(r);
        return RequestResponse.of(r);
    }

    /**
     * BR-2 (ACCEPTED → FULFILLED) and BR-4 (deduct the units from that hospital's
     * stock, never below zero). The stock deduction happens in the same
     * transaction as the status change, so a request cannot be marked fulfilled
     * while the inventory write fails.
     */
    @Transactional
    public RequestResponse fulfil(String code, Long hospitalId) {
        BloodRequest r = requireLocked(code);
        requireTransition(r, RequestStatus.FULFILLED);

        Long target = hospitalId != null ? hospitalId
                : r.getHospital() == null ? null : r.getHospital().getHospitalId();
        if (target == null) {
            throw new BusinessException("Assign this request to a hospital before marking it fulfilled —"
                    + " fulfilment deducts that hospital's stock.");
        }
        if (r.getHospital() != null && !r.getHospital().getHospitalId().equals(target)) {
            throw new BusinessException(r.getPublicCode() + " belongs to " + r.getHospital().getLabel()
                    + ", not hospital " + target + ".");
        }

        int issued = inventory.decrement(target, r.getBloodGroup(), r.getUnits());
        r.setStatus(RequestStatus.FULFILLED);
        r.setFulfilledAt(Instant.now());
        stamp(r, issued > 0
                ? "Donation completed & verified — " + issued + " unit" + (issued == 1 ? "" : "s")
                        + " of " + r.getBloodGroup().getLabel() + " issued from " + r.getHospitalLabel()
                : "Donation completed & verified — no stock was deducted (the hospital had none of "
                        + r.getBloodGroup().getLabel() + ")");
        requests.save(r);

        notifications.notifyRequester(r, NotificationType.STATUS_UPDATE, "your request has been fulfilled");
        return RequestResponse.of(r);
    }

    /** BR-2: the requester (or an admin) withdraws a request that is not already terminal. */
    @Transactional
    public RequestResponse cancel(String code, ActorContext actor) {
        BloodRequest r = require(code);
        requireTransition(r, RequestStatus.CANCELLED);
        boolean owner = r.getRequester() != null && actor != null && actor.userId() != null
                && actor.userId().equals(r.getRequester().getUserId());
        if (!owner && !(actor != null && actor.isAdmin())) {
            throw new BusinessException("Only the requester can cancel this request.");
        }
        r.setStatus(RequestStatus.CANCELLED);
        stamp(r, "Cancelled by " + (actor != null && actor.isAdmin() ? "platform admin" : "the requester"));
        requests.save(r);
        return RequestResponse.of(r);
    }

    /** Admin moderation: flag a request as spam/withdrawn (BR-2 → REJECTED). */
    @Transactional
    public RequestResponse reject(String code, ActorContext actor) {
        if (actor == null || !actor.isAdmin()) {
            throw new BusinessException("Only an administrator can flag a request.");
        }
        BloodRequest r = require(code);
        requireTransition(r, RequestStatus.REJECTED);
        r.setStatus(RequestStatus.REJECTED);
        stamp(r, "Flagged as spam by platform admin");
        requests.save(r);
        return RequestResponse.of(r);
    }

    /* --------------------------------------------------- the demo's generic PATCH */

    /**
     * Routes {@code PATCH /api/requests/{id}} into the explicit operations above.
     *
     * <p>This exists because the pages send one generic patch shape from five
     * different screens, and the real-mode flip (B7) should not need a page
     * rewrite. It is a TRANSLATOR, not a field writer — no branch below sets a
     * status without going through a method that checks the rule for it.
     *
     * <p>The delicate one is {@code status: "Accepted"}, which the demo used for
     * three different events. It is disambiguated by WHO is asking:
     * a donor session = a real match (BR-1/BR-3); an admin with a hospital in the
     * body = routing (status stays Pending, because forwarding is not matching);
     * hospital staff = they take the request on.
     */
    @Transactional
    public RequestResponse applyUpdate(String code, RequestUpdateRequest patch, ActorContext actor) {
        if (patch == null) {
            throw new BusinessException("Empty update.");
        }
        BloodRequest r = require(code);

        if (isBlank(patch.status())) {
            if (patch.declinedBy() != null && !patch.declinedBy().isEmpty()) {
                declineByDonor(code, actor == null ? null : actor.donorId());
                return RequestResponse.of(require(code));
            }
            if (patch.notes() != null) {
                r.setNotes(isBlank(patch.notes()) ? null : patch.notes().trim());
                requests.save(r);
                return RequestResponse.of(r);
            }
            throw new BusinessException("Nothing to update.");
        }

        RequestStatus target = parseStatus(patch.status());
        return switch (target) {
            case CANCELLED -> cancel(code, actor);
            case REJECTED -> reject(code, actor);
            case FULFILLED -> fulfil(code, patch.hospitalId() != null
                    ? patch.hospitalId() : actor == null ? null : actor.hospitalId());
            case PENDING -> throw new BusinessException("A request cannot be moved back to Pending.");
            case ACCEPTED -> {
                if (actor != null && actor.donorId() != null) {
                    yield acceptByDonor(code, actor.donorId());
                }
                Long routed = patch.hospitalId() != null
                        ? patch.hospitalId() : hospitalIdOfLabel(patch.hospital());
                if (routed != null) {
                    yield assignHospital(code, routed, actor);
                }
                yield acceptByHospital(code, actor == null ? null : actor.hospitalId());
            }
        };
    }

    /* ------------------------------------------------------------------ internals */

    /**
     * BR-2 in one place — the only legal state changes:
     * PENDING → ACCEPTED | CANCELLED | REJECTED; ACCEPTED → FULFILLED | CANCELLED |
     * REJECTED; and nothing out of the terminal states.
     */
    private void requireTransition(BloodRequest r, RequestStatus to) {
        RequestStatus from = r.getStatus();
        boolean allowed = switch (from) {
            case PENDING -> to == RequestStatus.ACCEPTED || to == RequestStatus.CANCELLED || to == RequestStatus.REJECTED;
            case ACCEPTED -> to == RequestStatus.FULFILLED || to == RequestStatus.CANCELLED || to == RequestStatus.REJECTED;
            case FULFILLED, CANCELLED, REJECTED -> false;
        };
        if (!allowed) {
            throw new BusinessException("A " + from.label().toLowerCase() + " request cannot become "
                    + to.label().toLowerCase() + ".");
        }
    }

    /** Append one audit line — the server's own, never the client's. */
    private void stamp(BloodRequest r, String what) {
        r.getTimeline().add(new BloodRequest.RequestTimelineEntry(Instant.now(), what));
    }

    private BloodRequest require(String code) {
        if (isBlank(code)) {
            throw new NotFoundException("No request id given.");
        }
        return requests.findByPublicCode(code.trim())
                .orElseThrow(() -> new NotFoundException("Unknown request: " + code));
    }

    /** Same lookup, under a write lock — the BR-3 accept path. */
    private BloodRequest requireLocked(String code) {
        BloodRequest found = require(code);
        return requests.findWithLockByRequestId(found.getRequestId())
                .orElseThrow(() -> new NotFoundException("Unknown request: " + code));
    }

    /**
     * The public code. A client may PIN one (Postman / the QA harness need a code
     * they can look up later) — validated, prefix-matched and uniqueness-checked
     * rather than trusted; otherwise a 6-character code is generated.
     */
    private String resolveCode(String requested, boolean guest) {
        String prefix = guest ? "EM-" : "BB-";
        if (!isBlank(requested)) {
            String code = requested.trim().toUpperCase();
            if (!PINNED_CODE.matcher(code).matches()) {
                throw new BusinessException("Request ids look like BB-8F3K2M or EM-K3P9QA.");
            }
            if (!code.startsWith(prefix)) {
                throw new BusinessException(prefix + " is the prefix for this kind of request.");
            }
            if (requests.findByPublicCode(code).isPresent()) {
                throw new BusinessException("Request id " + code + " is already in use.");
            }
            return code;
        }
        for (int attempt = 0; attempt < 12; attempt++) {
            StringBuilder sb = new StringBuilder(prefix);
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (requests.findByPublicCode(code).isEmpty()) {
                return code;
            }
        }
        throw new BusinessException("Could not allocate a request id — please try again.");
    }

    /**
     * Resolve the submitted hospital: id first, then the label. This is the
     * server-side replacement for {@code bb-store.js}'s {@code normalizeHospital}
     * alias table — and it closes the session #9 bug for good: a variant label
     * ("TUTH" / "Tribhuvan University Teaching Hospital" / "TUTH, Maharajgunj")
     * now resolves to the SAME row, so a request cannot fall out of the hospital
     * queue's equality filter.
     */
    /* Hospital id/label resolution moved to its own component in session #22 —
       registration needs exactly the same "TUTH" → row mapping that a request
       does, and two copies of an alias table is how they drift apart. */

    /** The label stored for display: the canonical row label when resolved, else what was sent. */
    private String hospitalLabelFor(Hospital h, String submitted) {
        if (h != null) {
            return h.getLabel();
        }
        String v = submitted == null ? "" : submitted.trim();
        return v.isEmpty() ? RequestResponse.NO_HOSPITAL : v;
    }

    private Long hospitalIdOfLabel(String label) {
        return hospitalResolver.idOfLabel(label);
    }

    private User signedIn(ActorContext actor) {
        if (actor == null || actor.userId() == null) {
            throw new BusinessException("Sign in, or use the public emergency form for a guest request.");
        }
        return users.findById(actor.userId())
                .orElseThrow(() -> new NotFoundException("Unknown account."));
    }

    /** ISO date → LocalDate, with the rule's own message instead of a Jackson stack trace. */
    private static LocalDate parseDate(String iso) {
        if (isBlank(iso)) {
            throw new BusinessException("A needed-by date is required.");
        }
        try {
            return LocalDate.parse(iso.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("Use a date like 2026-09-20 for the needed-by field.");
        }
    }

    private static BloodGroup parseGroup(String blood) {
        try {
            return BloodGroup.fromLabel(blood);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    private static RequestStatus parseStatus(String label) {
        try {
            return RequestStatus.fromLabel(label);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
