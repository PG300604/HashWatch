/**
 * =============================================================================
 * DOMAIN: Frontend & API
 * ASSIGNED TO: Riya (Sprint 4)
 * FOLDER / TARGET: src/main/resources/static/js/app.js
 * DOC TO UPDATE: docs/TRD.md (Section 5 REST API)
 * =============================================================================
 *
 * Task Description:
 * Client-side asynchronous interaction handling: triggers verification scans,
 * submits new file registration, requests re-baselining, and resolves alerts.
 */

// Trigger manual integrity scan
async function triggerScan() {
    // TODO [Sprint 4 - Frontend]: Assigned to Riya
    // POST /api/alerts/scan-now, alert user and reload on success
    try {
        const res = await fetch('/api/alerts/scan-now', { method: 'POST' });
        if (res.ok) {
            alert('Integrity scan executed.');
            location.reload();
        } else {
            alert('Scan trigger failed.');
        }
    } catch (e) {
        console.error(e);
    }
}

// Register a new file
async function registerFile(event) {
    event.preventDefault();
    // TODO [Sprint 4 - Frontend]: Assigned to Riya
    // Read #filePathInput, POST /api/files with { filePath }
    const filePath = document.getElementById('filePathInput').value.trim();
    if (!filePath) return;

    try {
        const res = await fetch('/api/files', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filePath })
        });
        if (res.ok) {
            location.reload();
        }
    } catch (e) {
        console.error(e);
    }
}

// Re-baseline a single file
async function rebaselineSingle(fileId) {
    // TODO [Sprint 4 - Frontend]: Assigned to Riya
    // POST /api/baselines/generate/{fileId}
    if (!confirm('Regenerate and sign cryptographic baseline?')) return;
    try {
        const res = await fetch(`/api/baselines/generate/${fileId}`, { method: 'POST' });
        if (res.ok) location.reload();
    } catch (e) {
        console.error(e);
    }
}

// Re-baseline all files
async function rebaselineAll() {
    // TODO [Sprint 4 - Frontend]: Assigned to Riya
    if (!confirm('Rebaseline all files?')) return;
    try {
        const res = await fetch('/api/baselines/generate-all', { method: 'POST' });
        if (res.ok) location.reload();
    } catch (e) {
        console.error(e);
    }
}

// Remove/unwatch file
async function removeFile(fileId) {
    // TODO [Sprint 4 - Frontend]: Assigned to Riya
    if (!confirm('Deactivate monitoring for this file?')) return;
    try {
        const res = await fetch(`/api/files/${fileId}`, { method: 'DELETE' });
        if (res.ok) location.reload();
    } catch (e) {
        console.error(e);
    }
}

// Resolve security alert
async function resolveAlert(alertId) {
    // TODO [Sprint 4 - Frontend]: Assigned to Riya
    try {
        const res = await fetch(`/api/alerts/${alertId}/resolve`, { method: 'POST' });
        if (res.ok) location.reload();
    } catch (e) {
        console.error(e);
    }
}
