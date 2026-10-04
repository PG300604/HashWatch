package com.hashwatch.entity;

/**
 * Strongly-typed alert severity levels for security incident triage (Sprint 1: S1-T4).
 * Stored as VARCHAR in the database via @Enumerated(EnumType.STRING).
 */
public enum AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
