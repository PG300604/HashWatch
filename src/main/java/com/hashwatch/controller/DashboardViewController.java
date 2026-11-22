package com.hashwatch.controller;

import com.hashwatch.entity.FileStatus;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.SigningService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * =============================================================================
 * DOMAIN: Frontend & Backend
 * ASSIGNED TO: Samarjeet (Sprint 4: S4-T3 / SB-1)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/DashboardViewController.java
 * DOC TO UPDATE: docs/PACKAGE_STRUCTURE.md & docs/SPRINT_PLAN.md
 * =============================================================================
 *
 * MVC Controller that populates the Model with live database entities and stats
 * to render dashboard.html and alerts.html Thymeleaf templates.
 */
@Controller
public class DashboardViewController {

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
     * GET / - Render main monitoring dashboard with high-density KPI metrics,
     * watched file inventory, and cryptographic identity.
     */
    @GetMapping("/")
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
        } catch (Exception ignored) {
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
        model.addAttribute("alerts", alertEventRepository.findTop50ByOrderByDetectedAtDesc());
        model.addAttribute("unresolvedAlerts", alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc());
        model.addAttribute("alertCount", alertEventRepository.countByResolvedFalse());
        model.addAttribute("activeCount", watchedFileRepository.countByActiveTrue());
        return "alerts";
    }
}
