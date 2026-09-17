package com.bloodbuddy.service;

/**
 * The addressed row does not exist (unknown public code, donor id, hospital id).
 * Separate from {@link BusinessException} because B3 answers 404 for one and 409
 * for the other, and merging them would make every missing id look like a rule
 * violation.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
