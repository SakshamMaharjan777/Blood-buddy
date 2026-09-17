package com.bloodbuddy.service;

import com.bloodbuddy.model.Hospital;
import com.bloodbuddy.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Turns a SUBMITTED hospital reference into a database row. One implementation,
 * two callers: the request flow (a request names the hospital it needs blood
 * from) and registration (hospital staff say where they work).
 *
 * <p>It exists because the alternative was the same alias table twice. The
 * frontend's forms submit free text — {@code "TUTH, Maharajgunj"} from a
 * dropdown, {@code "Bir Hospital"} or {@code "NMC"} typed by hand — and
 * {@code bb-store.js} used to normalise all of that in the browser. §2.2 moved
 * that knowledge server-side, and this component is where it lives, so the
 * request queue and a newly registered staff account can never disagree about
 * which row {@code "TUTH"} means.
 *
 * <p>Resolution order, cheapest and most exact first:
 * <ol>
 *   <li>an id, when the client already knows the row (authoritative, 404s if unknown);</li>
 *   <li>the {@code "name, area"} label form the dropdowns submit, against both the
 *       full name and the short name;</li>
 *   <li>the bare name or short name ({@code "TUTH"}, {@code "Bir Hospital"}).</li>
 * </ol>
 *
 * <p><b>Unresolved is not an error here.</b> A label that matches no row returns
 * {@code null}, because the callers need different things from that: a request
 * legitimately has NO hospital until an administrator routes it (the "Not routed
 * yet" bucket), and a staff account that names a hospital we do not have yet
 * should still be created — with a warning — rather than rejected. An id, on the
 * other hand, is a claim about a row that must exist, so it throws.
 */
@Component
@RequiredArgsConstructor
public class HospitalResolver {

    /** The demo's "no hospital yet" spellings, taken from the forms. */
    private static final String OTHER = "other";

    private final HospitalRepository hospitals;

    /** @return the row, or {@code null} when a label matched nothing (see the class javadoc) */
    public Hospital resolve(Long hospitalId, String label) {
        if (hospitalId != null) {
            return hospitals.findById(hospitalId)
                    .orElseThrow(() -> new NotFoundException("Unknown hospital: " + hospitalId));
        }
        String v = label == null ? "" : label.trim();
        if (v.isEmpty() || "-".equals(v) || "—".equals(v) || OTHER.equalsIgnoreCase(v)) {
            return null;   // unrouted: the admin queue's "Not routed yet" bucket
        }
        int comma = v.indexOf(',');
        if (comma > 0) {
            String name = v.substring(0, comma).trim();
            String area = v.substring(comma + 1).trim();
            Hospital hit = hospitals.findByNameIgnoreCaseAndAreaIgnoreCase(name, area)
                    .or(() -> hospitals.findByShortNameIgnoreCaseAndAreaIgnoreCase(name, area))
                    .orElse(null);
            if (hit != null) {
                return hit;
            }
        }
        // Long name, then short name ("TUTH", "NMC") — the two shapes the demo's
        // forms and seeds both use.
        return hospitals.findByNameIgnoreCase(v)
                .or(() -> hospitals.findByShortNameIgnoreCase(v))
                .orElse(null);
    }

    /** The same resolution when only the id matters (the PATCH translator's "route to this label"). */
    public Long idOfLabel(String label) {
        Hospital h = resolve(null, label);
        return h == null ? null : h.getHospitalId();
    }
}
