package com.bloodbuddy.dto;

import com.bloodbuddy.model.BloodGroup;
import com.bloodbuddy.model.Donor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * One donor card (proposal Appendix B: {@code GET /api/donors/search}).
 *
 * <p>Component names deliberately match the keys {@code requester-search.html}
 * already renders ({@code blood, area, avail, compat, last}) — the demo store's
 * row shape is the de-facto wire contract, so keeping it means the real-mode
 * flip needs no page edits.
 *
 * <p>{@code compat} is NOT a property of the donor: it is computed for the
 * recipient group the search was run for (BR-1 — {@code BloodGroup} carries the
 * chart in the recipient → donor direction). A null recipient group yields
 * {@code false} rather than a guess.
 *
 * <p>{@code last} is the human label the card prints ("4 months ago"), derived
 * from the real {@code lastDonationDate}; the raw date is not exposed because no
 * page uses it.
 */
public record DonorResponse(
        String id,
        String name,
        String blood,
        String area,
        String district,
        boolean avail,
        boolean compat,
        String last,
        boolean hasPhoto
) {

    /** Map a donor for a search run for {@code recipient} (null = no compat information). */
    public static DonorResponse of(Donor d, BloodGroup recipient) {
        return new DonorResponse(
                String.valueOf(d.getDonorId()),
                d.getUser() == null ? "BloodBuddy donor" : d.getUser().getName(),
                d.getBloodGroup() == null ? "—" : d.getBloodGroup().getLabel(),
                d.getArea(),
                d.getUser() == null ? null : d.getUser().getDistrict(),
                d.isAvailable(),
                recipient != null && recipient.acceptsDonor(d.getBloodGroup()),
                lastDonated(d.getLastDonationDate()),
                d.getPhoto() != null && d.getPhoto().length > 0);
    }

    /**
     * "4 months ago" — the donor card's Last donated value. Kept coarse on
     * purpose: the demo prints months/years, and a day-level date invites
     * "is this donor eligible?" questions the service does not answer yet.
     */
    public static String lastDonated(LocalDate date) {
        if (date == null) {
            return "—";
        }
        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days < 0) {
            return "scheduled";
        }
        if (days < 31) {
            return days <= 1 ? "Today" : days + " days ago";
        }
        long months = ChronoUnit.MONTHS.between(date, LocalDate.now());
        if (months < 12) {
            return months + (months == 1 ? " month ago" : " months ago");
        }
        long years = ChronoUnit.YEARS.between(date, LocalDate.now());
        return years + (years == 1 ? " year ago" : " years ago");
    }
}
