# Research Papers for Benchmarking & Comparison — HashWatch

These papers report actual performance numbers that our own measurements (from
`python-analysis/`) can be compared against in the report. Load all of these into NotebookLM
alongside the conceptual papers already listed in the synopsis (Tripwire, Fingerprinting of
Machines, Closing the Visibility Gap).

---

## 1. Hashing Performance

### 1. Performance Evaluation of Hashing Algorithms on Commodity Hardware
- **Link:** [https://arxiv.org/abs/2407.08284](https://arxiv.org/abs/2407.08284)
- **Summary:** Benchmarks SHA-256, SHA-512, and Blake3 hash rate/throughput on commodity desktop and VM hardware — the same class of machine we're testing on.
- **Use for:** Comparing our measured SHA-256 throughput against a published baseline, and to explain why we chose SHA-256 over faster alternatives (standardization, NIST approval, and hardware compatibility over raw speed).

### 2. snaproot: Decentralized File Integrity Verification Using Blockchain-Anchored Cryptographic Hashing
- **Link:** [https://arxiv.org/pdf/2606.10625](https://arxiv.org/pdf/2606.10625)
- **Summary:** Reports SHA-256 hashing throughput of approximately 0.8 GB/s (~800 MB/s) on modern hardware with SHA-NI hardware acceleration, and shows hashing time scales linearly with file size.
- **Use for:** A direct throughput comparison point, and to justify that hashing is not expected to be our system's bottleneck.

---

## 2. Signature (Ed25519) Performance

### 3. High-speed High-security Signatures (the original Ed25519 paper)
- **Link:** [https://ed25519.cr.yp.to/ed25519-20110705.pdf](https://ed25519.cr.yp.to/ed25519-20110705.pdf)
- **Summary:** The foundational Ed25519 paper — reports roughly 109,000 signs/sec and 71,000 verifications/sec on their reference hardware.
- **Use for:** The primary citation for Ed25519's expected performance class, to compare against our own signing/verification benchmarks.

### 4. Performance Analysis and Deployment of Classical and Post-Quantum Signature Schemes
- **Link:** [https://arxiv.org/pdf/2505.02239](https://arxiv.org/pdf/2505.02239)
- **Summary:** A modern benchmarking framework comparing EdDSA (Ed25519) against ECDSA, RSA, and post-quantum schemes, including how performance scales with message size.
- **Use for:** Methodology reference (how to structure our own benchmark: repeated trials, warm-up runs discarded, mean reported) and as a comparison point showing Ed25519's expected advantage over RSA/ECDSA.

---

## 3. File Integrity Monitoring Overhead

### 5. A High-performance Real-time Container File Monitoring Approach Based on Virtual Machine Introspection
- **Link:** [https://arxiv.org/pdf/2509.16030](https://arxiv.org/pdf/2509.16030)
- **Summary:** Reports concrete read/write throughput measurements before and after enabling file monitoring (e.g. 62.72 MB/s $\to$ 61.69 MB/s at a given block size), demonstrating low overhead.
- **Use for:** A template for how to present our own "monitoring overhead" comparison — before/after numbers rather than just an abstract percentage claim.

---

## 4. How to Use These in the Final Report

For each benchmark we run (hashing throughput, signing/verification speed, end-to-end detection latency), include a short comparison line:

> *"Our measured [X] is consistent with / differs from [paper]'s reported [Y], likely due to [hardware/implementation difference]."*

This empirical grounding provides rigorous academic validation as requested by the project guide.
