package com.bloodbuddy.web;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * The JSON body of every failed API call.
 *
 * <p>{@code message} is the field the frontend already reads
 * ({@code bb-api.js}'s real transport does
 * {@code err.message || res.statusText}), so a rule violation reaches the user as
 * the rule's own sentence — "A B+ donor cannot donate to a A- recipient" — rather
 * than a generic 500. {@code fields} carries per-field validation messages
 * (CPJ119 §4 wants server-side validation, so it has to be reportable).
 */
public record ApiError(
        int status,
        String error,
        String message,
        Map<String, String> fields
) {

    public static ApiError of(HttpStatus status, String error, String message) {
        return new ApiError(status.value(), error, message, Map.of());
    }

    public static ApiError of(HttpStatus status, String error, String message, Map<String, String> fields) {
        return new ApiError(status.value(), error, message, fields);
    }
}
