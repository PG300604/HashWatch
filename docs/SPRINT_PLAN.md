# Weekly Sprint Plan & Task Allocations — HashWatch

---

## 1. Sprint Cadence & Principles

- **Sprint Duration:** 1 Calendar Week (Sunday midnight planning $\to$ Saturday evening code freeze & review).
- **Model:** Event-Based Dynamic Allocation across domains (Backend, DBMS, DevOps, Analysis, Cryptology, API, Frontend).
- **Core Rule:** Each task ticket specifies:
  1. **Assigned Member** & **Domain**
  2. **Target Files / Directories**
  3. **Required Documentation Updates**
  4. **Definition of Done (DoD)**

---

## 2. Sprint Breakdown

### Sprint 1: Foundation, Cryptographic Primitives & Database Schema
**Focus:** Cryptology, DBMS, DevOps Setup.

| Task ID | Domain | Assigned To | Status | Folder / Target Files | Task Description | Doc Update Required |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **S1-T1** | DevOps / DBMS | Priyanshu | ✅ **Done** | `mvnw`, `mvnw.cmd`, `.gitattributes`, `docker-compose.yml`, `src/main/resources/application*.properties` | Bundle Apache Maven Wrapper (v3.9.9); configure HikariCP connection pool (`HashWatchHikariPool`); setup dual database profiles (`h2` default + `postgres`). | `docs/SETUP.md`, `docs/TRD.md` |
| **S1-T2** | Cryptology | Riya | ⏳ **Pending (Riya)** | `src/main/java/com/hashwatch/service/SigningService.java`, `src/test/java/.../SigningServiceTest.java` | Implement Ed25519 keypair loading/generation, signing, and verification; enable and pass `SigningServiceTest`. | `docs/TRD.md` (Sec 3.2) |
| **S1-T3** | Cryptology & Backend | Samarjeet | ✅ **Done** | `src/main/java/com/hashwatch/service/HashingService.java`, `src/test/java/.../HashingServiceTest.java` | Implement 64 KB buffered SHA-256 streaming in `HashingService`; enable and pass `HashingServiceTest`. | `docs/TRD.md` (Sec 3.1) |
| **S1-T4** | DBMS | Priyanshu | ✅ **Done** | `src/main/java/com/hashwatch/entity/`, `repository/`, `src/test/java/.../RepositoryIntegrationTest.java` | Implement strongly-typed enums (`FileStatus`, `EventType`, `AlertSeverity`), JPA `@Index` & `@ColumnDefault` annotations, FK `CASCADE` / `SET NULL` rules, and 8 `@DataJpaTest` integration tests. | `docs/ERD.md`, `docs/PACKAGE_STRUCTURE.md` |

#### Sprint 1 Completion Summary
- **Build & DevOps (`S1-T1` - Priyanshu):** Added `mvnw` / `mvnw.cmd` (Maven 3.9.9), `.gitattributes` (enforcing LF on `mvnw`), `application-postgres.properties`, and HikariCP connection pool configuration in `application.properties`.
- **Cryptology & Backend (`S1-T3` - Samarjeet):** Implemented streaming SHA-256 hashing with 64 KB buffer in `HashingService.java`, `hashString(String input)`, validation against null/missing files, and enabled all 5 unit tests in `HashingServiceTest.java` (all passing).
- **DBMS & Schema (`S1-T4` - Priyanshu):** Created `FileStatus`, `EventType`, and `AlertSeverity` enums; added JPA `@Index`, `@ColumnDefault`, `@OnDelete(CASCADE / SET_NULL)`, and `@PrePersist` hooks on `WatchedFile`, `BaselineEntry`, and `AlertEvent`; created `RepositoryIntegrationTest.java` (8/8 tests passing, including RepoMind edge cases for enum string mappings, missing `status` migration defaults, FK cascade/set-null, and detached `AlertEvent` creation).

---

### Sprint 2: Core Integrity Engine & Scheduled Monitoring
**Focus:** Backend, DBMS, Cryptology.

| Task ID | Domain | Assigned To | Folder / Target Files | Task Description | Doc Update Required |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **S2-T1** | Backend | Priyanshu | `src/main/java/com/hashwatch/service/ComparisonService.java` | Implement `establishBaseline()` and `verifyFile()` logic with status state machine. | `docs/DFD.md` (Level 2) |
| **S2-T2** | Backend | Samarjeet | `src/main/java/com/hashwatch/scheduler/MonitoringJob.java`, `config/SchedulerConfig.java` | Quartz trigger execution loop; handle job exceptions and scan timeouts. | `docs/TRD.md` (Scheduler) |
| **S2-T3** | DBMS & API | Riya | `src/main/java/com/hashwatch/repository/`, `entity/AlertEvent.java` | Implement alert queries (unresolved alerts, severity filters) and persistence tests. | `docs/ERD.md` (Table 3.3) |

---

### Sprint 3: REST API & Baseline Management Layer
**Focus:** API, Backend, Cryptology.

| Task ID | Domain | Assigned To | Folder / Target Files | Task Description | Doc Update Required |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **S3-T1** | API | Riya | `src/main/java/com/hashwatch/controller/FileController.java`, `BaselineController.java` | Implement endpoints for file registration, re-baselining, and public key retrieval. | `docs/TRD.md` (API Spec) |
| **S3-T2** | API & Backend | Priyanshu | `src/main/java/com/hashwatch/controller/AlertController.java` | Implement alert triage endpoints (`/api/alerts`, `/resolve`, `/scan-now`). | `docs/TRD.md` (Alerts API) |
| **S3-T3** | Cryptology | Samarjeet | `src/main/java/com/hashwatch/service/SigningService.java` | Handle key rotation, PEM/DER encoding, and detached signature validation. | `docs/TRD.md` (Key Storage) |

---

### Sprint 4: Web Dashboard & Frontend Integration
**Focus:** Frontend, API, DBMS.

| Task ID | Domain | Assigned To | Folder / Target Files | Task Description | Doc Update Required |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **S4-T1** | Frontend | Riya | `src/main/resources/templates/dashboard.html`, `static/css/style.css` | Build responsive dashboard UI with status badges, metric counters, and file tables. | `docs/PRD.md` (UI section) |
| **S4-T2** | Frontend & API | Riya | `src/main/resources/static/js/app.js`, `templates/alerts.html` | Wire asynchronous REST calls for instant scan triggers, alert resolution, and live reload. | `docs/TRD.md` |
| **S4-T3** | Backend & DBMS | Samarjeet | `src/main/java/com/hashwatch/controller/DashboardViewController.java` | Model attribute aggregation (count queries, alert summaries) for Thymeleaf rendering. | `docs/PACKAGE_STRUCTURE.md` |

---

### Sprint 5: DevOps Automation, Performance Benchmarking & Python Analysis
**Focus:** DevOps, Analysis, Backend.

| Task ID | Domain | Assigned To | Folder / Target Files | Task Description | Doc Update Required |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **S5-T1** | DevOps | Priyanshu | `.github/workflows/ci.yml`, `Dockerfile` | Multi-stage Docker build for Spring Boot app and GitHub Actions automated CI testing. | `docs/SETUP.md` |
| **S5-T2** | Analysis | Priyanshu | `python-analysis/overhead_benchmark.py` | Benchmark SHA-256 throughput vs file size & Ed25519 signs/sec; produce comparison plots. | `docs/RESEARCH_BENCHMARKS.md` |
| **S5-T3** | Analysis | Samarjeet | `python-analysis/latency_analysis.py` | Measure end-to-end detection latency distributions; compute mean, median, P95 metrics. | `docs/RESEARCH_BENCHMARKS.md` |
| **S5-T4** | Cryptology & API | Riya | `src/test/java/` | Integration testing of tamper detection when malicious bytes are injected into watched files. | `docs/PRD.md` (Testing) |

---

### Sprint 6: System Hardening, Academic Verification & Final Deliverables
**Focus:** All Team Members.

| Task ID | Domain | Assigned To | Folder / Target Files | Task Description | Doc Update Required |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **S6-T1** | Analysis & DevOps | Priyanshu | `python-analysis/charts/`, Final Report | Collate benchmark figures against published research papers (snaproot, Ed25519 reference). | Final Synopsis & Report |
| **S6-T2** | Frontend & UI | Riya | `src/main/resources/` | UI polish, accessibility checks, dark-mode enhancements, and demo preparation. | User Manual |
| **S6-T3** | Backend & Cryptology | Samarjeet | `src/main/java/com/hashwatch/` | Corner-case hardening: missing files, locked files, unreadable directory permissions. | `docs/TRD.md` |
