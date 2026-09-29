async function triggerScan() {
    try {
        const res = await fetch('/api/alerts/scan-now', { method: 'POST' });
        if (res.ok) {
            alert('Integrity scan executed successfully.');
            location.reload();
        } else {
            alert('Failed to trigger scan.');
        }
    } catch (e) {
        console.error(e);
        alert('Error triggering scan: ' + e.message);
    }
}

async function registerFile(event) {
    event.preventDefault();
    const filePath = document.getElementById('filePathInput').value.trim();
    if (!filePath) return;

    try {
        const res = await fetch('/api/files', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ filePath })
        });
        const data = await res.json();
        if (res.ok) {
            alert('File registered and baseline established: ' + filePath);
            location.reload();
        } else {
            alert('Error: ' + (data.error || 'Failed to register file'));
        }
    } catch (e) {
        console.error(e);
        alert('Request failed: ' + e.message);
    }
}

async function rebaselineSingle(fileId) {
    if (!confirm('Are you sure you want to regenerate the cryptographic baseline for this file?')) return;
    try {
        const res = await fetch(`/api/baselines/generate/${fileId}`, { method: 'POST' });
        if (res.ok) {
            alert('Baseline refreshed and signed.');
            location.reload();
        } else {
            alert('Failed to rebaseline file.');
        }
    } catch (e) {
        console.error(e);
        alert('Request failed: ' + e.message);
    }
}

async function rebaselineAll() {
    if (!confirm('Regenerate and sign baselines for all active files?')) return;
    try {
        const res = await fetch('/api/baselines/generate-all', { method: 'POST' });
        if (res.ok) {
            alert('All active baselines updated.');
            location.reload();
        } else {
            alert('Failed to update baselines.');
        }
    } catch (e) {
        console.error(e);
        alert('Request failed: ' + e.message);
    }
}

async function removeFile(fileId) {
    if (!confirm('Stop monitoring this file?')) return;
    try {
        const res = await fetch(`/api/files/${fileId}`, { method: 'DELETE' });
        if (res.ok) {
            location.reload();
        } else {
            alert('Failed to remove file.');
        }
    } catch (e) {
        console.error(e);
        alert('Request failed: ' + e.message);
    }
}

async function resolveAlert(alertId) {
    try {
        const res = await fetch(`/api/alerts/${alertId}/resolve`, { method: 'POST' });
        if (res.ok) {
            location.reload();
        } else {
            alert('Failed to resolve alert.');
        }
    } catch (e) {
        console.error(e);
        alert('Request failed: ' + e.message);
    }
}
