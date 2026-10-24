package com.hashwatch.controller;

import com.hashwatch.entity.AlertEvent;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.service.ComparisonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 * DOMAIN: API & Backend
 * ASSIGNED TO: Priyanshu / Riya (Sprint 3: S3-T2 & SA-4)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/AlertController.java
 * DOC TO UPDATE: docs/TRD.md (Section 5.3 Alerts API)
 * =============================================================================
 *
 * RESTful controller exposing endpoints for incident triage, retrieving
 * unresolved security alerts, marking alerts resolved, and triggering
 * immediate out-of-band integrity verification scans.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private static final Logger log = LoggerFactory.getLogger(AlertController.class);

    private final AlertEventRepository alertEventRepository;
    private final ComparisonService comparisonService;

    public AlertController(AlertEventRepository alertEventRepository, ComparisonService comparisonService) {
        this.alertEventRepository = alertEventRepository;
        this.comparisonService = comparisonService;
    }

    /**
     * GET /api/alerts - List all currently unresolved security alerts, newest first.
     *
     * @return list of unresolved AlertEvent entities
     */
    @GetMapping
    public List<AlertEvent> getUnresolvedAlerts() {
        return alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc();
    }

    /**
     * GET /api/alerts/all - List the 50 most recent security alerts regardless of resolution state.
     *
     * @return list of recent AlertEvent entities
     */
    @GetMapping("/all")
    public List<AlertEvent> getAllAlerts() {
        return alertEventRepository.findTop50ByOrderByDetectedAtDesc();
    }

    /**
     * POST /api/alerts/{id}/resolve - Mark a security alert as investigated and resolved.
     *
     * @param id the primary key of the AlertEvent
     * @return 200 OK with confirmation, or 404 Not Found
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolveAlert(@PathVariable Long id) {
        return alertEventRepository.findById(id)
                .map(alert -> {
                    alert.setResolved(true);
                    alertEventRepository.save(alert);
                    log.info("Resolved alert ID {} for file: {}", id, alert.getFilePath());
                    return ResponseEntity.ok(Map.of(
                            "message", "Alert successfully marked as resolved",
                            "alertId", id,
                            "resolved", true
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", HttpStatus.NOT_FOUND.value(),
                        "error", "Not Found",
                        "message", "AlertEvent with ID " + id + " not found"
                )));
    }

    /**
     * POST /api/alerts/scan-now - Trigger an immediate out-of-band file integrity verification scan.
     *
     * @return 200 OK with scan summary
     */
    @PostMapping("/scan-now")
    public ResponseEntity<?> triggerManualScan() {
        log.info("Manual out-of-band integrity verification scan triggered via REST API");
        comparisonService.runVerificationScan();
        return ResponseEntity.ok(Map.of(
                "message", "Manual integrity verification scan executed successfully",
                "status", "COMPLETED"
        ));
    }
}
