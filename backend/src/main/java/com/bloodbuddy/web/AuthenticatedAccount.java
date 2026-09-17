package com.bloodbuddy.web;

import com.bloodbuddy.model.Role;

import java.io.Serializable;

/**
 * Who is signed in, as stored in the HTTP session — the principal behind
 * {@code SecurityContextHolder}. Deliberately NOT the JPA {@link com.bloodbuddy.model.User}
 * entity: a Hibernate proxy is not something to put in a session (serialization,
 * detach/reattach across requests, and a stale password hash travelling in memory).
 * The resolver loads the entity fresh per request from this id.
 *
 * <p>{@code role} is the enum's {@code name()} ({@code "DONOR"}), matching the
 * {@code ROLE_DONOR} authority Spring Security grants.
 */
public record AuthenticatedAccount(Long userId, String email, Role role) implements Serializable {
}
