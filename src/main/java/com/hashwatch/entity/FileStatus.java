package com.hashwatch.entity;

/**
 * Strongly-typed lifecycle statuses for a monitored file (Sprint 1: S1-T4).
 * Stored as a VARCHAR string in the database via @Enumerated(EnumType.STRING).
 */
public enum FileStatus {
    /** File hash matches the signed baseline and Ed25519 signature is valid. */
    VERIFIED,

    /** File content on disk does not match the active baseline SHA-256 hash. */
    TAMPERED,

    /** File is registered in the database but missing from the filesystem. */
    MISSING,

    /** File is registered but no cryptographic baseline has been generated yet. */
    UNTRACKED,

    /** Stored baseline failed Ed25519 signature verification (database tampering suspected). */
    SIGNATURE_INVALID
}
