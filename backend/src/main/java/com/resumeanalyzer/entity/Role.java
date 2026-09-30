package com.resumeanalyzer.entity;

/**
 * The fixed set of user roles supported by the system.
 *
 * Per the finalized project decisions, there are exactly two roles.
 * There is no ADMIN role, and no additional role should be added without
 * revisiting the project's architecture decisions.
 */
public enum Role {
    JOB_SEEKER,
    RECRUITER
}
