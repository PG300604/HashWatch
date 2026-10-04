package com.hashwatch.entity;

/**
 * Strongly-typed integrity violation event classifications (Sprint 1: S1-T4).
 * Stored as VARCHAR in the database via @Enumerated(EnumType.STRING).
 */
public enum EventType {
    /** General hash mismatch detected during verification pass. */
    MISMATCH,

    /** File content altered on disk without re-baselining. */
    UNAUTHORIZED_MODIFICATION,

    /** Monitored file was deleted or moved from its expected disk path. */
    MISSING_FILE,

    /** Baseline entry failed Ed25519 signature verification. */
    SIGNATURE_INVALID
}
