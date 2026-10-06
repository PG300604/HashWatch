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

> [!IMPORTANT]
> All Pull Requests for the sprint must be submitted by **Saturday 18:00 IST** to allow review and resolution before the Sunday cutoff.
