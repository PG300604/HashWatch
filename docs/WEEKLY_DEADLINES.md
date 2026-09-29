# Weekly Deadlines & Deliverables Checklist — HashWatch

| Sprint | Cut-off Date | Core Deliverable | Acceptance Criteria |
| :--- | :--- | :--- | :--- |
| **Sprint 1** | Week 1 Sunday 23:59 | Project base, Docker DB, Cryptographic classes, JPA Entities | `mvn test` compiles and passes; SHA-256 and Ed25519 tests succeed; PostgreSQL connects. |
| **Sprint 2** | Week 2 Sunday 23:59 | Comparison engine and automated Quartz scheduler | App logs periodic scan execution every 30s; detects modified test file. |
| **Sprint 3** | Week 3 Sunday 23:59 | Complete REST API | Postman / cURL tests succeed for `/api/files`, `/api/baselines`, and `/api/alerts`. |
| **Sprint 4** | Week 4 Sunday 23:59 | Interactive Web Dashboard | UI loads in browser on port 8080; shows files, alerts, and triggers scans. |
| **Sprint 5** | Week 5 Sunday 23:59 | Python benchmarks & CI pipeline | `python-analysis` scripts output charts comparing SHA-256 and Ed25519 to papers; GitHub CI green. |
| **Sprint 6** | Week 6 Sunday 23:59 | End-to-end hardened demo & Final Report | Live project defense readiness; full documentation synced with codebase. |

> [!IMPORTANT]
> All Pull Requests for the sprint must be submitted by **Saturday 18:00 IST** to allow review and resolution before the Sunday cutoff.
