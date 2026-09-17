package com.bloodbuddy.service;

/**
 * A request that violates a documented business rule (BR-1 … BR-9) or a
 * server-side validation rule — "that donor cannot serve that recipient",
 * "this request is already fulfilled", "those units are not in stock".
 *
 * <p>B3 maps it to HTTP 409 (or 400 with a {@code message} field the pages can
 * show). Thrown instead of silently ignoring the call: an application whose
 * critical logic lives in the backend (CPJ119 §2.2) has to SAY no, or the
 * frontend becomes the referee again.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
