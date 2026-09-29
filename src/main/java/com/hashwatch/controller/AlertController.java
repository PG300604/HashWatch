package com.hashwatch.controller;

import com.hashwatch.entity.AlertEvent;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.service.ComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 * DOMAIN: API & Backend
 * ASSIGNED TO: Priyanshu / Riya (Sprint 3)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/AlertController.java
 * DOC TO UPDATE: docs/TRD.md (Section 5.3 Alerts API)
 * =============================================================================
 *
 * Task Description:
 * REST endpoints for retrieving unresolved alerts, security event history,
 * resolving alerts, and triggering immediate manual verification scans.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertEventRepository alertEventRepository;
    private final ComparisonService comparisonService;

    public AlertController(AlertEventRepository alertEventRepository, ComparisonService comparisonService) {
        this.alertEventRepository = alertEventRepository;
        this.comparisonService = comparisonService;
    }

    /**
     * GET /api/alerts - List all unresolved alerts.
     */
    @GetMapping
    public List<AlertEvent> getUnresolvedAlerts() {
        // TODO [Sprint 3 - API]: Assigned to Priyanshu / Riya
        return alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc();
    }

    /**
     * GET /api/alerts/all - List recent 50 alerts.
     */
    @GetMapping("/all")
    public List<AlertEvent> getAllAlerts() {
        // TODO [Sprint 3 - API]: Assigned to Priyanshu / Riya
        return alertEventRepository.findTop50ByOrderByDetectedAtDesc();
    }

    /**
     * POST /api/alerts/{id}/resolve - Mark an alert as investigated and resolved.
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolveAlert(@PathVariable Long id) {
        // TODO [Sprint 3 - API]: Assigned to Priyanshu / Riya
        return ResponseEntity.ok(Map.of("message", "TODO: Implement resolveAlert in Sprint 3", "id", id));
    }

    /**
     * POST /api/alerts/scan-now - Trigger an out-of-band integrity check.
     */
    @PostMapping("/scan-now")
    public ResponseEntity<?> triggerManualScan() {
        // TODO [Sprint 3 - API & Backend]: Assigned to Priyanshu / Riya
        comparisonService.runVerificationScan();
        return ResponseEntity.ok(Map.of("message", "Triggered manual verification scan"));
    }
}
