# Weekly Deadlines & Deliverables Checklist — HashWatch

| Sprint | Cut-off Date | Core Deliverable | Acceptance Criteria |
| :--- | :--- | :--- | :--- |
| **Sprint 1** | Week 1 Sunday 23:59 | Project base, Docker DB, Cryptographic classes, JPA Entities | `.\mvnw.cmd test` compiles and passes; `@DataJpaTest` suite (8 tests), SHA-256, and Ed25519 tests succeed. |
| **Sprint 2** | Week 2 Sunday 23:59 | Comparison engine and automated Quartz scheduler | App logs periodic scan execution every 30s; detects modified test file. |
| **Sprint 3** | Week 3 Sunday 23:59 | Complete REST API | Postman / cURL tests succeed for `/api/files`, `/api/baselines`, and `/api/alerts`. |
| **Sprint 4** | Week 4 Sunday 23:59 | Interactive Web Dashboard | UI loads in browser on port 8080; shows files, alerts, and triggers scans. |
| **Sprint 5** | Week 5 Sunday 23:59 | Python benchmarks & CI pipeline | `python-analysis` scripts output charts comparing SHA-256 and Ed25519 to papers; GitHub CI green. |
| **Sprint 6** | Week 6 Sunday 23:59 | End-to-end hardened demo & Final Report | Live project defense readiness; full documentation synced with codebase. |

---

## Sprint 1 Task Status Checklist

- [x] **S1-T1 (Priyanshu — DevOps / DBMS):** Bundled Apache Maven Wrapper (`mvnw` / `mvnw.cmd`), `.gitattributes`, HikariCP connection pool (`HashWatchHikariPool`), and dual-profile setup (`h2` default + `postgres`).
- [x] **S1-T4 (Priyanshu — DBMS):** Implemented strongly-typed enums (`FileStatus`, `EventType`, `AlertSeverity`), JPA `@Index` & `@ColumnDefault` annotations, FK `ON DELETE CASCADE` / `SET NULL` rules, and `RepositoryIntegrationTest` (8/8 passing, including RepoMind edge cases).
- [x] **S1-T2 (Riya — Cryptology):** Implement Ed25519 key generation, loading, signing, and verification in `SigningService.java` and pass `SigningServiceTest.java` (5/5 tests passing).
- [x] **S1-T3 (Samarjeet — Cryptology & Backend):** Implement 64 KB buffered SHA-256 streaming in `HashingService.java` and pass `HashingServiceTest.java` (5/5 tests passing).

---

## Sprint 2 Task Status Checklist

- [x] **S2-T1 (Priyanshu — Backend):** Implemented `ComparisonService.java` with Triple-Lock canonical envelope (`HashWatch:v1:<path>:<hash>:<size>`), fingerprint-pinned Ed25519 verification in `SigningService.java`, 5-state integrity machine (`UNTRACKED`, `VERIFIED`, `TAMPERED`, `MISSING`, `SIGNATURE_INVALID`), and alert deduplication. All 10 tests in `ComparisonServiceTest.java` passing.
- [x] **S2-T2 (Samarjeet — Backend):** Implement Quartz trigger execution loop, scan timeout handling, and exception resilience in `MonitoringJob.java` & `SchedulerConfig.java` (3/3 tests passing).
- [x] **S2-T3 (Riya — DBMS & API):** Implement alert queries (unresolved alerts, severity filters, event-type filters, date-range, per-file lookups, pagination, aggregation counts) in `AlertEventRepository` and comprehensive persistence tests in `AlertEventRepositoryTest` (18 tests passing).

---

## Sprint 3 Task Status Checklist

- [x] **S3-T1 (Riya & Priyanshu — API):** Implemented `FileController.java` (batch registration, OS readability checks, 100MB cap, soft-delete reactivation) and `BaselineController.java` (baseline feed, re-baseline triggers, public key export). Passed all MockMvc integration test suites (`FileControllerTest`, `BaselineControllerTest`).
- [x] **SA-5 (Riya — Docs):** Documented OpenAPI / REST endpoints contract in `docs/TRD.md` Section 5 covering request/response JSON schemas, query parameters, error responses, and HTTP status codes.
- [x] **S3-T2 (Priyanshu & Riya — API & Backend):** Implemented `AlertController.java` (unresolved alerts, 50 recent alerts, `/resolve`, `/scan-now`) and verified with `AlertControllerTest`.
- [x] **S3-T3 (Samarjeet — Cryptology):** Implement Ed25519 key rotation with timestamped archives, standard RFC 7468 PEM/DER export and parsing, and detached signature validation in `SigningService.java` (12/12 unit tests passing).

---

## Sprint 4 Task Status Checklist

- [x] **S4-T1 (Riya & Priyanshu — Frontend):** Built responsive Thymeleaf dashboard (`dashboard.html`) and dedicated CSS styles (`style.css`) with status badges, metric counters, file tables, and manual scan triggers adhering to `docs/DESIGN_SYSTEM.md`.
- [x] **S4-T2 (Riya — Frontend & API):** Implemented `alerts.html` security incident triage view and wired asynchronous REST client operations in `app.js` for instant scan triggers and alert resolution.
- [x] **S4-T3 / SB-1 (Samarjeet — Backend & DBMS):** Implemented `DashboardViewController.java` populating model attributes with repository count queries (`countByStatus`), verified with `DashboardViewControllerTest`.

---

## Sprint 5 Task Status Checklist

- [x] **S5-T1 (Priyanshu — DevOps):** Multi-stage Docker build for Spring Boot app and GitHub Actions automated CI testing (`.github/workflows/ci.yml`, `Dockerfile`).
- [x] **S5-T2 (Priyanshu — Analysis):** Benchmark SHA-256 throughput vs file size & Ed25519 signs/sec; produce comparison plots (`python-analysis/overhead_benchmark.py`).
- [x] **S5-T3 (Samarjeet — Analysis):** Measure end-to-end detection latency distributions; compute mean, median, P95 metrics (`python-analysis/latency_analysis.py`).
- [x] **S5-T4 (Riya & Priyanshu — Cryptology & API):** Implement end-to-end integration test suite verifying tamper detection, single-bit flip sensitivity, database signature invalidation, missing files, and authorized re-baselining (`TamperDetectionIntegrationTest.java`, 7/7 passing).

---

## Sprint 6 Task Status Checklist

- [x] **S6-T1 (Priyanshu — Analysis & DevOps):** Collate benchmark figures against published research papers (snaproot, Ed25519 reference, VMI container FIM).
- [x] **S6-T2 (Riya — Frontend & UI):** Author Operator User Manual and administration runbook (`docs/USER_MANUAL.md`).
- [x] **SC-3 (Priyanshu, Riya, Samarjeet — Academic Report):** Final Academic Project Report for Asansol Engineering College / MAKAUT CSBS Department (`docs/FINAL_ACADEMIC_REPORT.md`).
- [x] **SC-4 (Priyanshu, Riya, Samarjeet — Defense Deck):** Project Defense & Viva Voce Presentation Deck with Speaker Notes and Examiner Q&A Cheat Sheet (`docs/PROJECT_DEFENSE_PRESENTATION.md`).
- [x] **S6-T3 (Samarjeet — Backend & Cryptology):** Corner-case hardening: missing files, locked files, directory-path guards, and unreadable permissions.

> [!IMPORTANT]
> All Pull Requests for the sprint must be submitted by **Saturday 18:00 IST** to allow review and resolution before the Sunday cutoff.
