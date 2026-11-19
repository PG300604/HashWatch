# HashWatch Design System & UI Specification
**Codename:** *Obsidian & Electric Vermilion (Cyber-FIM SOC)*  
**Primary Reference:** Arounda Dark Data Architecture  
**Target Audience:** Security Operations Engineers, University Evaluators, Frontend Developers  
**Author / Attributions:** Co-authored by Group 3 (Riya & Priyanshu)

---

## 1. Design Philosophy & Guidelines

The HashWatch user interface is engineered to reflect an enterprise-grade **Cryptographically Signed File Integrity Monitoring (FIM)** system. It replaces generic, toy-like "AI slop" aesthetics (slate-900 navy, emoji icons, oversized gradients) with high-density, precision-engineered cybersecurity ergonomics:

1. **Obsidian Depth over Washed-out Navy:** True pitch-dark canvases (`#0c0d10`) minimize eye fatigue in SOC environments and elevate high-contrast cryptographic signals.
2. **Hairline Micro-Borders & Subtle Top Highlights:** Cards utilize `1px solid rgba(255, 255, 255, 0.07)` and inner top specular reflections (`inset 0 1px 0 rgba(255,255,255,0.06)`) rather than heavy drop shadows.
3. **No System Emojis:** All actions, status indicators, and entity types must use clean, vector-rendered 1.5px/2px SVG icons (Phosphor / Lucide geometry).
4. **Data Density & Monospace Rigor:** Hashes, key fingerprints, file paths, and timestamps are always formatted in `JetBrains Mono` with truncation and tooltip inspection.
5. **Electric Vermilion CTA Accent:** Crucial operational commands (such as out-of-band integrity verification scans) stand out via high-intensity vermilion (`#fb4202`).

---

## 2. Color Palette & Design Tokens

```css
:root {
    /* ── Canvas & Surface Tokens ── */
    --bg-canvas: #0c0d10;              /* True obsidian canvas */
    --bg-surface: #141519;             /* Primary card surface */
    --bg-surface-elevated: #1a1c22;    /* Interactive / Hover surface */
    --bg-surface-subtle: #18191f;      /* Secondary container */
    --bg-glass: rgba(20, 21, 25, 0.85);/* Top navbar frosted backdrop */

    /* ── Borders & Separators ── */
    --border-subtle: rgba(255, 255, 255, 0.07);
    --border-card: rgba(255, 255, 255, 0.08);
    --border-glow: rgba(255, 255, 255, 0.14);
    --border-focus: rgba(251, 66, 2, 0.5);

    /* ── Primary Dynamic Accents ── */
    --accent-vermilion: #fb4202;       /* Electric Vermilion (Arounda aesthetic) */
    --accent-vermilion-hover: #ff5722;
    --accent-vermilion-glow: rgba(251, 66, 2, 0.28);
    --accent-crypto: #6366f1;          /* Ed25519 SunEC Violet */
    --accent-crypto-subtle: rgba(99, 102, 241, 0.12);

    /* ── State & Severity Tokens ── */
    --status-verified: #10b981;        /* Cyber Emerald (Intact) */
    --status-verified-bg: rgba(16, 185, 129, 0.10);
    --status-verified-border: rgba(16, 185, 129, 0.28);

    --status-tampered: #f43f5e;        /* Crimson Alert (Digest mismatch) */
    --status-tampered-bg: rgba(244, 63, 94, 0.12);
    --status-tampered-border: rgba(244, 63, 94, 0.32);

    --status-missing: #f59e0b;         /* Amber Warning (File deleted) */
    --status-missing-bg: rgba(245, 158, 11, 0.10);
    --status-missing-border: rgba(245, 158, 11, 0.28);

    --status-invalid: #ec4899;         /* Signature / Envelope invalid */
    --status-invalid-bg: rgba(236, 72, 153, 0.12);
    --status-invalid-border: rgba(236, 72, 153, 0.32);

    /* ── Typography & Contrast Hierarchy ── */
    --text-primary: #f8fafc;          /* High-contrast headings and active stats */
    --text-secondary: #94a3b8;        /* Body copy and data table cells */
    --text-muted: #64748b;            /* Metadata labels, subtitles, table headers */

    /* ── Geometry & Layout ── */
    --radius-sm: 6px;
    --radius-md: 10px;
    --radius-lg: 14px;
    --radius-full: 9999px;
    --font-sans: 'Plus Jakarta Sans', system-ui, -apple-system, sans-serif;
    --font-mono: 'JetBrains Mono', 'Fira Code', monospace;
}
```

---

## 3. Typography Scale & Pairing

| Token | Family | Size / Weight | Line Height | Usage |
| :--- | :--- | :--- | :--- | :--- |
| **Display Title** | `Plus Jakarta Sans` | `1.5rem` (24px) / Bold 700 | 1.2 | Brand title, Major section headers |
| **KPI Metric Value**| `JetBrains Mono` | `2.1rem` (34px) / SemiBold 600 | 1.1 | Numeric counters (Active Files, Baselines) |
| **Card Header** | `Plus Jakarta Sans` | `0.95rem` (15px) / SemiBold 600 | 1.3 | Metric card titles, Table headers |
| **Sublabel / Meta** | `Plus Jakarta Sans` | `0.72rem` (11.5px) / Medium 500 | 1.0 | Uppercase section labels (tracking intervals) |
| **Data Code / Hash**| `JetBrains Mono` | `0.80rem` (13px) / Regular 400 | 1.4 | SHA-256 digests, Ed25519 keys, file paths |

---

## 4. Component Standards

### A. Top Navigation Bar (`.navbar`)
* **Background:** Sticky frosted glass (`background: var(--bg-glass)`, `backdrop-filter: blur(14px)`).
* **Brand Cluster:** Micro-emblem with SVG shield and cryptographic pulse badge.
* **Scan Trigger Button (`#scanBtn`):** Electric Vermilion fill (`#fb4202`) with smooth ambient glow (`box-shadow: 0 0 16px var(--accent-vermilion-glow)`).

### B. Health Status Radar Banner (`.health-banner`)
* Displays an animated dual-ring radar beacon (`pulse-beacon`) and instant state summary.
* Automatically adjusts color between Cyber Emerald (`.health-ok`) and Crimson Alert (`.health-breached`).

### C. Metric Cards Grid (`.metrics-grid`)
* 4-column responsive grid with minimum width `220px`.
* Top-right SVG icon container with translucent backdrop.
* Subtitle providing contextual baseline or incident feedback.

### D. Inventory & Incident Data Tables (`table`)
* **Header:** Sticky, uppercase text with letter-spacing `0.05em` (`var(--text-muted)`).
* **Row Hover:** Subtle luminescence (`background: rgba(255, 255, 255, 0.025)`).
* **Status Badges:** Rounded pills (`border-radius: 9999px`) with 1px border matching the status color.
* **Hash Diff Container:** Monospace side-by-side comparison for incident triage.

### E. Floating Toast Notifications (`.toast-container`)
* Fixed at bottom-right (`bottom: 2rem; right: 2rem; z-index: 1000`).
* Dark frosted pill with 300ms ease slide-up and auto-dismiss after 3.5 seconds.

---

## 5. Developer Instructions for Future Sprints

When adding new screens or modals (e.g. Sprint C Benchmark Visualizer, Audit Report Views):
1. Always link `style.css` and use the CSS variables defined above.
2. Use inline SVGs with `width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"`.
3. Never introduce third-party CSS frameworks (Bootstrap, Tailwind CDN, Bulma) to keep the client 100% lightweight and offline-functional.
4. For any code additions, maintain clear domain separation and include proper co-authorship metadata in PR descriptions.
