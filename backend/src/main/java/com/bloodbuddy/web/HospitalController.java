package com.bloodbuddy.web;

import com.bloodbuddy.dto.DonorResponse;
import com.bloodbuddy.dto.InventoryBoard;
import com.bloodbuddy.dto.InventoryRow;
import com.bloodbuddy.dto.InventoryUpdateRequest;
import com.bloodbuddy.dto.RequestCreateRequest;
import com.bloodbuddy.dto.RequestResponse;
import com.bloodbuddy.model.BloodGroup;
import com.bloodbuddy.model.Hospital;
import com.bloodbuddy.repository.HospitalRepository;
import com.bloodbuddy.service.ActorContext;
import com.bloodbuddy.service.BusinessException;
import com.bloodbuddy.service.DonorService;
import com.bloodbuddy.service.InventoryService;
import com.bloodbuddy.service.NotFoundException;
import com.bloodbuddy.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * {@code /api/hospitals/*} (proposal Appendix B) — the blood-bank board.
 *
 * <p><b>Access:</b> hospital staff and administrators only ({@code @PreAuthorize} +
 * the URL rule in {@code config/SecurityConfig}).
 *
 * <p><b>BR-5 scoping:</b> a hospital account may only touch its own stock, and
 * this controller enforces it by preferring the actor's hospital over anything the
 * caller passes: if the signed-in account staffs a hospital, {@code ?hospitalId}
 * is ignored, so staff cannot cross hospitals even deliberately. The parameter
 * remains for administrators (who legitimately read any board) and for the seeded
 * demo accounts whose staff list is not fixed up yet.
 *
 * <p>Responses are the plain {@code { "O+": 24, … }} map, because
 * {@code hospital-dashboard.html} iterates the board with {@code Object.keys()}
 * and fills its own table.
 */
@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HOSPITAL','ADMIN')")
public class HospitalController {

    private final InventoryService inventory;
    private final HospitalRepository hospitals;
    private final RequestService requests;
    private final DonorService donorService;

    /** The scoped board the dashboard renders (the pages send no hospital id — the session decides). */
    @GetMapping("/inventory")
    public Map<String, Integer> board(@RequestParam(required = false) Long hospitalId, ActorContext actor) {
        return inventory.board(scopedHospital(hospitalId, actor)).units();
    }

    /** Absolute stock set for one group (the adjust-stock modal's Save). */
    @PutMapping("/inventory/{bloodGroup}")
    public InventoryRow setUnits(@PathVariable String bloodGroup,
                                 @Valid @RequestBody InventoryUpdateRequest request,
                                 @RequestParam(required = false) Long hospitalId,
                                 ActorContext actor) {
        BloodGroup group = BloodGroup.fromLabel(bloodGroup);
        InventoryBoard board = inventory.setUnits(scopedHospital(hospitalId, actor), group, request.units());
        return new InventoryRow(group.getLabel(), board.units().getOrDefault(group.getLabel(), 0));
    }

    /** Appendix B's explicit form: the board of one named hospital. */
    @GetMapping("/{hospitalId}/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> boardOf(@PathVariable Long hospitalId) {
        return inventory.board(hospitalId).units();
    }

    /* ============================ Proposal Appendix B ============================
       The five-hospital "local API" shape. Every one of these is a THIN delegate:
       the logic already lives in the services (InventoryService / RequestService /
       DonorService), and these routes only give it the appendix's path names so the
       proposal and the code agree. BR-5 is unchanged and un-duplicated here —
       requireScopedAccess() below is the same rule the rest of the controller uses,
       expressed for a hospital id that arrives IN THE PATH.
       ========================================================================== */

    /** Appendix B: {@code POST /api/hospitals/inventory} — absolute stock set, scoped. */
    @PostMapping("/inventory")
    public InventoryRow postBoard(@RequestParam String bloodGroup,
                                  @Valid @RequestBody InventoryUpdateRequest request,
                                  @RequestParam(required = false) Long hospitalId,
                                  ActorContext actor) {
        BloodGroup group = BloodGroup.fromLabel(bloodGroup);
        InventoryBoard board = inventory.setUnits(scopedHospital(hospitalId, actor), group, request.units());
        return new InventoryRow(group.getLabel(), board.units().getOrDefault(group.getLabel(), 0));
    }

    /**
     * Appendix B: {@code PUT /api/hospitals/{hospitalId}/inventory/{bloodGroup}} —
     * the same stock set, addressed by the hospital in the path. A staff account
     * may only name its OWN hospital (BR-5); an administrator may name any.
     */
    @PutMapping("/{hospitalId}/inventory/{bloodGroup}")
    public InventoryRow setUnitsAt(@PathVariable Long hospitalId,
                                   @PathVariable String bloodGroup,
                                   @Valid @RequestBody InventoryUpdateRequest request,
                                   ActorContext actor) {
        requireScopedAccess(hospitalId, actor);
        BloodGroup group = BloodGroup.fromLabel(bloodGroup);
        InventoryBoard board = inventory.setUnits(hospitalId, group, request.units());
        return new InventoryRow(group.getLabel(), board.units().getOrDefault(group.getLabel(), 0));
    }

    /**
     * Appendix B: {@code GET /api/hospitals/{hospitalId}/donors/nearby} — the
     * registered donors near the hospital's own district, optionally filtered to
     * those who may serve {@code ?blood=}. Delegates to the donor search so the
     * BR-1 direction and the availability rule are the same ones the registry uses.
     */
    @GetMapping("/{hospitalId}/donors/nearby")
    public List<DonorResponse> donorsNearby(@PathVariable Long hospitalId,
                                            @RequestParam(required = false) String blood,
                                            ActorContext actor) {
        requireScopedAccess(hospitalId, actor);
        Hospital h = requireHospital(hospitalId);
        return donorService.search(blood, h.getDistrict(), true).donors();
    }

    /**
     * Appendix B: {@code POST /api/hospitals/{hospitalId}/requests} — submit a
     * request straight into that hospital's queue. The hospital comes from the
     * PATH, so a body-supplied hospital is overwritten rather than trusted — the
     * same "identity never from the body" rule the request service already keeps
     * for the donor and the hospital.
     */
    @PostMapping("/{hospitalId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponse submitToHospital(@PathVariable Long hospitalId,
                                            @Valid @RequestBody RequestCreateRequest body,
                                            ActorContext actor) {
        requireScopedAccess(hospitalId, actor);
        requireHospital(hospitalId);
        RequestCreateRequest routed = new RequestCreateRequest(
                body.id(), body.emergency(), body.blood(), body.units(), body.urgency(),
                body.neededBy(), null, hospitalId, body.district(), body.notes(), body.contactPhone());
        return requests.create(routed, actor);
    }

    /**
     * Appendix B: {@code GET /api/hospitals/{hospitalId}/requests/{requestId}/status}
     * — the fulfilment status of one request in that hospital's queue. A request
     * that is not in this hospital's queue answers 404, so the route cannot be used
     * to read another hospital's request (BR-5).
     */
    @GetMapping("/{hospitalId}/requests/{requestId}/status")
    public RequestResponse requestStatus(@PathVariable Long hospitalId,
                                         @PathVariable String requestId,
                                         ActorContext actor) {
        requireScopedAccess(hospitalId, actor);
        Hospital h = requireHospital(hospitalId);
        RequestResponse r = requests.get(requestId);
        if (!h.getLabel().equals(r.hospital())) {
            throw new NotFoundException("No request " + requestId + " in " + h.getLabel() + "'s queue.");
        }
        return r;
    }

    /**
     * The id the write/read is allowed to touch: the actor's own hospital when they
     * are hospital staff (BR-5), else the caller's parameter (dev/admin), else null
     * — and the service refuses null with "No hospital selected."
     */
    private static Long scopedHospital(Long requested, ActorContext actor) {
        return actor.hospitalId() != null ? actor.hospitalId() : requested;
    }

    /**
     * BR-5 for a hospital id taken from the PATH: an administrator may address any
     * hospital; a staff account may address only its own. A staff account with no
     * hospital link cannot use a path-scoped hospital route at all — it has nothing
     * to scope to, and silently treating that as "admin" would be the wrong default.
     */
    private static void requireScopedAccess(Long hospitalId, ActorContext actor) {
        if (actor == null || actor.isAdmin()) {
            return;
        }
        if (actor.hospitalId() == null || !actor.hospitalId().equals(hospitalId)) {
            throw new BusinessException("Hospital staff may only access their own hospital's data (BR-5).");
        }
    }

    private Hospital requireHospital(Long hospitalId) {
        return hospitals.findById(hospitalId)
                .orElseThrow(() -> new NotFoundException("Unknown hospital: " + hospitalId));
    }
}
