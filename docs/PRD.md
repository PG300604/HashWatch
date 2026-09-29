# Product Requirements Document (PRD) — HashWatch

**Project Title:** HashWatch: Cryptographically Signed File Integrity Monitoring System  
**Academic Group:** Group 3, CSBS, Asansol Engineering College  
**Repository:** [https://github.com/PG300604/HashWatch.git](https://github.com/PG300604/HashWatch.git)  
**Status:** Approved / Active Development  

---

## 1. Executive Summary & Objective

In modern enterprise and cloud computing infrastructures, unauthorized modification of configuration files, binaries, and system libraries is a primary vector for privilege escalation, backdoors, and advanced persistent threats (APTs). Conventional File Integrity Monitoring (FIM) systems often compute hashes (e.g., MD5 or SHA-1) and store them in plain database tables or flat files, leaving the baseline itself vulnerable to offline tampering by attackers with local administrative access.

**HashWatch** solves this problem by combining streaming SHA-256 cryptographic hashing with asymmetric digital signatures using Edwards-curve Digital Signature Algorithm (**Ed25519**). Every file baseline is digitally signed with a private key kept secure by the system administrator. During continuous automated verification passes, HashWatch guarantees both **authenticity** (that the baseline was not forged) and **integrity** (that the monitored files on disk match the verified baseline).

---

## 2. Target Personas

| Persona | Role | Primary Need |
| :--- | :--- | :--- |
| **System Administrator (SecOps)** | Server Security Lead | Needs real-time automated detection when critical configuration files (`/etc/`, `C:\Windows\System32\drivers\etc\`) are modified without authorization. |
| **Compliance Auditor** | Regulatory Officer | Requires cryptographically verifiable proof and immutable timestamps indicating files were unaltered between inspection cycles (PCI-DSS 11.5, SOC2). |
| **DevOps Engineer** | CI/CD Platform Lead | Needs lightweight containerized monitoring with minimal CPU/RAM overhead that can be deployed via Docker without degrading application throughput. |

---

## 3. Scope & Key Differentiators

### In-Scope
- Monitoring arbitrary local file paths across Linux and Windows operating systems.
- Streaming SHA-256 hash generation (64 KB buffered reads) to minimize memory footprints for multi-gigabyte files.
- Asymmetric Ed25519 cryptographic baseline signing and public-key verification.
- Automated periodic polling using a robust Quartz scheduler.
- PostgreSQL database persistence for files, baselines, and security alerts.
- Modern Web Dashboard (Thymeleaf, HTML5, Vanilla CSS/JS) for real-time visualization, manual scans, and incident triage.
- REST API layer allowing programmatic automation and health telemetry.
- Python-based statistical and benchmarking analysis suite for academic report generation.

### Out-of-Scope (Future Iterations)
- Real-time kernel driver-level filesystem hooking (e.g., eBPF or Windows Filter Drivers) — periodic polling is intentionally selected for portable user-space stability.
- Automatic rollbacks / file restoration (HashWatch is an auditor/detector, not a backup tool).

---

## 4. Functional Requirements (FR)

- **FR1: File Registration & Lifecycle Management**
  - Users can register any valid filesystem path to be monitored.
  - The system records initial file attributes: file path, size in bytes, and last modified timestamp.
  - Users can activate, deactivate, or delete watched files.

- **FR2: Cryptographic Baseline Establishment**
  - For each watched file, the system streams the content through SHA-256 to generate a 256-bit digest.
  - The SHA-256 digest is signed using an Ed25519 private key.
  - The hash, signature, signing key ID, and timestamp are stored in the database as the active baseline.
  - Re-baselining an existing file marks older baselines as superseded while preserving historical records.

- **FR3: Automated Integrity Verification (Polling Loop)**
  - Quartz Scheduler triggers automated scans on a configurable interval (default: 30 seconds).
  - For each active file, the system first verifies the Ed25519 signature of the stored baseline.
  - If the signature is valid, the current file on disk is hashed and compared against the baseline hash.

- **FR4: Anomaly Detection & State Machine**
  - **VERIFIED:** Disk hash matches baseline hash, and baseline signature is valid.
  - **TAMPERED / MISMATCH:** Disk hash differs from baseline hash.
  - **MISSING:** File exists in the database but is absent from the filesystem.
  - **SIGNATURE_INVALID:** Baseline signature fails Ed25519 verification (alerting database tampering).

- **FR5: Alert Event Logging & Incident Triage**
  - Any integrity anomaly automatically triggers an `AlertEvent` entry with severity level (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), expected hash, actual hash, and timestamp.
  - Operators can view unresolved alerts and mark them as resolved.

- **FR6: Management Dashboard & Web Interface**
  - Clean web dashboard rendering overall health status, active file count, alert count, and baseline totals.
  - Provides quick action buttons: "Run Integrity Scan Now", "Add File", "Rebaseline All", "Resolve Alert".

- **FR7: REST API**
  - Exposes standardized JSON endpoints for files (`/api/files`), baselines (`/api/baselines`), and alerts (`/api/alerts`).
  - Allows integration with external security operations center (SOC) tooling.

- **FR8: Academic Performance Benchmarking & Statistical Evaluation**
  - Standalone Python scripts measuring SHA-256 throughput, Ed25519 signing/verifying ops/sec, and end-to-end detection latency distributions.
  - Direct comparison with published academic baseline literature.

---

## 5. Non-Functional Requirements (NFR)

- **NFR1: Performance & Overhead:**
  - Memory consumption must remain bounded under 256 MB JVM heap even when hashing 10+ GB files (achieved via 64 KB streaming buffers).
  - CPU utilization during idle polling cycles must remain below 3% on standard quad-core systems.
- **NFR2: Cryptographic Rigor:**
  - Edwards-curve Digital Signature Algorithm (Ed25519 / RFC 8032) provides 128-bit security level, resistant to side-channel attacks and collision attacks.
  - SHA-256 provides collision resistance conforming to FIPS 180-4.
- **NFR3: Portability:**
  - Supports Java 17+ on Windows, Linux, and macOS platforms.
  - Can run standalone using in-memory H2 or with enterprise PostgreSQL via Docker.
- **NFR4: Reliability:**
  - Database transactions ensure that file status updates and alert event insertions occur atomically.

---

## 6. Success Metrics & Acceptance Criteria

1. **Detection Accuracy:** 100% detection rate when 1 single bit in a watched file is modified.
2. **Tamper-Evident Baselines:** Any unauthorized direct modification of the database baseline table is immediately flagged with `SIGNATURE_INVALID`.
3. **Detection Latency:** Time from file modification to alert generation is bounded by `Polling_Interval + Processing_Time` (typically < 30.5 seconds).
4. **Clean Code & Test Coverage:** Core cryptographic and comparison services must achieve > 90% unit test coverage.
