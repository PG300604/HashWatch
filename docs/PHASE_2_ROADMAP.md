# HashWatch Phase 2 Roadmap (8th Semester: Dec 2026 – May 2027)

**Project Title:** HashWatch — Cryptographically Signed File Integrity Monitoring System  
**Team:** Group 3, Department of Computer Science & Business Systems (CSBS), Asansol Engineering College  
**Phase 1 Baseline (Sem 7, Sprints 1–6):** 100% Complete (98/98 tests passing, Ed25519 Triple-Lock Envelopes, 64 KB SHA-256 streaming, Quartz 30s daemon, Obsidian SOC UI, Docker/PostgreSQL stack, Python empirical benchmarks).

---

## 1. Honest Codebase Audit: Current Limitations & Phase 2 Targets

| Phase 1 Limitation | Root Cause in Current Codebase | Phase 2 Engineering Solution (Sprints 7–12) |
| :--- | :--- | :--- |
| **30s TOCTOU Polling Blind Spot** | `MonitoringJob` polls every 30s (`SchedulerConfig.java`), and `runVerificationScan()` iterates sequentially. | **Hybrid WatchService + Periodic Fallback + Java 21 Virtual Threads** for sub-second detection and parallel I/O hashing. |
| **Unauthenticated Management Endpoints** | `pom.xml` has no `spring-boot-starter-security`; `/api/baselines/**` is open on localhost. | **Spring Security RBAC + JWT / API Key Auth** separating Read-Only Auditor vs SecOps Admin roles. |
| **Mutable Alert Audit Table** | `baseline_entries` are Ed25519-signed, but `alert_events` rows are plain JPA rows without hash chaining. | **Tamper-Evident Hash-Chained Alert Ledger** (`prev_alert_hash` + Ed25519 signature per alert). |
| **Disk-Resident Private Key** | `SigningService` stores `ed25519_private.key` on local filesystem (`keys/`). | **PKCS#11 HSM / HashiCorp Vault Custody** + automated baseline re-signing queue on rotation. |
| **Individual-File Signing Cost at Scale** | Each file signs 1 Ed25519 envelope individually. | **Merkle Tree Batch Root Signing** for 10,000+ file deployments with $\mathcal{O}(\log N)$ inclusion proofs. |
| **Synthetic Latency Modeling & Classical Crypto** | `latency_analysis.py` models $U(0, 30\text{s})$ Monte Carlo polling; Ed25519 is pre-quantum. | **Live End-to-End JVM Telemetry + JMH + Testcontainers + Hybrid Ed25519 + ML-DSA (FIPS 204)** research evaluation. |

---

## 2. 6-Month Sprint Schedule (Semester 8: Dec 2026 – May 2027)

| Sprint | Month | Focus Track | Key Engineering Deliverables | Target Metrics / Verification |
| :---: | :--- | :--- | :--- | :--- |
| **Sprint 7** | Dec 2026 | **Event-Driven Detection & Concurrency** | Java NIO `WatchService` (with Linux `eBPF` / `fanotify` PoC) + Java 21 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`) in `ComparisonService`. | Mean detection latency reduced from **14.83s $\to$ < 250 ms**; 4x+ parallel scan speedup. |
| **Sprint 8** | Jan 2027 | **Security Hardening & RBAC** | Spring Security role-based access (`ADMIN`, `AUDITOR`), rate limiting, and TLS/mTLS deployment profile. | 100% unauthorized mutation requests blocked (`401/403`); OWASP ZAP clean scan. |
| **Sprint 9** | Feb 2027 | **Hash-Chained Audit Log & Merkle Roots** | `prev_event_hash` cryptographic chaining on `alert_events` + Merkle Tree root signing for bulk baselines. | Instant detection of deleted/reordered alert rows; 10x faster bulk verification. |
| **Sprint 10** | Mar 2027 | **HSM / Vault Key Custody & SIEM Integrations** | PKCS#11 / HashiCorp Vault signer abstraction + RFC 5424 Syslog / Webhook alert forwarders (Slack, Wazuh, Splunk). | Zero private key bytes stored in container filesystem; live webhook/Syslog tests. |
| **Sprint 11** | Apr 2027 | **JMH Microbenchmarks, Testcontainers & Mutation Testing** | Replace synthetic models with live JMH (`openjdk.jmh`), real PostgreSQL 16 `Testcontainers`, and `PITest` mutation coverage. | >85% mutation kill score; real multi-GB disk/DB live latency distribution plots. |
| **Sprint 12** | May 2027 | **Post-Quantum Hybrid Signatures & Research Paper** | Hybrid **Ed25519 + ML-DSA (Dilithium / FIPS 204)** prototype via BouncyCastle + IEEE/Springer comparative paper draft. | Dual-signature verification benchmarks & complete B.Tech Final Defense submission. |
