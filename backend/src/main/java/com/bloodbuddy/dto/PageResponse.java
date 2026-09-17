package com.bloodbuddy.dto;

import java.util.List;

/**
 * A page of a listing (CPJ119 §3 pagination; proposal §1.4 "pagination for large
 * result sets, including donor search results and administrative user and request
 * listings").
 *
 * <p><b>Additive by design.</b> The endpoints that support paging return the plain
 * array they always did when the caller sends no {@code page} — every existing
 * page in the frontend, the QA harness and the Postman collection keeps working
 * unchanged. A caller that DOES send {@code ?page=&size=} gets this envelope
 * instead, so the API itself paginates rather than handing the whole result set
 * over for the browser to slice.
 *
 * <p>{@code total}/{@code totalPages}/{@code hasNext} travel with the window so a
 * client never has to count what it was not sent. {@code page} is zero-based, to
 * match Spring Data's {@code Pageable}.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long total,
        int totalPages,
        boolean hasNext
) {

    /**
     * Window {@code all} (already filtered and sorted by the caller) down to one
     * page. A page past the end answers an empty {@code items} list rather than an
     * error — "you asked for page 9 of a 3-page list" is not a client mistake worth
     * a 4xx.
     */
    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int size1 = Math.max(1, size);
        int page1 = Math.max(0, page);
        long total = all.size();
        int totalPages = (int) Math.ceil(total / (double) size1);
        int from = (int) Math.min((long) page1 * size1, total);
        int to = (int) Math.min((long) from + size1, total);
        return new PageResponse<>(List.copyOf(all.subList(from, to)), page1, size1, total, totalPages, to < total);
    }
}
