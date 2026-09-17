package com.bloodbuddy.dto;

import java.util.List;

/**
 * Outcome of a donor search, including whether the service had to WIDEN it.
 *
 * <p>The widen fallback (session #9, CPJ119 search requirement): if no donor of
 * the requested group is available in the requested district, the search must
 * not dead-end at "0 results" — the same group is offered across all districts
 * and {@code widened} tells the page to say so.
 *
 * <p>Wire format note for B3: the demo's page
 * ({@code requester-search.html}) calls {@code donors.search({})} and iterates
 * the resolved value as an array, so {@code GET /api/donors/search} must answer
 * with the array ({@link #donors()}). {@code widened} / {@code message} are here
 * so the controller CAN return them (e.g. on {@code ?withMeta=true}, or via
 * response headers) once the page is reworked to show the server's message
 * instead of computing the fallback itself.
 */
public record DonorSearchResult(
        List<DonorResponse> donors,
        boolean widened,
        String message
) {

    public static DonorSearchResult direct(List<DonorResponse> donors) {
        return new DonorSearchResult(donors, false, null);
    }

    public static DonorSearchResult widened(List<DonorResponse> donors, String message) {
        return new DonorSearchResult(donors, true, message);
    }
}
