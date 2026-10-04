# Developer Setup Guide — HashWatch

Welcome to HashWatch! Follow this guide to set up your local development environment.

---

## 1. Prerequisites

Ensure you have the following installed:
- **Java Development Kit (JDK):** Version 17 or higher (Eclipse Temurin / OpenJDK 17 recommended).
- **Maven:** *Optional!* This repository bundles the **Apache Maven Wrapper** (`mvnw.cmd` for Windows, `./mvnw` for Linux/macOS), which automatically downloads and uses Maven 3.9.9.
- **Git:** 2.30+.
- **Python:** 3.10+ (for `python-analysis/` benchmarking suite).
- **Docker & Docker Compose (Optional):** For running PostgreSQL 16 locally.

Check versions:
```bash
java -version
python --version
git --version
```

---

## 2. Forking and Cloning the Repository

1. Visit [https://github.com/PG300604/HashWatch](https://github.com/PG300604/HashWatch) and click **Fork**.
2. Clone your personal fork:
   ```bash
   git clone https://github.com/<YOUR_USERNAME>/HashWatch.git
   cd HashWatch
   ```
3. Add the upstream remote to sync future updates:
   ```bash
   git remote add upstream https://github.com/PG300604/HashWatch.git
   git fetch upstream
   ```

---

## 3. Database Profiles & HikariCP Connection Pooling (Sprint 1: S1-T1)

HashWatch uses a dual-profile database setup with **HikariCP** (`HashWatchHikariPool`, max pool size `10`, min idle `2`):

### Option A: Default Zero-Setup Mode (`h2` Profile)
By default, [`application.properties`](../src/main/resources/application.properties) activates the `h2` profile ([`application-h2.properties`](../src/main/resources/application-h2.properties)) so you can run tests or launch the server immediately without installing PostgreSQL or Docker:
```powershell
# Windows (PowerShell / CMD)
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
- **H2 Web Console:** `http://localhost:8080/h2-console`
  - **JDBC URL:** `jdbc:h2:mem:hashwatch_db`
  - **User:** `sa` | **Password:** *(leave empty)*

### Option B: Production Mode (`postgres` Profile via Docker Compose)
To run against PostgreSQL 16 ([`application-postgres.properties`](../src/main/resources/application-postgres.properties)):
```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres
```
- **Database:** `hashwatch_db` (`localhost:5432`)
- **User:** `hashwatch` | **Password:** `hashwatch_secret`

---

## 4. Building and Running Tests

### Run Automated JUnit & `@DataJpaTest` Suite
```powershell
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

Once the server is started, open your browser:
- **Web Dashboard:** [http://localhost:8080](http://localhost:8080)
- **Security Alerts:** [http://localhost:8080/alerts](http://localhost:8080/alerts)
- **REST Endpoints:** [http://localhost:8080/api/files](http://localhost:8080/api/files)

---

## 5. Setting Up Python Analysis Suite

To run the offline benchmarking scripts:
```bash
cd python-analysis
python -m venv venv

# On Windows:
venv\Scripts\activate
# On Linux/macOS:
source venv/bin/activate

pip install -r requirements.txt
```

### Run Benchmarks:
```bash
python overhead_benchmark.py
python latency_analysis.py
```
Generated PNG charts will be saved directly into `python-analysis/charts/`.

---

## 6. How to Contribute to a Sprint Task

1. Review your task in [`docs/SPRINT_PLAN.md`](SPRINT_PLAN.md).
2. Branch off `main`:
   ```bash
   git checkout -b sprint-1/<domain>-<feature-name>
   ```
3. Implement your changes in the designated folders (see [`docs/PACKAGE_STRUCTURE.md`](PACKAGE_STRUCTURE.md)).
4. **Update Documentation:** Always edit the corresponding section in `docs/`! (See [`docs/CONTRIBUTING.md`](CONTRIBUTING.md)).
5. Push to your fork and submit a PR to `PG300604/HashWatch:main`.
