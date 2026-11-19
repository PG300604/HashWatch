package com.hashwatch.controller;

import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;

/**
 * =============================================================================
 * DOMAIN: Frontend & Backend
 * ASSIGNED TO: Samarjeet / Riya (Sprint 4)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/DashboardViewController.java
 * DOC TO UPDATE: docs/PACKAGE_STRUCTURE.md & docs/PRD.md
 * =============================================================================
 *
 * Task Description:
 * MVC Controller that populates the Model with live database entities and stats
 * to render dashboard.html and alerts.html Thymeleaf templates.
 */
@Controller
public class DashboardViewController {

    private final WatchedFileRepository watchedFileRepository;
    private final BaselineEntryRepository baselineEntryRepository;
    private final AlertEventRepository alertEventRepository;

    public DashboardViewController(WatchedFileRepository watchedFileRepository,
                                   BaselineEntryRepository baselineEntryRepository,
                                   AlertEventRepository alertEventRepository) {
        this.watchedFileRepository = watchedFileRepository;
        this.baselineEntryRepository = baselineEntryRepository;
        this.alertEventRepository = alertEventRepository;
    }

    /**
     * GET / - Render main monitoring dashboard.
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        // TODO [Sprint 4 - Frontend & Backend]: Assigned to Samarjeet / Riya
        // Populate model attributes:
        // 1. "files" -> watchedFileRepository.findAll()
        // 2. "activeCount" -> count of active watched files
        // 3. "baselines" -> baselineEntryRepository.findByCurrentTrue()
        // 4. "unresolvedAlerts" -> alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc()
        // 5. "alertCount" -> count of unresolved alerts
        model.addAttribute("files", Collections.emptyList());
        model.addAttribute("activeCount", 0);
        model.addAttribute("baselines", Collections.emptyList());
        model.addAttribute("alertCount", 0);
        return "dashboard";
    }

    /**
     * GET /alerts - Render dedicated alert history log.
     */
    @GetMapping("/alerts")
    public String alerts(Model model) {
        // TODO [Sprint 4 - Frontend & Backend]: Assigned to Samarjeet / Riya
        model.addAttribute("alerts", Collections.emptyList());
        model.addAttribute("alertCount", 0);
        return "alerts";
    }
}
