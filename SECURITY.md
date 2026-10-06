# Security Policy

## Supported Versions

HashWatch is an active 6-week agile engineering project (Cryptographically Signed File Integrity Monitoring System). Security updates and fixes are applied to the latest `main` branch.

| Version | Branch | Supported          |
| ------- | ------ | ------------------ |
| `0.1.x` | `main` | :white_check_mark: |

## Cryptographic & Repository Security Rules

1. **Never Commit Private Keys:**
   * Local Ed25519 private keys (`keys/ed25519_private.key` or `*.key`) are strictly ignored via `.gitignore` and must **never** be committed to version control.
2. **Database Credentials:**
   * Never hardcode production PostgreSQL passwords in `application-postgres.properties`. Always supply credentials via environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
3. **Zero-Trust Baseline Verification:**
   * Before comparing any watched file's SHA-256 digest against a stored `BaselineEntry`, HashWatch verifies the `Ed25519` digital signature of the baseline record (`filePath:sha256Hash`) to prevent database-level tampering (`BASELINE_COMPROMISED`).

## Reporting a Vulnerability

If you discover a security issue, cryptographic flaw, or accidental credential exposure in this repository:

1. **Do not open a public issue** containing exploit details or sensitive keys.
2. Contact the project lead & maintainer (**Priyanshu Gupta**, `@PG300604`) directly or use GitHub's private vulnerability reporting feature.
3. Include:
   * Affected class/endpoint (e.g., `SigningService`, `HashingService`, `ComparisonService`, or REST API).
   * Steps to reproduce the issue.
   * Proposed remediation if available.
