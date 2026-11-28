# Final Academic Project Report: HashWatch

## A Cryptographically Signed File Integrity Monitoring System with Edwards-Curve Digital Signatures and Asymmetric Baselines

---

**Academic Year:** 2026–2027  
**Department:** Department of Computer Science & Business Systems (CSBS)  
**Institution:** Asansol Engineering College, Asansol, West Bengal  
*(Affiliated to Maulana Abul Kalam Azad University of Technology — MAKAUT)*  

**Project Group:** Group 3  
**Project Members:**
1. **Priyanshu Ghosh** *(Project Lead, [`@PG300604`](https://github.com/PG300604))* — Backend Architecture, DBMS, DevOps, Analysis, Cryptology
2. **Riya** *([`@riyaaa0710`](https://github.com/riyaaa0710))* — Cryptology, REST API, Frontend UI, DBMS
3. **Samarjeet Kumar** *([`@samarjeet-kr`](https://github.com/samarjeet-kr))* — Backend Scheduling, Cryptology, DBMS Queries, Analysis

**Repository:** [https://github.com/PG300604/HashWatch.git](https://github.com/PG300604/HashWatch.git)  
**Document Status:** Final Defense Version (Production Release 1.0.0)  

---

## Executive Summary / Abstract

In modern distributed server architectures and enterprise cloud infrastructures, unauthorized alteration of critical configuration files, system binaries, security descriptors, and cryptographic credentials constitutes a primary vector for privilege escalation, stealth persistence, and Advanced Persistent Threats (APTs). Conventional File Integrity Monitoring (FIM) systems—including industry standards such as Tripwire, OSSEC, AIDE, and Samhain—compute cryptographic digests (e.g., SHA-256 or MD5) and store them in relational database tables, local flat files, or centralized index stores. However, these systems exhibit a fundamental architectural vulnerability: **baseline mutable vulnerability**. An adversary who obtains local administrative privileges, gains physical storage access, or compromises the database backend can manipulate both the file on disk and the corresponding database baseline record simultaneously, entirely bypassing integrity audits.

To eliminate this critical single point of failure, we present **HashWatch**: an end-to-end, enterprise-grade, cryptographically signed File Integrity Monitoring system. HashWatch combines 64 KB streaming SHA-256 hashing with asymmetric Edwards-curve Digital Signatures (**Ed25519** over Curve25519). Every baseline snapshot is bound into a canonical **Triple-Lock Envelope** ($$\text{HashWatch:v1:path:sha256:size}$$) and signed with an out-of-database Ed25519 private key. During continuous automated verification loops executed by a non-blocking Quartz scheduler, HashWatch enforces zero-trust verification: the baseline digital signature and public key fingerprint must verify successfully prior to comparing disk bytes. Experimental benchmarking confirms that HashWatch achieves a **100% detection rate** on single-bit modifications, sustains hashing throughput of **1,872–2,009 MB/s**, executes **30,525 Ed25519 signs/sec and 13,859 verifications/sec**, maintains a constant $$\mathcal{O}(1)$$ memory footprint under a 256 MB JVM heap even when scanning multi-gigabyte files, and prevents alert spamming via state-transition alert gating.

---

## 1. Introduction & Problem Formulation

### 1.1 The Threat Model
Modern enterprise security frameworks (e.g., PCI-DSS Section 11.5, SOC2 Type II, ISO/IEC 27001, and NIST SP 800-53) mandate rigorous monitoring of system and application file integrity. Configuration files (`/etc/pam.d/`, `/etc/shadow`, `/etc/sudoers`, Windows System32 drivers) define authentication mechanisms, access control lists, and security boundaries. Attackers exploiting zero-day vulnerabilities or stolen administrative credentials frequently modify these files to install backdoors, inject unauthorized public keys, or disable audit daemons.

### 1.2 Limitations of Conventional FIM Architectures
1. **Symmetric / Plaintext Baseline Storage:** Traditional tools store expected file hashes in plaintext SQLite/MySQL tables or flat files. Once root/admin privileges are achieved, the attacker runs `sha256sum altered_file > baseline.db`, rendering the FIM oblivious to the intrusion.
2. **Alert Fatigue in Automated Polling Loops:** Without intelligent state-transition deduplication, periodic monitors emit duplicate alert storms every polling interval for an unresolved breach, drowning security operation centers (SOC) in noise.
3. **Memory Exhaustion on Large Files:** Inefficient FIM utilities read entire files into memory buffers prior to hashing, causing `OutOfMemoryError` failures when encountering multi-gigabyte databases or container images.
4. **Heavyweight Build Infrastructure:** Many modern solutions require complex microservice orchestrations, heavy agent runtimes, and external Node.js frontend build systems that hinder lightweight, rapid edge deployment.

### 1.3 Project Objectives
- Architect an asymmetric, tamper-evident baseline snapshot mechanism using standard **Ed25519** (RFC 8032).
- Guarantee constant $$\mathcal{O}(1)$$ memory consumption through chunked 64 KB streaming SHA-256 pipelines.
- Implement an automated background verification engine using **Quartz Scheduler** with strict concurrency controls and per-file fault isolation.
- Design a deterministic 5-state integrity machine (`UNTRACKED`, `VERIFIED`, `TAMPERED`, `MISSING`, `SIGNATURE_INVALID`) with state-transition alert deduplication.
- Deliver a modern, zero-dependency operator dashboard and incident triage center powered by Spring Boot and Thymeleaf.
- Provide a standardized REST API for SOC automation and programmatic SIEM integration.

---

## 2. Literature Review & Comparative Analysis

We conducted an exhaustive survey of established commercial and open-source FIM implementations.

### 2.1 Comparative Analysis Matrix

| Feature / Metric | Tripwire (Open Source) | OSSEC | Samhain | AIDE | **HashWatch (Proposed)** |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Primary Hash Algorithm** | SHA-256 / MD5 | MD5 / SHA-1 | SHA-256 / TIGER | SHA-256 / SHA-512 | **Streaming SHA-256 (64 KB buffers)** |
| **Baseline Cryptographic Protection** | Local encrypted database | Centralized plaintext server | Signed database (Tiger192) | Plaintext database file | **Asymmetric Ed25519 Digital Signatures** |
| **Tamper Evidence Against Root DB Edit** | Partial (Passphrase required) | ❌ None (Database mutable) | Partial (GPG detached) | ❌ None (Flat text file) | **✅ 100% Tamper Evident (Ed25519 envelope)** |
| **Replay & Cut-and-Paste Defense** | ❌ Vulnerable to path swaps | ❌ Vulnerable | ❌ Vulnerable | ❌ Vulnerable | **✅ Triple-Lock Canonical Envelope** |
| **Scheduling Engine** | OS Cron dependency | Continuous daemon | Native daemon loop | OS Cron dependency | **Quartz Enterprise Scheduler (30s interval)** |
| **Alert Deduplication** | ❌ Alert every cron run | Basic windowing | ❌ Repeated logging | ❌ Alert every cron run | **✅ State-Transition Alert Gating** |
| **Operator Interface** | CLI Only | CLI / Web UI Plugin | CLI / Beltane Web UI | CLI Only | **✅ Native Real-time Thymeleaf Dashboard** |
| **REST API Automation** | ❌ None | ❌ Proprietary protocol | ❌ None | ❌ None | **✅ Standard OpenAPI REST Endpoints** |
| **Memory Consumption on 10 GB File** | Variable ($\mathcal{O}(N)$ or chunked) | Bounded | Bounded | Bounded | **✅ Constant $\mathcal{O}(1)$ (< 64 KB stream buffer)** |

---

## 3. Mathematical & Cryptographic Foundations

HashWatch enforces integrity through a dual-layer cryptographic design: collision-resistant hashing and Edwards-curve digital signatures.

### 3.1 Streaming SHA-256 Hash Generation
Conforming to FIPS PUB 180-4, SHA-256 operates on 512-bit message blocks through 64 rounds of bitwise logical operations. To guarantee bounded memory utilization regardless of file size $$S$$, HashWatch processes input files via chunked streaming:

$$\text{Digest} = \mathcal{H}(F) = \text{SHA-256}\left(\bigcup_{i=1}^{\lceil S / B \rceil} b_i\right), \quad B = 65,536 \text{ bytes (64 KB)}$$

The collision resistance property guarantees that it is computationally infeasible to find two distinct files $$F_1 \neq F_2$$ such that $$\mathcal{H}(F_1) = \mathcal{H}(F_2)$$, with an operational work factor of $$2^{128}$$.

### 3.2 Ed25519 Asymmetric Digital Signatures
HashWatch adopts the Edwards-curve Digital Signature Algorithm (**Ed25519**, RFC 8032), operating over the twisted Edwards curve:

$$-x^2 + y^2 = 1 - \frac{121665}{121666} x^2 y^2 \quad \text{over the prime field } \mathbb{F}_{2^{255}-19}$$

Key properties of Ed25519:
- **Security Level:** ~128-bit security level, equivalent to 3072-bit RSA, but with significantly smaller keys (32-byte public key, 64-byte private key) and compact 64-byte signatures.
- **Side-Channel Immunity:** Constant-time arithmetic without secret branch conditions or data-dependent table lookups, preventing timing and cache-based side-channel attacks.
- **Collision Resilience:** Uses SHA-512 for hash-based nonce generation ($$r = \mathcal{H}_{\text{SHA-512}}(h_b \parallel M)$$), completely eliminating the catastrophic catastrophic key-leakage vulnerabilities of ECDSA when encountering poor pseudo-random number generators (PRNG).

### 3.3 The Triple-Lock Canonical Envelope
Signing raw hashes alone leaves systems susceptible to **database cut-and-paste attacks** (where an attacker swaps the baseline row of `/bin/ls` with the baseline row of `/bin/su`). HashWatch prevents cross-file and replay attacks by binding all critical file metadata into a canonical envelope prior to signing:

$$\mathcal{P}_{\text{canonical}} = \texttt{"HashWatch:v1:"} \parallel \text{NormPath}(P) \parallel \texttt{":"} \parallel \mathcal{H}(F) \parallel \texttt{":"} \parallel \text{Size}(F)$$

$$\Sigma = \text{Sign}_{\text{Ed25519}}\left(SK, \mathcal{P}_{\text{canonical}}\right)$$

```
+---------------------------------------------------------------------------------------------+
|                                TRIPLE-LOCK CANONICAL ENVELOPE                               |
|                                                                                             |
|  [ Domain Prefix: HashWatch:v1 ] : [ Path: /etc/shadow ] : [ SHA-256: 3a7b... ] : [ Size ] |
|                                              |                                              |
|                                     Ed25519 Digital Sign                                    |
|                                              v                                              |
|                   [ Base64 Digital Signature (512-bit cryptographic proof) ]                |
+---------------------------------------------------------------------------------------------+
```

---

## 4. System Architecture & Technical Design

HashWatch is engineered as a modular, high-cohesion Spring Boot application designed to run independently in enterprise server environments.

```mermaid
flowchart TD
    subgraph StorageLayer["Physical Filesystem & Database"]
        FS[("Watched Files on Disk\n(/etc/, Binaries, Configs)")]
        DB[("PostgreSQL / H2\n(WatchedFile, Baseline, Alerts)")]
        KEY[("Secure Key Directory\n(ed25519_private.key, .pub)")]
    end

    subgraph CoreEngine["HashWatch Cryptographic Core"]
        HASH["HashingService\n(64 KB Buffered SHA-256)"]
        SIGN["SigningService\n(SunEC Ed25519 Engine)"]
        COMP["ComparisonService\n(Triple-Lock & State Machine)"]
        SCHED["Quartz Scheduler Daemon\n(30s Periodic Verification Loop)"]
    end

    subgraph InterfaceLayer["Presentation & API Layer"]
        REST["REST API Controllers\n(/api/files, /api/baselines, /api/alerts)"]
        WEB["Thymeleaf Dashboard UI\n(Live Operator Cockpit /)"]
        ALERT_UI["Incident Triage Center\n(/alerts Feed & Hash Diff)"]
    end

    FS -->|Chunked I/O Stream| HASH
    KEY -->|Load / Sign| SIGN
    HASH --> COMP
    SIGN --> COMP
    COMP -->|Persist & Update| DB
    SCHED -->|Trigger Scheduled Scan| COMP
    REST --> COMP
    DB --> REST
    DB --> WEB
    DB --> ALERT_UI
```

### 4.1 The 5-State Integrity Verification State Machine
The core engine models every file across five deterministic states:
1. **`UNTRACKED`:** The file has been registered in the database, but no initial baseline has yet been calculated.
2. **`VERIFIED`:** Baseline digital signature verifies against the trusted public key, and current disk bytes hash matches the baseline hash.
3. **`TAMPERED`:** Baseline digital signature is valid, but the disk file hash does not match the baseline hash (indicates file tampering). Emits an alert with severity `HIGH` and event type `UNAUTHORIZED_MODIFICATION`.
4. **`MISSING`:** The file record exists in the database with an active baseline, but the target file has been removed from disk. Emits a `CRITICAL` alert with event type `MISSING_FILE`.
5. **`SIGNATURE_INVALID`:** The digital signature of the baseline record fails verification against the public key fingerprint (indicates database tampering or key substitution). Emits a `CRITICAL` alert with event type `SIGNATURE_INVALID` and aborts file comparison.

---

## 5. Implementation Highlights

### 5.1 Zero-Trust Verification Pipeline (`ComparisonService.java`)
The verification algorithm enforces strict zero-trust precedence:

```java
// Step 1: Zero-Trust Signature Verification FIRST
String canonicalPayload = buildCanonicalPayload(file.getFilePath(), baseline.getSha256Hash(), file.getFileSize());
boolean signatureValid = signingService.verify(canonicalPayload, baseline.getSignature(), baseline.getPublicKeyId());

if (!signatureValid) {
    file.setStatus(FileStatus.SIGNATURE_INVALID);
    createAlertIfNotPresent(file, EventType.SIGNATURE_INVALID, AlertSeverity.CRITICAL, ...);
    return; // NEVER evaluate disk bytes against a corrupted/untrusted baseline!
}

// Step 2: Stream current file hash from disk
String currentHash = hashingService.hashFile(path.toFile());

// Step 3: Compare digests
if (currentHash.equalsIgnoreCase(baseline.getSha256Hash())) {
    file.setStatus(FileStatus.VERIFIED);
} else {
    file.setStatus(FileStatus.TAMPERED);
    createAlertIfNotPresent(file, EventType.UNAUTHORIZED_MODIFICATION, AlertSeverity.HIGH, ...);
}
```

### 5.2 Enterprise Scheduling & Fault Isolation (`MonitoringJob.java`)
- **Annotation `@DisallowConcurrentExecution`:** Guarantees that slow disk I/O on large volumes does not spawn concurrent execution threads.
- **Batch Verification Fault Isolation:** Wrapped scan loops catch transient I/O exceptions on a per-file basis so an unreadable file does not disrupt verification of remaining files.
- **Alert Deduplication:** `createAlertIfNotPresent` checks whether an unresolved alert with the same event type already exists for the file, preventing alert fatigue.

---

## 6. Experimental Evaluation & Benchmark Results

Performance and reliability benchmarks were executed empirically using `python-analysis/overhead_benchmark.py` and `python-analysis/latency_analysis.py` on Windows 11 (x64, Java 21 LTS / Python 3.12) and verified in GitHub Actions CI (`ubuntu-latest`).

### 6.1 SHA-256 Streaming Throughput vs. File Size (`overhead_benchmark.py`)

| Test Payload Size | Mean Processing Time (ms) | Memory Consumption (Stream Buffer) | Measured Sustained Throughput |
| :--- | :--- | :--- | :--- |
| **1 MB** | 0.53 ms | < 64 KB | 1,871.68 MB/s |
| **5 MB** | 2.62 ms | < 64 KB | 1,909.02 MB/s |
| **10 MB** | 5.17 ms | < 64 KB | 1,932.90 MB/s |
| **25 MB** | 12.45 ms | < 64 KB | 2,008.82 MB/s |
| **50 MB** | 26.15 ms | < 64 KB | 1,912.02 MB/s |
| **100 MB** | 51.99 ms | < 64 KB | 1,923.36 MB/s |

*Key Finding:* Sustained SHA-256 throughput ranges from **1,871.68 MB/s to 2,008.82 MB/s** (~2.4x faster than the ~800 MB/s *snaproot* arXiv:2606.10625 reference baseline), proving that cryptographic hashing is not a system bottleneck.

### 6.2 Ed25519 Cryptographic Operations Performance (`overhead_benchmark.py`)

| Cryptographic Operation | Sample Iterations | Measured Operations / Second |
| :--- | :--- | :--- |
| **Ed25519 Signing** | 2,000 signatures | **30,525 ops/sec** |
| **Ed25519 Verification** | 2,000 verifications | **13,859 ops/sec** |

*Key Finding:* Verifying 1,000 signed file baselines takes ~72 ms total, confirming negligible cryptographic overhead per polling cycle.

### 6.3 End-to-End Detection Latency Distribution (`latency_analysis.py`, $N = 10,000$)

Under automated 30-second Quartz polling combined with empirical 5.00 ms cryptographic verification overhead:

| Latency Metric | Measured Latency | Theoretical / Reference Bound |
| :--- | :--- | :--- |
| **Minimum Latency** | 0.0078 seconds | $> 0$ seconds |
| **Mean Detection Latency** | **14.8300 seconds** | $\approx T_{\text{poll}} / 2 + T_{\text{verify}} \approx 15.005$ s |
| **Median (P50) Latency** | **14.7592 seconds** | $\approx 15.000$ seconds |
| **Interquartile Range (IQR)** | 14.8200 seconds | $P_{25} = 7.4412\text{ s},\ P_{75} = 22.2612\text{ s}$ |
| **90th Percentile (P90)** | 26.7044 seconds | $< 27.0$ seconds |
| **95th Percentile (P95)** | **28.4186 seconds** | $\approx 28.505$ seconds |
| **99th Percentile (P99)** | **29.6689 seconds** | $< 30.0$ seconds (`Max = 29.9999 s`) |

---

## 7. Quality Assurance & Automated Test Coverage

The HashWatch engineering lifecycle implemented rigorous test-driven development:

| Test Suite / Target Class | Test Scope | Tests Run | Pass Rate |
| :--- | :--- | :--- | :--- |
| `SigningServiceTest` | Ed25519 key generation, key rotation, PEM/DER export, detached validation | 13 | **100% (13/13)** |
| `HashingServiceTest` | SHA-256 streaming correctness, 64 KB buffer boundary conditions | 5 | **100% (5/5)** |
| `ComparisonServiceTest` | Zero-trust verification, Triple-Lock envelope, directory/untracked guards, state transitions | 13 | **100% (13/13)** |
| `MonitoringJobTest` | Quartz trigger execution loop, timeout resilience, error boundaries | 3 | **100% (3/3)** |
| `RepositoryIntegrationTest` | JPA indexes, enum mapping, cascade deletes, unique path constraints | 8 | **100% (8/8)** |
| `AlertEventRepositoryTest` | Filter queries (severity, event type, date range, pagination) | 18 | **100% (18/18)** |
| `FileControllerTest` | File registration REST endpoints, batch fault isolation, soft-deletes, 100 MB cap | 12 | **100% (12/12)** |
| `BaselineControllerTest` | Baseline generation, bulk resigning, public key & PEM exports | 5 | **100% (5/5)** |
| `AlertControllerTest` | Incident triage REST endpoints, manual scan triggers, alert resolution | 5 | **100% (5/5)** |
| `DashboardViewControllerTest` | Model attribute binding, count queries, dual routing, alert cap triage | 5 | **100% (5/5)** |
| `TamperDetectionIntegrationTest` | Byte tampering, single-bit flip, DB corruption, key rotation continuity, Triple-Lock size invariant, mixed batch recovery | 10 | **100% (10/10)** |
| `HashWatchApplicationTests` | Spring Boot application context initialization | 1 | **100% (1/1)** |
| **Total Automated Tests** | **Complete System Verification Suite** | **98** | **100% (98/98)** |

---

## 8. Conclusion & Future Work

### 8.1 Concluding Summary
HashWatch successfully resolves the historic vulnerability of mutable FIM baselines by introducing asymmetric Ed25519 digital signatures and Triple-Lock canonical envelopes. By pairing collision-resistant SHA-256 chunked streaming with a resilient Quartz scheduling daemon and an intuitive operator cockpit, HashWatch delivers an enterprise-ready security auditing platform suitable for compliance frameworks such as PCI-DSS and SOC2.

### 8.2 Future Research & Engineering Extensions
1. **eBPF Kernel-Space Event Hooking:** Integrating Linux extended Berkeley Packet Filters (`eBPF`) to intercept `sys_enter_write` and `sys_enter_unlink` in real time, reducing detection latency to sub-millisecond intervals while retaining Quartz as a fallback auditor.
2. **Hardware Security Module (HSM) Integration:** Supporting PKCS#11 interfaces to store Ed25519 private keys within FIPS 140-2 Level 3 tamper-resistant hardware modules (e.g. YubiKey or AWS CloudHSM).
3. **Decentralized Ledger Forensics:** Anchoring periodic cryptographic baseline roots into a permissioned blockchain (e.g., Hyperledger Fabric) to provide multi-organization non-repudiation for regulatory compliance.

---

## 9. References

1. Bernstein, D. J., Duif, N., Lange, T., Schwabe, P., & Yang, B. Y. (2012). *High-speed high-security signatures*. Journal of Cryptographic Engineering, 2(2), 77-89. (RFC 8032: Edwards-Curve Digital Signature Algorithm).
2. National Institute of Standards and Technology. (2015). *Secure Hash Standard (SHS)*. FIPS PUB 180-4, U.S. Department of Commerce.
3. Kim, G. H., & Spafford, E. H. (1994). *The design and implementation of Tripwire: A file integrity checker*. Proceedings of the 2nd ACM Conference on Computer and Communications Security, 18-29.
4. PCI Security Standards Council. (2022). *Payment Card Industry Data Security Standard (PCI DSS) Requirements and Testing Procedures Version 4.0*. Section 11.5: Change-Detection Mechanisms.
5. National Institute of Standards and Technology. (2020). *Security and Privacy Controls for Information Systems and Organizations*. NIST Special Publication 800-53, Revision 5.

---

*Academic Project Report submitted in partial fulfillment of the requirements for the Degree of Bachelor of Technology in Computer Science & Business Systems (CSBS).*
