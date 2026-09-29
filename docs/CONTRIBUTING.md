# Contributing & Documentation Guidelines — HashWatch

To ensure our team maintains high code quality and our documentation never goes out of sync with code, all group members must follow these guidelines.

---

## 1. Golden Rule: "Code + Docs in the Same Pull Request"

No pull request (PR) will be approved or merged if code changes are made without the corresponding documentation updates.

### Documentation Update Rules by Domain

| Domain of Change | Code Files Modified | Required Documentation Update |
| :--- | :--- | :--- |
| **DBMS** | `entity/*.java`, `repository/*.java`, SQL schemas | **`docs/ERD.md`**: Update the Mermaid ER diagram, column table specifications, and PostgreSQL DDL script. |
| **API** | `controller/*Controller.java` | **`docs/TRD.md`**: Update Section 5 (REST API Specifications) with endpoint method, URL, sample request/response JSON, and HTTP status codes. |
| **Cryptology** | `service/HashingService.java`, `service/SigningService.java` | **`docs/TRD.md`**: Update Section 3 (Cryptographic Implementation Details) with algorithms, key sizes, or curve details. |
| **Backend & Scheduler** | `service/ComparisonService.java`, `scheduler/*.java`, `config/*.java` | **`docs/DFD.md`**: Update Level 1 / Level 2 sequence flow diagrams if verification or polling logic is altered. |
| **Analysis** | `python-analysis/*.py` | **`docs/RESEARCH_BENCHMARKS.md`**: Update the methodology, benchmark findings, or comparison notes with cited papers. |
| **DevOps** | `docker-compose.yml`, `pom.xml`, `.github/` | **`docs/SETUP.md`**: Update local setup instructions or environment flags. |

---

## 2. Git Branching & Commit Message Conventions

### Branch Naming Convention
Branches should clearly indicate the sprint number, domain, and feature:
```
sprint-<SprintNumber>/<domain>-<short-description>
```
**Examples:**
- `sprint-1/cryptology-ed25519-tests`
- `sprint-2/backend-scheduler-tuning`
- `sprint-3/api-files-endpoints`
- `sprint-4/frontend-alert-banner`

### Commit Messages (Conventional Commits)
Use standard prefix tags:
- `feat(domain): description` — New feature or capability.
- `fix(domain): description` — Bug fix or error resolution.
- `docs(name): description` — Documentation only updates.
- `test(domain): description` — Adding or modifying unit tests.
- `refactor(domain): description` — Code refactoring without changing functionality.

**Examples:**
```bash
git commit -m "feat(api): add DELETE /api/files endpoint for unwatching"
git commit -m "docs(erd): add idx_baseline_current index to ERD specification"
git commit -m "test(crypto): add SHA-256 test vector for empty file"
```

---

## 3. Pull Request (PR) Checklist

Before submitting a PR from your fork to `PG300604/HashWatch:main`, verify:
- [ ] Code compiles and passes all unit tests (`mvn test`).
- [ ] Target sprint task ID is referenced (e.g. `Resolves S1-T2`).
- [ ] No hardcoded passwords, private keys, or absolute local machine paths are committed.
- [ ] Corresponding `docs/*.md` file has been updated in the same branch.
- [ ] Branch is synced with upstream `main` without merge conflicts.
