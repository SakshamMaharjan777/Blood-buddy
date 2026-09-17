package com.bloodbuddy.dto;

/**
 * One profile editor's payload (donor / requester / hospital / admin) — the
 * union of the four pages' forms, because CPJ119 §2.2 puts the persistence here
 * and the pages should not each need their own endpoint.
 *
 * <p>Field names are the pages' own form ids ({@code p-fname}, {@code p-lname},
 * …), which is what the demo's mock {@code profile.save} stores verbatim under
 * {@code bb_<role>_profile}.
 *
 * <p><b>Two honest mappings</b> (no silent lies):
 * <ul>
 *   <li><b>name</b>: the ERD has one {@code users.name} column while the forms
 *       have first/last — the service joins them on save and splits on the first
 *       space on read. A true split needs two columns; noted for B3/B6.</li>
 *   <li><b>address</b>: requester/admin profiles have an address field that the
 *       ERD never modelled. B2 adds {@code users.address} (ddl-auto=update adds
 *       the column) so the form value is actually persisted rather than echoed
 *       back and lost. Donor/hospital addresses map to their own
 *       {@code area} column.</li>
 * </ul>
 *
 * <p>{@code photo} is a §2.5 Base64 data URL; it is unwrapped to BLOB bytes on
 * save and re-wrapped on read (never a file path, never in a list payload).
 */
public record ProfileDto(
        /* --- shared --- */
        String fname,
        String lname,
        String email,
        String phone,
        String district,
        String address,
        /* --- donor --- */
        String blood,
        Boolean available,
        /* --- requester --- */
        String hospital,
        Boolean alerts,
        /* --- hospital (blood bank) --- */
        String hname,
        String type,
        String hours,
        Boolean open,
        /* --- §2.5 photo --- */
        String photo
) {
}
