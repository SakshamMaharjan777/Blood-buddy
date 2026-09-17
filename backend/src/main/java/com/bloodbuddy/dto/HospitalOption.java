package com.bloodbuddy.dto;

import com.bloodbuddy.model.Hospital;

/**
 * One hospital as a list/picker item: the id plus the labels the pages and forms
 * show. Used by the Donor ↔ Hospital affiliation read
 * ({@code GET /api/donors/{id}/hospitals}) and usable by any hospital dropdown.
 *
 * <p>{@code label} is the canonical {@code "name, area"} the rest of the system
 * stores on requests ("TUTH, Maharajgunj"); {@code shortName} is what people
 * actually type ("TUTH", "NMC") and what the resolver matches against. Both are
 * exposed because the forms need to SHOW one and may SUBMIT the other — which is
 * exactly the ambiguity {@code HospitalResolver} exists to absorb.
 */
public record HospitalOption(
        String id,
        String label,
        String shortName,
        String district,
        String area
) {

    public static HospitalOption of(Hospital h) {
        return new HospitalOption(
                String.valueOf(h.getHospitalId()),
                h.getLabel(),
                h.getShortName(),
                h.getDistrict(),
                h.getArea());
    }
}
