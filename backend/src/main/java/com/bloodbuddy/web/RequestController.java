package com.bloodbuddy.web;

import com.bloodbuddy.dto.PageResponse;
import com.bloodbuddy.dto.RequestCreateRequest;
import com.bloodbuddy.dto.RequestResponse;
import com.bloodbuddy.dto.RequestUpdateRequest;
import com.bloodbuddy.service.ActorContext;
import com.bloodbuddy.service.BusinessException;
import com.bloodbuddy.service.DonorService;
import com.bloodbuddy.service.HospitalResolver;
import com.bloodbuddy.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code /api/requests*} (proposal Appendix B) — the lifecycle endpoints.
 *
 * <p>The interesting method here is {@link #list}: the demo used ONE collection
 * endpoint for five different screens, so the filter the caller passes decides
 * which reader's queue answers. That routing is HTTP-level dispatch (which queue
 * did you ask for); every rule INSIDE a queue — BR-1 matching, BR-5 scoping, BR-7
 * declines — is the service's.
 *
 * <pre>
 *   ?requester=… | ?requesterId=…   the requester's tracker
 *   ?hospital=…  | ?hospitalId=…    one hospital's queue (BR-5)
 *   ?status=…                       narrow any of the above
 *   ?guest=true                     admin: guest emergency submissions
 *   ?unrouted=true                  admin: not yet assigned to a hospital
 *   (nothing)                       the signed-in donor's board, else every request
 * </pre>
 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requests;
    private final DonorService donors;
    private final HospitalResolver hospitalResolver;

    @GetMapping
    public Object list(@RequestParam(required = false) String requester,
                       @RequestParam(required = false) Long requesterId,
                       @RequestParam(required = false) String hospital,
                       @RequestParam(required = false) Long hospitalId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) Boolean guest,
                       @RequestParam(required = false) Boolean unrouted,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       ActorContext actor) {
        Long scopedHospitalId = hospitalId;
        if (actor != null && actor.isHospital()) {
            /*
             * BR-5 on the GENERIC queue endpoint. The path-scoped routes under
             * /api/hospitals/{id}/... already enforce it; this list is the same
             * data behind a query parameter, and until now it was not gated, so a
             * TUTH session could read Patan's queue with ?hospitalId=8 (found by
             * walking the demo runbook, not by reading the code).
             *
             * A staff account is therefore pinned to its own hospital here, and
             * the admin-only views (?guest, ?unrouted, ?requester[Id]) are refused
             * outright — a hospital has no moderation queue to read.
             */
            Long own = actor.hospitalId();
            if (own == null) {
                throw new BusinessException(BR5);
            }
            if (hospitalId != null && !own.equals(hospitalId)) {
                throw new BusinessException(BR5);
            }
            if (!isBlank(hospital)) {
                Long asked = hospitalResolver.idOfLabel(hospital);
                if (asked != null && !own.equals(asked)) {
                    throw new BusinessException(BR5);
                }
            }
            if (Boolean.TRUE.equals(guest) || Boolean.TRUE.equals(unrouted)
                    || requesterId != null || !isBlank(requester)) {
                throw new BusinessException(BR5);
            }
            // No filter at all means "my queue", never the whole table.
            scopedHospitalId = own;
        }
        List<RequestResponse> rows;
        if (Boolean.TRUE.equals(guest)) {
            rows = requests.forAdmin(status, true, false);
        } else if (Boolean.TRUE.equals(unrouted)) {
            rows = requests.forAdmin(status, false, true);
        } else if (scopedHospitalId != null) {
            rows = requests.byHospital(scopedHospitalId, status);
        } else if (!isBlank(hospital)) {
            rows = requests.byHospitalLabel(hospital, status);
        } else if (requesterId != null) {
            rows = requests.byRequester(requesterId);
        } else if (!isBlank(requester)) {
            rows = requests.byRequesterRef(requester);
        } else if (!isBlank(status)) {
            // `?status=` ALONE is a filter too: it used to fall through to the
            // unfiltered list below, so `?status=Pending` answered every request
            // (found by running the Postman collection for real, not by reading it).
            rows = requests.forAdmin(status, false, false);
        } else if (actor.donorId() != null) {
            // No filter: a signed-in donor gets THEIR board (BR-1 compatible, BR-7
            // declines removed) — which is exactly what donor-dashboard.html renders
            // from requests.list({}). Everyone else (admin moderation, dashboards)
            // gets the full list, as the demo's mock did.
            rows = donors.requestsNearYou(actor.donorId());
        } else {
            rows = requests.all();
        }
        // Additive pagination (see PageResponse): the array by default, a paged
        // envelope only when ?page= is supplied.
        return page == null ? rows : PageResponse.of(rows, page, size == null ? 10 : size);
    }

    @GetMapping("/{code}")
    public RequestResponse get(@PathVariable String code) {
        return requests.get(code);
    }

    /**
     * BR-8 — the public "check your request" card. Guest ({@code EM-}) requests
     * only: a member's code answers 404 here, so the public page cannot be used
     * to confirm that somebody else's request exists.
     */
    @GetMapping("/lookup/{code}")
    public RequestResponse lookup(@PathVariable String code) {
        return requests.guestLookup(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponse create(@Valid @RequestBody RequestCreateRequest body, ActorContext actor) {
        return requests.create(body, actor);
    }

    /**
     * The demo's one generic patch. {@code applyUpdate} translates it into the
     * explicit, rule-checked operations — including disambiguating
     * {@code status: "Accepted"}, which the pages use for a donor match, an admin
     * forward and a hospital takeover alike.
     */
    @PatchMapping("/{code}")
    public RequestResponse update(@PathVariable String code,
                                 @RequestBody RequestUpdateRequest patch,
                                 ActorContext actor) {
        return requests.applyUpdate(code, patch, actor);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** The one BR-5 wording, so the query-param gate and HospitalController agree. */
    private static final String BR5 =
            "Hospital staff may only access their own hospital's data (BR-5).";
}
