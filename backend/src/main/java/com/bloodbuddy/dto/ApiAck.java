package com.bloodbuddy.dto;

/**
 * The shape of the endpoints that only confirm something happened (contact form,
 * forgot-password, photo upload). A wire DTO rather than a bare {@code Map} so the
 * response contract is visible in one place — the demo's mock already answers
 * {@code { ok, message }}, and the pages read {@code message}.
 */
public record ApiAck(boolean ok, String message) {

    public static ApiAck of(String message) {
        return new ApiAck(true, message);
    }
}
