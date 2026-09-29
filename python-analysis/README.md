# Python Analysis & Benchmarking

This module contains offline benchmarking scripts designed to test and compare HashWatch performance metrics against academic literature.

## Requirements
```bash
pip install -r requirements.txt
```

## Running Benchmarks
- **Hashing & Cryptography Overhead:**
  ```bash
  python overhead_benchmark.py
  ```
  Generates `charts/hashing_throughput.png` and `charts/ed25519_performance.png`.

- **Detection Latency Analysis:**
  ```bash
  python latency_analysis.py
  ```
  Generates `charts/detection_latency_distribution.png`.
