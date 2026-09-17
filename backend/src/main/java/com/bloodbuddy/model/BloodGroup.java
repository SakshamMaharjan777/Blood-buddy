package com.bloodbuddy.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * The eight blood groups, carrying the Appendix A compatibility chart
 * (Table 3 of the proposal). The map is written in the RECIPIENT → COMPATIBLE
 * DONOR GROUPS direction — the same direction the chart is printed in — which
 * is exactly the direction the donor-matching service (B2) must apply it in.
 *
 * Direction matters: reading the chart backwards would offer a donor a patient
 * they must not give blood to. See report business rule BR-1 and the comment
 * in CODE/js/bb-compat.js (the frontend copy of this chart, which becomes a
 * presentation-only helper once the backend is authoritative).
 */
public enum BloodGroup {
    O_MINUS("O-"),
    O_PLUS("O+"),
    A_MINUS("A-"),
    A_PLUS("A+"),
    B_MINUS("B-"),
    B_PLUS("B+"),
    AB_MINUS("AB-"),
    AB_PLUS("AB+");

    private final String label;

    BloodGroup(String label) {
        this.label = label;
    }

    /** Human-readable label, identical to the frontend's strings ("O+", "AB-", …). */
    public String getLabel() {
        return label;
    }

    /**
     * Blood groups whose carriers may donate TO a recipient of this group
     * (Appendix A, recipient → compatible donor groups).
     */
    public Set<BloodGroup> getCompatibleDonorGroups() {
        return switch (this) {
            case AB_PLUS  -> EnumSet.allOf(BloodGroup.class);            // universal recipient
            case AB_MINUS -> Set.of(AB_MINUS, A_MINUS, B_MINUS, O_MINUS);
            case A_PLUS   -> Set.of(A_PLUS, A_MINUS, O_PLUS, O_MINUS);
            case A_MINUS  -> Set.of(A_MINUS, O_MINUS);
            case B_PLUS   -> Set.of(B_PLUS, B_MINUS, O_PLUS, O_MINUS);
            case B_MINUS  -> Set.of(B_MINUS, O_MINUS);
            case O_PLUS   -> Set.of(O_PLUS, O_MINUS);
            case O_MINUS  -> Set.of(O_MINUS);                            // universal donor
        };
    }

    /** Convenience for B2's matching: can a donor with group {@code donor} serve a recipient with group {@code recipient}? */
    public boolean acceptsDonor(BloodGroup donor) {
        return getCompatibleDonorGroups().contains(donor);
    }

    /** Parse a label ("O+", "AB-") from a request/DTO. */
    public static BloodGroup fromLabel(String label) {
        for (BloodGroup g : values()) {
            if (g.label.equalsIgnoreCase(label == null ? "" : label.trim())) {
                return g;
            }
        }
        throw new IllegalArgumentException("Unknown blood group: " + label);
    }
}
