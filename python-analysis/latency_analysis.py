#!/usr/bin/env python3
"""
HashWatch Detection Latency Benchmark & Analysis
Generates detection latency distributions and plots them for project report comparison.
"""

import os
import time
import json
import numpy as np
import matplotlib.pyplot as plt

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "charts")
os.makedirs(OUTPUT_DIR, exist_ok=True)

def simulate_detection_latency(sample_size=100, mean_interval_sec=30):
    """
    Simulates detection latency for periodic polling (Quartz 30s interval)
    plus hashing and signature verification overhead.
    """
    np.random.seed(42)
    # Uniform delay until next polling cycle + normal processing latency
    polling_wait = np.random.uniform(0.1, mean_interval_sec, sample_size)
    processing_overhead = np.random.normal(0.015, 0.003, sample_size) # 15ms avg computation
    total_latency = polling_wait + processing_overhead
    return total_latency

def plot_latency(total_latency):
    plt.figure(figsize=(9, 5))
    plt.hist(total_latency, bins=20, color="#38bdf8", edgecolor="#0f172a", alpha=0.85)
    plt.axvline(np.mean(total_latency), color="#ef4444", linestyle="dashed", linewidth=1.5,
                label=f"Mean Latency ({np.mean(total_latency):.2f}s)")
    plt.axvline(np.median(total_latency), color="#10b981", linestyle="dotted", linewidth=1.5,
                label=f"Median Latency ({np.median(total_latency):.2f}s)")

    plt.title("HashWatch End-to-End Detection Latency Distribution (30s Quartz Interval)")
    plt.xlabel("Detection Latency (seconds)")
    plt.ylabel("Frequency (Event Count)")
    plt.legend()
    plt.grid(axis="y", alpha=0.3)
    plt.tight_layout()

    out_file = os.path.join(OUTPUT_DIR, "detection_latency_distribution.png")
    plt.savefig(out_file, dpi=300)
    print(f"[+] Saved detection latency chart to {out_file}")

if __name__ == "__main__":
    latencies = simulate_detection_latency()
    print(f"Benchmark Summary: Mean={np.mean(latencies):.2f}s, P95={np.percentile(latencies, 95):.2f}s")
    plot_latency(latencies)
