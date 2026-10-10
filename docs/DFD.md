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

### 3.1. Process 1.0 — Baseline Establishment Detail (Sprint 2: S2-T1)
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
    Note over CS: Build Triple-Lock Canonical Envelope:<br/>HashWatch:v1:<path>:<sha256>:<size>
    CS->>SS: sign(canonicalPayload)
    SS-->>CS: Base64 Ed25519 signature
    CS->>SS: getKeyFingerprint()
    SS-->>CS: SHA-256 public key fingerprint (publicKeyId)
    CS->>DB: UPDATE baseline_entries SET is_current=false WHERE watched_file_id=?
    CS->>DB: INSERT INTO baseline_entries (hash, signature, public_key_id, is_current=true)
    CS->>DB: UPDATE watched_files SET status='VERIFIED', file_size=?, last_modified=?
    CS-->>FC: Baseline established
    FC-->>Admin: 200 OK (WatchedFile + Baseline details)
```

### 3.2. Process 2.0 — Scheduled Verification Loop Detail (Sprint 2: S2-T1)
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
    loop For Each Monitored File (Isolated Try-Catch)
        CS->>DB: SELECT active baseline_entry
        Note over CS: Rebuild Expected Triple-Lock Envelope:<br/>HashWatch:v1:<path>:<sha256>:<size>
        CS->>SS: verify(payload, signature, baseline.publicKeyId)
        alt Public Key Fingerprint Mismatch OR Invalid Ed25519 Sig
            CS->>DB: UPDATE watched_files SET status='SIGNATURE_INVALID'
            CS->>DB: INSERT INTO alert_events (SIGNATURE_INVALID, CRITICAL) [Deduplicated]
        else Signature & Fingerprint Valid (Zero-Trust Verified)
            alt File Missing on Disk
                CS->>DB: UPDATE watched_files SET status='MISSING'
                CS->>DB: INSERT INTO alert_events (MISSING_FILE, CRITICAL) [Deduplicated]
            else File Exists
                CS->>HS: hashFile(diskFile)
                alt Hash Matches Baseline
                    CS->>DB: UPDATE watched_files SET status='VERIFIED', last_checked_at=NOW()
                else Hash Mismatch
                    CS->>DB: UPDATE watched_files SET status='TAMPERED'
                    CS->>DB: INSERT INTO alert_events (UNAUTHORIZED_MODIFICATION, HIGH) [Deduplicated]
                end
            end
        end
    end
```
