/**
 * =============================================================================
 * DOMAIN: Frontend & API Client
 * CO-AUTHORED BY: Group 3 (Riya, Samarjeet & Priyanshu)
 * FOLDER / TARGET: src/main/resources/static/js/app.js
 * DESIGN SYSTEM: Obsidian & Electric Vermilion
 * =============================================================================
 *
 * Client-side asynchronous interaction handling: triggers verification scans,
 * submits new file registration, requests re-baselining, resolves alerts,
 * provides toast feedback, and supports alert filtering without page refresh.
 */

// Toast notification helper with sleek SVG iconography
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
    
    let iconSvg = '';
    if (type === 'success') {
        iconSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
    } else if (type === 'error') {
        iconSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';
    } else {
        iconSvg = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>';
    }

    toast.innerHTML = iconSvg;
    const msgSpan = document.createElement('span');
    msgSpan.textContent = message;
    toast.appendChild(msgSpan);
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
    const originalContent = btn ? btn.innerHTML : '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = `
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="spin">
                <line x1="12" y1="2" x2="12" y2="6"/>
                <line x1="12" y1="18" x2="12" y2="22"/>
                <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"/>
                <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"/>
                <line x1="2" y1="12" x2="6" y2="12"/>
                <line x1="18" y1="12" x2="22" y2="12"/>
                <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"/>
                <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"/>
            </svg>
            Scanning...`;
    }

    try {
        const res = await fetch('/api/alerts/scan-now', { method: 'POST' });
        if (res.ok) {
            showToast('Integrity verification scan completed successfully.', 'success');
            setTimeout(() => location.reload(), 600);
        } else {
            showToast('Integrity scan trigger failed. Check server logs.', 'error');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = originalContent;
            }
        }
    } catch (e) {
        console.error('Scan error:', e);
        showToast('Network error while requesting scan execution.', 'error');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = originalContent;
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
            showToast(`Baseline for File #${fileId} updated and cryptographically signed.`, 'success');
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
