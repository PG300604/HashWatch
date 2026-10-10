# HashWatch Contributor & Engineering Team Guide 🧭

Welcome to the internal engineering guide for **HashWatch** (Cryptographically Signed File Integrity Monitoring System).  
This guide documents the team development workflow, documentation protocols, branching conventions, and documentation sitemap for team members and open-source contributors.

---

## 1. Team & Domain Specialization Matrix

Our project uses **Event-Based Agile Development**. Work is distributed in weekly sprints based on domain competence, while teammates shift fluidly where extra workforce is required:

| Member | GitHub Handle | Primary Assigned Domains | Key Responsibilities |
| :--- | :--- | :--- | :--- |
| **Priyanshu** *(Lead)* | [`@PG300604`](https://github.com/PG300604) | **Backend**, **DBMS**, **DevOps**, **Analysis**, **Cryptology** | Comparison engine, Maven build/wrapper, HikariCP, JPA schema, benchmarking suite. |
| **Riya** | [`@riyaaa0710`](https://github.com/riyaaa0710) | **Cryptology**, **API**, **Frontend**, **DBMS** | Ed25519 key persistence, REST controllers, Thymeleaf UI, alert triage queries. |
| **Samarjeet** | [`@samarjeet-kr`](https://github.com/samarjeet-kr) | **Backend**, **Cryptology**, **Analysis** | SHA-256 64 KB streaming, Quartz background scheduler, latency distributions. |

---

## 2. Weekly Sprint Workflow & The "Golden Rule"

### The Golden Rule: "Code + Docs in Every PR"
Every feature or bugfix Pull Request **must** update code and documentation in the same commit:
* Never submit a PR containing code without updating the corresponding markdown document in `docs/`.
* Mark your task status as `✅ Done` in [`docs/SPRINT_PLAN.md`](SPRINT_PLAN.md) and check the box `[x]` in [`docs/WEEKLY_DEADLINES.md`](WEEKLY_DEADLINES.md).

### Sprint PR Submission Deadlines
* **Cutoff:** Every **Sunday 23:59 IST**.
* **PR Submission Deadline:** Every **Saturday 18:00 IST** (allows peer code review and conflict resolution before Sunday cutoff).

---

## 3. Branching & Pull Request Protocol

### A. Fork & Branch Naming
All work must happen on dedicated sprint feature branches created from the latest `main`:
```bash
git checkout main
git pull origin main
git checkout -b sprint-<N>/<domain>-<short-description>
```

**Standard Branch Naming Conventions:**
* Sprint 1:
  * `sprint-1/devops-maven-hikari-dbms` (Priyanshu)
  * `sprint-1/cryptology-ed25519-signing` (Riya)
  * `sprint-1/cryptology-sha256-streaming` (Samarjeet)
* Sprint 2:
  * `sprint-2/backend-comparison-engine` (Priyanshu)
  * `sprint-2/backend-quartz-scheduler` (Samarjeet)
  * `sprint-2/dbms-alert-queries` (Riya)

### B. Commit Messages (Conventional Commits)
Follow the standardized commit format:
```text
feat(<domain>): implement <feature description> (Resolves S<N>-T<M>)
test(<domain>): add unit tests for <class>
docs(<doc>): update <document> with <changes>
fix(<domain>): resolve <issue>
```

### C. Local Pre-PR Verification
Before submitting a PR, verify that the entire test suite compiles and succeeds:
```powershell
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```
Ensure zero private keys (`keys/`), local IDE directories (`.idea/`, `.vscode/`), or temporary databases (`data/`) are staged.

---

## 4. Documentation Mapping & Reference Sitemap

| Documentation File | Target Audience | When to Read / Update |
| :--- | :--- | :--- |
| **[`docs/BRAIN.md`](BRAIN.md)** | All Teammates | Architecture Decision Records (ADRs), collaboration core, sprint allocation matrix. |
| **[`docs/PRD.md`](PRD.md)** | Product / Evaluators | Personas, functional requirements (FR1–FR8), non-functional benchmarks. |
| **[`docs/TRD.md`](TRD.md)** | Backend / Crypto / API | Cryptographic specs, database schema, REST API endpoints, scheduler parameters. |
| **[`docs/ERD.md`](ERD.md)** | DBMS / Backend | Relational entity diagrams, PostgreSQL DDL, foreign keys, indexes, cascades. |
| **[`docs/DFD.md`](DFD.md)** | Evaluators / Architecture | Level 0 Context, Level 1 Process Breakdown, Level 2 sequence diagrams. |
| **[`docs/SPRINT_PLAN.md`](SPRINT_PLAN.md)** | All Teammates | Weekly task cards (S1 to S6), folder targets, Definition of Done (DoD). |
| **[`docs/WEEKLY_DEADLINES.md`](WEEKLY_DEADLINES.md)** | All Teammates | Cutoff milestones, deliverable acceptance criteria, task checklists. |
| **[`docs/ROLES.md`](ROLES.md)** | Contributors | Git fork workflow, domain responsibilities, collaboration rules. |
| **[`docs/SETUP.md`](SETUP.md)** | New Developers | Java 17, Maven Wrapper, Docker PostgreSQL, Python virtual environment. |
| **[`docs/PACKAGE_STRUCTURE.md`](PACKAGE_STRUCTURE.md)** | Developers | Java and Python folder directory layout and class locations. |
| **[`docs/CONTRIBUTING.md`](CONTRIBUTING.md)** | Contributors | PR templates, code review checklists, coding standards. |
| **[`docs/RESEARCH_BENCHMARKS.md`](RESEARCH_BENCHMARKS.md)** | Analysts / Evaluators | Academic citations (*snaproot*, Bernstein Ed25519, commodity hashing papers). |

---

## 5. Local Environment Troubleshooting

### Java 17 Path Issues on Windows
If `mvnw.cmd` complains about `JAVA_HOME`, the wrapper script has built-in auto-detection for Adoptium/Eclipse Temurin OpenJDK at `C:\Program Files\Eclipse Adoptium\jdk-17.*`. You can also explicitly set it in your terminal session:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
```

### Knowledge Graph Maintenance (Local Graphify)
The structural knowledge graph lives in `graphify-out/` and is strictly local (ignored in Git):
```powershell
# Rebuild or query knowledge graph
& 'C:\Users\priya\AppData\Local\Programs\Python\Python312\python.exe' -m graphify query "<question>"
```
