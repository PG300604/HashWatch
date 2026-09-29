#!/usr/bin/env python3
"""
HashWatch Cryptographic & Overhead Benchmark
Measures SHA-256 throughput and Ed25519 signing/verifying speeds,
generating charts to compare against research benchmarks (arxiv:2407.08284, ed25519.cr.yp.to).
"""

import os
import time
import hashlib
import matplotlib.pyplot as plt
from cryptography.hazmat.primitives.asymmetric import ed25519

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "charts")
os.makedirs(OUTPUT_DIR, exist_ok=True)

def benchmark_hashing():
    sizes_mb = [1, 5, 10, 25, 50, 100]
    throughputs = []

    print("[*] Running SHA-256 throughput benchmarks...")
    for size in sizes_mb:
        data = os.urandom(size * 1024 * 1024)
        trials = 5
        start = time.perf_counter()
        for _ in range(trials):
            h = hashlib.sha256()
            h.update(data)
            _ = h.hexdigest()
        elapsed = (time.perf_counter() - start) / trials
        mb_per_sec = size / elapsed
        throughputs.append(mb_per_sec)
        print(f"    Size: {size:3d} MB | Mean Duration: {elapsed*1000:6.2f} ms | Throughput: {mb_per_sec:7.2f} MB/s")

    # Plot
    plt.figure(figsize=(8, 5))
    plt.plot(sizes_mb, throughputs, marker="o", color="#0284c7", linewidth=2.5, label="Measured SHA-256")
    plt.axhline(800, color="#f59e0b", linestyle="--", label="snaproot (SHA-NI Baseline ~800 MB/s)")
    plt.title("SHA-256 Hashing Throughput vs File Size")
    plt.xlabel("File Size (MB)")
    plt.ylabel("Throughput (MB/s)")
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.tight_layout()

    out_file = os.path.join(OUTPUT_DIR, "hashing_throughput.png")
    plt.savefig(out_file, dpi=300)
    print(f"[+] Saved hashing throughput chart to {out_file}")

def benchmark_ed25519():
    print("[*] Running Ed25519 operation benchmarks...")
    priv_key = ed25519.Ed25519PrivateKey.generate()
    pub_key = priv_key.public_key()

    message = b"sample_sha256_hash_digest_for_integrity_monitoring_baseline"
    iterations = 2000

    # Signing
    t0 = time.perf_counter()
    signatures = []
    for _ in range(iterations):
        signatures.append(priv_key.sign(message))
    sign_duration = time.perf_counter() - t0
    signs_per_sec = iterations / sign_duration

    # Verifying
    t1 = time.perf_counter()
    for sig in signatures:
        pub_key.verify(sig, message)
    verify_duration = time.perf_counter() - t1
    verifies_per_sec = iterations / verify_duration

    print(f"    Ed25519 Sign:   {signs_per_sec:,.0f} ops/sec")
    print(f"    Ed25519 Verify: {verifies_per_sec:,.0f} ops/sec")

    # Plot
    plt.figure(figsize=(6, 4.5))
    bars = plt.bar(["Sign", "Verify"], [signs_per_sec, verifies_per_sec], color=["#38bdf8", "#10b981"])
    plt.title("Ed25519 Cryptographic Performance")
    plt.ylabel("Operations per Second")
    for bar in bars:
        yval = bar.get_height()
        plt.text(bar.get_x() + bar.get_width()/2.0, yval + (yval*0.02), f"{yval:,.0f} ops/s", ha="center", va="bottom")
    plt.ylim(0, max(signs_per_sec, verifies_per_sec) * 1.2)
    plt.grid(axis="y", alpha=0.3)
    plt.tight_layout()

    out_file = os.path.join(OUTPUT_DIR, "ed25519_performance.png")
    plt.savefig(out_file, dpi=300)
    print(f"[+] Saved Ed25519 chart to {out_file}")

if __name__ == "__main__":
    benchmark_hashing()
    benchmark_ed25519()
