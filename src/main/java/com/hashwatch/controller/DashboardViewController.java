package com.hashwatch.controller;

import com.hashwatch.entity.AlertEvent;
import com.hashwatch.entity.AlertSeverity;
import com.hashwatch.entity.FileStatus;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.SigningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * =============================================================================
 * DOMAIN: Frontend & Backend
 * ASSIGNED TO: Samarjeet / Priyanshu (Sprint 4: S4-T3 / SB-1)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/DashboardViewController.java
 * DOC TO UPDATE: docs/PACKAGE_STRUCTURE.md & docs/PRD.md
 * =============================================================================
 *
 * MVC Controller that populates the Model with live database entities and stats
 * to render dashboard.html and alerts.html Thymeleaf templates.
 */
@Controller
public class DashboardViewController {

    private static final Logger log = LoggerFactory.getLogger(DashboardViewController.class);

    private final WatchedFileRepository watchedFileRepository;
    private final BaselineEntryRepository baselineEntryRepository;
    private final AlertEventRepository alertEventRepository;
    private final SigningService signingService;

    public DashboardViewController(WatchedFileRepository watchedFileRepository,
                                   BaselineEntryRepository baselineEntryRepository,
                                   AlertEventRepository alertEventRepository,
                                   SigningService signingService) {
        this.watchedFileRepository = watchedFileRepository;
        this.baselineEntryRepository = baselineEntryRepository;
        this.alertEventRepository = alertEventRepository;
        this.signingService = signingService;
    }

    /**
     * GET / and /dashboard - Render main monitoring dashboard with high-density KPI metrics,
     * watched file inventory, and cryptographic identity.
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("files", watchedFileRepository.findAll());
        model.addAttribute("activeCount", watchedFileRepository.countByActiveTrue());
        model.addAttribute("baselines", baselineEntryRepository.findByCurrentTrue());
        model.addAttribute("alertCount", alertEventRepository.countByResolvedFalse());
        model.addAttribute("unresolvedAlerts", alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc());
        model.addAttribute("tamperedCount", watchedFileRepository.countByStatus(FileStatus.TAMPERED));
        model.addAttribute("missingCount", watchedFileRepository.countByStatus(FileStatus.MISSING));

        String fingerprint = "UNKNOWN";
        try {
            if (signingService != null) {
                fingerprint = signingService.getKeyFingerprint();
            }
        } catch (Exception e) {
            log.warn("Unable to obtain Ed25519 key fingerprint for dashboard display: {}", e.getMessage());
            fingerprint = "UNAVAILABLE";
        }
        model.addAttribute("keyFingerprint", fingerprint);

        return "dashboard";
    }

    /**
     * GET /alerts - Render dedicated security incident triage log with
     * severity filters, event types, and resolution actions.
     */
    @GetMapping("/alerts")
    public String alerts(Model model) {
        List<AlertEvent> unresolvedAlerts = alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc();
        List<AlertEvent> recentAlerts = alertEventRepository.findTop50ByOrderByDetectedAtDesc();

        // Combine all unresolved alerts with recent historical events (deduplicated)
        // so operators can triage every pending alert without pagination cutoff.
        Set<AlertEvent> combinedAlerts = new LinkedHashSet<>(unresolvedAlerts);
        combinedAlerts.addAll(recentAlerts);

        long totalAlerts = alertEventRepository.count();
        long unresolvedCount = alertEventRepository.countByResolvedFalse();
        long resolvedCount = Math.max(0, totalAlerts - unresolvedCount);
        long criticalCount = alertEventRepository.countBySeverity(AlertSeverity.CRITICAL);

        model.addAttribute("alerts", new ArrayList<>(combinedAlerts));
        model.addAttribute("unresolvedAlerts", unresolvedAlerts);
        model.addAttribute("alertCount", unresolvedCount);
        model.addAttribute("totalAlertCount", totalAlerts);
        model.addAttribute("resolvedAlertCount", resolvedCount);
        model.addAttribute("criticalCount", criticalCount);
        model.addAttribute("activeCount", watchedFileRepository.countByActiveTrue());
        return "alerts";
    }
}
