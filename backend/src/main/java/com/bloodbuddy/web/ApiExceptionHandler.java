package com.bloodbuddy.web;

import com.bloodbuddy.service.BusinessException;
import com.bloodbuddy.service.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns the service layer's exceptions into HTTP answers — the piece that lets
 * the controllers stay thin (CPJ119 §2.2: no business logic, and no {@code try}
 * blocks either).
 *
 * <table>
 *   <tr><td>404</td><td>{@link NotFoundException} — the addressed row does not exist</td></tr>
 *   <tr><td>409</td><td>{@link BusinessException} — a documented rule says no (BR-1 … BR-8)</td></tr>
 *   <tr><td>400</td><td>bean-validation failures, unparseable bodies/labels, malformed json</td></tr>
 *   <tr><td>409</td><td>unique-constraint races (two registrations for one email at once)</td></tr>
 * </table>
 *
 * <p>Deliberately NO catch-all {@code Exception} handler: swallowing everything
 * would also swallow Spring's {@code NoResourceFoundException} and turn a missing
 * static page (the frontend is served from this same app) into a 500.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(HttpStatus.NOT_FOUND, "not-found", e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> ruleViolation(BusinessException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(HttpStatus.CONFLICT, "rule-violation", e.getMessage()));
    }

    /** {@code @Valid} failures — one entry per rejected field, so a form can mark them. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err ->
                fields.putIfAbsent(err.getField(), err.getDefaultMessage() == null
                        ? "invalid value" : err.getDefaultMessage()));
        String first = fields.isEmpty() ? "The request body is invalid."
                : "Check " + String.join(", ", fields.keySet()) + ".";
        return ResponseEntity.badRequest()
                .body(ApiError.of(HttpStatus.BAD_REQUEST, "validation-failed", first, fields));
    }

    /**
     * Bad enum/label parses ({@code BloodGroup.fromLabel}, {@code RequestStatus.fromLabel})
     * and other argument problems. The message is the parser's own, which names the
     * offending value — more useful than "Bad Request".
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(HttpStatus.BAD_REQUEST, "bad-request", e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        log.debug("Unreadable request body", e);
        return ResponseEntity.badRequest()
                .body(ApiError.of(HttpStatus.BAD_REQUEST, "malformed-body",
                        "The request body is not valid JSON for this endpoint."));
    }

    /** Unique-constraint collisions that lost a race (e.g. same email registered twice). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflict(DataIntegrityViolationException e) {
        log.warn("Data integrity conflict: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(HttpStatus.CONFLICT, "conflict",
                        "That change conflicts with data already in the system."));
    }
}
