# Package & Folder Structure — HashWatch

HashWatch follows standard Maven multi-tier conventions. Refer to this map to locate files relevant to your assigned sprint tasks.

```
hashwatch/
├── pom.xml                                  # Maven project object model & dependencies
├── mvnw / mvnw.cmd                          # Maven Wrapper (auto-downloads Maven 3.9.9)
├── docker-compose.yml                       # PostgreSQL 16 container definition
├── .gitignore                               # Excludes keys, target, local configs, venv
├── README.md                                # Project landing page and documentation index
├── src/
│   ├── main/
│   │   ├── java/com/hashwatch/
│   │   │   ├── HashWatchApplication.java    # Spring Boot entry point
│   │   │   │
│   │   │   ├── controller/                  # [API & Frontend Domains]
│   │   │   │   ├── FileController.java      # REST endpoints for monitored files
│   │   │   │   ├── BaselineController.java  # REST endpoints for cryptographic baselines
│   │   │   │   ├── AlertController.java     # REST endpoints for security alerts
│   │   │   │   └── DashboardViewController.java # Thymeleaf MVC view controller
│   │   │   │
│   │   │   ├── service/                     # [Cryptology & Backend Domains]
│   │   │   │   ├── HashingService.java      # 64 KB buffered SHA-256 computation
│   │   │   │   ├── SigningService.java      # Ed25519 keypair management & digital signatures
│   │   │   │   └── ComparisonService.java   # Core verification engine & state transitions
│   │   │   │
│   │   │   ├── scheduler/                   # [Backend Domain]
│   │   │   │   └── MonitoringJob.java       # Quartz scheduled execution job
│   │   │   │
│   │   │   ├── entity/                      # [DBMS Domain]
│   │   │   │   ├── WatchedFile.java         # JPA entity for registered files
│   │   │   │   ├── BaselineEntry.java       # JPA entity for signed baselines
│   │   │   │   ├── AlertEvent.java          # JPA entity for security anomaly logs
│   │   │   │   ├── FileStatus.java          # Enum: VERIFIED, TAMPERED, MISSING, UNTRACKED, SIGNATURE_INVALID
│   │   │   │   ├── EventType.java           # Enum: MISMATCH, UNAUTHORIZED_MODIFICATION, MISSING_FILE, SIGNATURE_INVALID
│   │   │   │   └── AlertSeverity.java       # Enum: LOW, MEDIUM, HIGH, CRITICAL
│   │   │   │
│   │   │   ├── repository/                  # [DBMS Domain]
│   │   │   │   ├── WatchedFileRepository.java
│   │   │   │   ├── BaselineEntryRepository.java
│   │   │   │   └── AlertEventRepository.java
│   │   │   │
│   │   │   └── config/                      # [Backend & DevOps Domains]
│   │   │       └── SchedulerConfig.java     # Quartz job detail and trigger configuration
│   │   │
│   │   └── resources/
│   │       ├── application.properties       # Base config + HikariCP connection pool (defaults to h2)
│   │       ├── application-h2.properties    # In-memory H2 profile config
│   │       ├── application-postgres.properties # PostgreSQL 16 production profile config
│   │       ├── application-local.properties.example # Template for local developer overrides
│   │       ├── templates/                   # [Frontend Domain]
│   │       │   ├── dashboard.html           # Main monitoring dashboard
│   │       │   └── alerts.html              # Security alert log and resolution interface
│   │       └── static/                      # [Frontend Domain]
│   │           ├── css/
│   │           │   └── style.css            # Responsive dark-theme design
│   │           └── js/
│   │               └── app.js               # Dynamic REST actions and triggers
│   │
│   └── test/java/com/hashwatch/             # Automated test suite
│       ├── HashWatchApplicationTests.java
│       ├── repository/
│       │   └── RepositoryIntegrationTest.java # @DataJpaTest DBMS verification suite (S1-T4)
│       └── service/
│           ├── HashingServiceTest.java      # Unit tests for SHA-256 hashing (S1-T3)
│           └── SigningServiceTest.java      # Unit tests for Ed25519 signatures (S1-T2)
│
├── python-analysis/                         # [Analysis Domain]
│   ├── requirements.txt                     # Python dependencies (matplotlib, numpy, etc.)
│   ├── latency_analysis.py                  # End-to-end detection latency simulation & plotting
│   ├── overhead_benchmark.py                # SHA-256 & Ed25519 performance benchmark script
│   ├── README.md
│   └── charts/                              # Output directory for generated figures (.gitkeep)
│
└── docs/                                    # Project Knowledge Base
    ├── BRAIN.md                             # Project Brain & Architecture Decision Records
    ├── PRD.md                               # Product Requirements Document
    ├── TRD.md                               # Technical Requirements Document
    ├── ERD.md                               # Entity-Relationship Document & DDL
    ├── DFD.md                               # Data Flow Diagrams (Level 0, 1, 2)
    ├── ROLES.md                             # Domain matrix & fork/PR collaboration guide
    ├── SPRINT_PLAN.md                       # Weekly sprint task allocations and DoD
    ├── TIMELINE.md                          # Mermaid Gantt chart of milestones
    ├── WEEKLY_DEADLINES.md                  # Deadlines and deliverables checklist
    ├── PACKAGE_STRUCTURE.md                 # Folder structure reference (this file)
    ├── SETUP.md                             # Step-by-step developer environment setup
    ├── CONTRIBUTING.md                      # Documentation update rules and Git conventions
    └── RESEARCH_BENCHMARKS.md               # Research papers for benchmarking & comparison
```
