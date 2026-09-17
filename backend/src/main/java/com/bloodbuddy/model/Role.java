package com.bloodbuddy.model;

/**
 * The four RBAC roles (proposal §1.3, report Table 3-1). Values match the
 * frontend's role strings so the DTO layer (B3+) can map without a translation
 * table; Spring Security (B6) grants authority per role.
 */
public enum Role {
    DONOR,
    REQUESTER,
    HOSPITAL,
    ADMIN
}
