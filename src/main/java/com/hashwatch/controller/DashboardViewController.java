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
 * DOMAIN: Frontend & Cockpit UI
 * ASSIGNED TO: Samarjeet / Riya / Priyanshu (Sprint B: SB-1)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/DashboardViewController.java
 * DOC TO UPDATE: docs/PACKAGE_STRUCTURE.md & docs/PRD.md
 * =============================================================================
 *
 * Spring MVC Controller that populates real-time system metrics, file registries,
 * active baselines, and unresolved security incidents into the Thymeleaf Model.
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
     * GET / - Render main monitoring dashboard cockpit.
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("files", watchedFileRepository.findAll());
        model.addAttribute("activeCount", watchedFileRepository.countByActiveTrue());
        model.addAttribute("baselines", baselineEntryRepository.findByCurrentTrue());
        model.addAttribute("alertCount", alertEventRepository.countByResolvedFalse());
        model.addAttribute("tamperedCount", watchedFileRepository.countByStatus(FileStatus.TAMPERED));
        model.addAttribute("missingCount", watchedFileRepository.countByStatus(FileStatus.MISSING));
        model.addAttribute("keyFingerprint", signingService.getKeyFingerprint());
        return "dashboard";
    }

    /**
     * GET /alerts - Render dedicated security incident triage log.
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
