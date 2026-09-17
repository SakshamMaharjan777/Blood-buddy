package com.bloodbuddy.web;

import com.bloodbuddy.dto.ApiAck;
import com.bloodbuddy.dto.DonorResponse;
import com.bloodbuddy.dto.DonorSearchResult;
import com.bloodbuddy.dto.HospitalOption;
import com.bloodbuddy.dto.PageResponse;
import com.bloodbuddy.dto.PhotoUploadRequest;
import com.bloodbuddy.model.BloodGroup;
import com.bloodbuddy.service.DonorService;
import com.bloodbuddy.service.NotFoundException;
import com.bloodbuddy.service.PhotoCodec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code /api/donors/*} (proposal Appendix B) — search, one donor, and the §2.5
 * photograph (stored in the database as a BLOB and served back as bytes, never a
 * file path).
 *
 * <p>Two shape details that matter for the real-mode flip:
 * <ul>
 *   <li>{@code GET /api/donors/search} returns the ARRAY by default, because
 *       {@code requester-search.html} iterates the resolved value directly. The
 *       widen-fallback explanation rides along only when asked for with
 *       {@code ?withMeta=true} ({@link DonorSearchResult}).</li>
 *   <li>{@code /search} is a literal segment and Spring's path matching prefers it
 *       over {@code /{id}} — the ordering bug the demo's own mock router had to be
 *       fixed for in session #10 cannot happen here, but the comment is cheaper
 *       than the rediscovery.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
public class DonorController {

    private final DonorService donors;

    /**
     * Donor search. {@code blood} is the RECIPIENT group the search is run for —
     * it decides the {@code compat} flag on every card (BR-1), so it is never
     * optional in the response sense even though the parameter is.
     */
    @GetMapping("/search")
    public Object search(@RequestParam(required = false) String blood,
                         @RequestParam(required = false) String district,
                         @RequestParam(required = false) Boolean available,
                         @RequestParam(defaultValue = "false") boolean withMeta,
                         @RequestParam(required = false) Integer page,
                         @RequestParam(required = false) Integer size) {
        DonorSearchResult result = donors.search(blood, district, available);
        if (page != null) {
            // Additive pagination: only a caller that asks for a page gets the
            // envelope; everyone else still gets the bare array the page iterates.
            return PageResponse.of(result.donors(), page, size == null ? 10 : size);
        }
        return withMeta ? result : result.donors();
    }

    /** One donor card. {@code blood} (optional) is again the recipient group for the compat flag. */
    @GetMapping("/{id}")
    public DonorResponse get(@PathVariable Long id, @RequestParam(required = false) String blood) {
        return donors.get(id, blood == null || blood.isBlank() ? null : BloodGroup.fromLabel(blood));
    }

    /** §2.5 upload: a Base64 data URL from the profile/register picker → the donor's BLOB column. */
    @PostMapping("/{id}/photo")
    public ApiAck uploadPhoto(@PathVariable Long id, @Valid @RequestBody PhotoUploadRequest request) {
        donors.savePhoto(id, request.dataUrl());
        return ApiAck.of("Photo stored in the database.");
    }

    /** §2.5 retrieval: the stored image itself, with its sniffed content type. */
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        byte[] bytes = donors.photoBytes(id);
        if (bytes.length == 0) {
            throw new NotFoundException("That donor has no photo.");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(PhotoCodec.contentType(bytes)))
                .body(bytes);
    }

    /** Convenience for pages that embed the image in a data URL (same codec as the BLOB). */
    @GetMapping("/{id}/photo.json")
    public ApiAck photoDataUrl(@PathVariable Long id) {
        String dataUrl = donors.photoDataUrl(id);
        return new ApiAck(dataUrl != null, dataUrl);
    }

    /** Every compatible donor for a recipient group — the alert set, exposed for the UI to show. */
    @GetMapping("/compatible")
    public List<DonorResponse> compatible(@RequestParam String blood) {
        return donors.compatibleCards(BloodGroup.fromLabel(blood));
    }

    /**
     * The blood banks this donor is registered with — the Donor ↔ Hospital M:N
     * (CPJ119 §2.3) read from the donor's side. The hospital side of the same
     * relationship is a repository join, since "who is affiliated here?" is a
     * portal question rather than a profile one.
     */
    @GetMapping("/{id}/hospitals")
    public List<HospitalOption> hospitals(@PathVariable Long id) {
        return donors.affiliatedHospitals(id);
    }
}
