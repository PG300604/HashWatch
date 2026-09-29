# Data Flow Diagrams (DFD) — HashWatch

---

## 1. Level 0: Context Diagram

```mermaid
flowchart LR
    Guide[Project Guide / Viewer / Operator] -->|views status & alerts, registers files| Dashboard[Web Dashboard / REST API]
    FS[(Watched Files\non disk)] -->|read file stream| System((HashWatch System))
    System -->|hash & compare| FS
    System -->|serves UI & status| Dashboard
    System -->|writes baselines & alert logs| DB[(PostgreSQL Database)]
    Analyst[Team Member] -->|runs benchmarking scripts| PyScripts[Python Analysis Scripts]
    DB -->|timing & event telemetry| PyScripts
    PyScripts -->|generates throughput/latency charts| Analyst
```

### Context Summary
- **External Entities:** Operators/System Administrators, Filesystem, PostgreSQL, and Team Analysts.
- **Boundary:** HashWatch runs as a resident monitoring daemon with an embedded web server, while the Python benchmarking scripts run offline against collected performance data.

---

## 2. Level 1: Process Decomposition

```mermaid
flowchart TD
    subgraph P1["1.0 Baseline Establishment"]
        direction LR
        A1[Read watched file] --> A2[Compute SHA-256 hash]
        A2 --> A3[Sign hash with Ed25519]
        A3 --> A4[(Store in baseline_entries)]
    end

    subgraph P2["2.0 Scheduled Monitoring"]
        direction LR
        B1[Quartz trigger fires] --> B2[Re-hash watched files]
        B2 --> B3[Compare vs signed baseline]
        B3 -->|match| B4[Set status = VERIFIED]
        B3 -->|mismatch| B5[Set status = TAMPERED]
        B4 --> B6[(alert_events table)]
        B5 --> B6
    end

    subgraph P3["3.0 Reporting & Analysis"]
        direction LR
        C1[REST API reads DB] --> C2[Dashboard renders status]
        C3[Python scripts read DB] --> C4[Generate latency/overhead charts]
    end

    A4 -.->|read active baseline| B3
    B6 --> C1
    B6 --> C3
```

---

## 3. Level 2: Sub-Process Breakdown

### 3.1. Process 1.0 — Baseline Establishment Detail
```mermaid
sequenceDiagram
    autonumber
    actor Admin as Operator / API
    participant FC as FileController
    participant CS as ComparisonService
    participant HS as HashingService
    participant SS as SigningService
    participant DB as PostgreSQL

    Admin->>FC: POST /api/files (filePath)
    FC->>CS: establishBaseline(watchedFile)
    CS->>HS: hashFile(diskFile)
    HS-->>CS: SHA-256 digest string
    CS->>SS: sign(SHA-256 digest)
    SS-->>CS: Base64 Ed25519 signature
    CS->>DB: INSERT INTO baseline_entries (hash, signature, is_current=true)
    CS->>DB: UPDATE watched_files SET status='VERIFIED'
    CS-->>FC: Baseline established
    FC-->>Admin: 200 OK (WatchedFile + Baseline details)
```

### 3.2. Process 2.0 — Scheduled Verification Loop Detail
```mermaid
sequenceDiagram
    autonumber
    participant QZ as Quartz Trigger (Every 30s)
    participant MJ as MonitoringJob
    participant CS as ComparisonService
    participant SS as SigningService
    participant HS as HashingService
    participant DB as PostgreSQL

    QZ->>MJ: execute()
    MJ->>CS: runVerificationScan()
    CS->>DB: SELECT * FROM watched_files WHERE is_active=true
    loop For Each Monitored File
        CS->>DB: SELECT active baseline_entry
        CS->>SS: verify(baseline.sha256, baseline.signature)
        alt Signature Invalid
            CS->>DB: INSERT INTO alert_events (SIGNATURE_INVALID, CRITICAL)
        else Signature Valid
            CS->>HS: hashFile(diskFile)
            alt Hash Matches Baseline
                CS->>DB: UPDATE watched_files SET status='VERIFIED'
            else Hash Mismatch
                CS->>DB: UPDATE watched_files SET status='TAMPERED'
                CS->>DB: INSERT INTO alert_events (UNAUTHORIZED_MODIFICATION, CRITICAL)
            end
        end
    end
```
