/**
 * =============================================================================
 * DOMAIN: Frontend & API Client
 * ASSIGNED TO: Riya (Sprint 4: S4-T1, S4-T2 / SB-2, SB-3)
 * FOLDER / TARGET: src/main/resources/static/js/app.js
 * DOC TO UPDATE: docs/PRD.md (Section 4 FR5 & FR6)
 * =============================================================================
 *
 * Client-side asynchronous interaction handling: triggers verification scans,
 * submits new file registration, requests re-baselining, resolves alerts,
 * provides toast feedback, and supports alert filtering.
 */

// Toast notification helper
function showToast(message, type = 'info') {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    const icon = type === 'success' ? '✅' : (type === 'error' ? '❌' : 'ℹ️');
    toast.innerHTML = `<span>${icon}</span><span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

// Trigger manual integrity scan
async function triggerScan() {
    const btn = document.getElementById('scanBtn');
    const originalText = btn ? btn.innerHTML : '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '⏳ Scanning...';
    }

    try {
        const res = await fetch('/api/alerts/scan-now', { method: 'POST' });
        if (res.ok) {
            showToast('Integrity verification scan completed successfully.', 'success');
            setTimeout(() => location.reload(), 600);
        } else {
            showToast('Integrity scan trigger failed. Inspect server logs.', 'error');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalText;
            }
        }
    } catch (e) {
        console.error('Scan error:', e);
        showToast('Network error while requesting scan execution.', 'error');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = originalText;
        }
    }
}

// Register a new file to the watchlist
async function registerFile(event) {
    if (event) event.preventDefault();

    const input = document.getElementById('filePathInput');
    const filePath = input ? input.value.trim() : '';
    if (!filePath) {
        showToast('Please provide a valid file path.', 'error');
        return;
    }

    const submitBtn = document.getElementById('registerBtn');
    if (submitBtn) submitBtn.disabled = true;

    try {
        const res = await fetch('/api/files', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filePath: filePath })
        });

        if (res.ok) {
            showToast(`File "${filePath}" registered and baseline established!`, 'success');
            if (input) input.value = '';
            setTimeout(() => location.reload(), 600);
        } else {
            const errData = await res.json().catch(() => ({}));
            const msg = errData.message || 'File registration failed (check path existence or duplicate).';
            showToast(msg, 'error');
            if (submitBtn) submitBtn.disabled = false;
        }
    } catch (e) {
        console.error('Registration error:', e);
        showToast('Network error during file registration.', 'error');
        if (submitBtn) submitBtn.disabled = false;
    }
}

// Re-baseline a single file
async function rebaselineSingle(fileId) {
    if (!confirm(`Regenerate SHA-256 digest and Ed25519 signature for File ID #${fileId}?`)) {
        return;
    }

    try {
        const res = await fetch(`/api/baselines/generate/${fileId}`, { method: 'POST' });
        if (res.ok) {
            showToast(`Baseline for File #${fileId} updated and signed.`, 'success');
            setTimeout(() => location.reload(), 600);
        } else {
            showToast('Baseline generation failed.', 'error');
        }
    } catch (e) {
        console.error('Rebaseline error:', e);
        showToast('Network error updating baseline.', 'error');
    }
}

// Re-baseline all files
async function rebaselineAll() {
    if (!confirm('Regenerate and sign cryptographic baselines for ALL active watched files?')) {
        return;
    }

    try {
        const res = await fetch('/api/baselines/generate-all', { method: 'POST' });
        if (res.ok) {
            showToast('All active baselines updated and signed.', 'success');
            setTimeout(() => location.reload(), 600);
        } else {
            showToast('Bulk re-baselining failed.', 'error');
        }
    } catch (e) {
        console.error('Bulk rebaseline error:', e);
        showToast('Network error during bulk re-baselining.', 'error');
    }
}

// Remove / Deactivate file monitoring
async function removeFile(fileId) {
    if (!confirm(`Deactivate monitoring for File ID #${fileId}?`)) {
        return;
    }

    try {
        const res = await fetch(`/api/files/${fileId}`, { method: 'DELETE' });
        if (res.ok) {
            showToast(`File #${fileId} monitoring deactivated.`, 'info');
            setTimeout(() => location.reload(), 600);
        } else {
            showToast('Failed to deactivate file monitoring.', 'error');
        }
    } catch (e) {
        console.error('Deactivate error:', e);
        showToast('Network error deactivating file.', 'error');
    }
}

// Resolve security alert
async function resolveAlert(alertId) {
    try {
        const res = await fetch(`/api/alerts/${alertId}/resolve`, { method: 'POST' });
        if (res.ok) {
            showToast(`Security alert #${alertId} marked as resolved.`, 'success');
            setTimeout(() => location.reload(), 600);
        } else {
            showToast('Failed to resolve alert.', 'error');
        }
    } catch (e) {
        console.error('Resolve alert error:', e);
        showToast('Network error resolving alert.', 'error');
    }
}

// Filter alerts by state / severity on /alerts page
function filterAlerts(type, btnElement) {
    const buttons = document.querySelectorAll('.filter-btn');
    buttons.forEach(b => b.classList.remove('active'));
    if (btnElement) btnElement.classList.add('active');

    const rows = document.querySelectorAll('.alert-row');
    rows.forEach(row => {
        const isResolved = row.getAttribute('data-resolved') === 'true';
        const severity = row.getAttribute('data-severity');

        if (type === 'all') {
            row.style.display = '';
        } else if (type === 'unresolved') {
            row.style.display = !isResolved ? '' : 'none';
        } else if (type === 'critical') {
            row.style.display = severity === 'CRITICAL' ? '' : 'none';
        }
    });
}
