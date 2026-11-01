package com.hashwatch.controller;

import com.hashwatch.entity.AlertEvent;
import com.hashwatch.entity.AlertSeverity;
import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.FileStatus;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * =============================================================================
 * DOMAIN: Frontend & Backend
 * ASSIGNED TO: Riya (Sprint 4: S4-T1, S4-T2 / SB-1, SB-2)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/DashboardViewController.java
 * DOC TO UPDATE: docs/PRD.md (Section 4 FR5 & FR6)
 * =============================================================================
 *
 * Spring MVC controller that populates the UI model with live database entities,
 * aggregated health metrics, active baselines, and unresolved incident logs
 * to render the responsive Thymeleaf dashboard and alert triage views.
 *
 * <p>Routes:</p>
 * <ul>
 *   <li><b>GET /:</b> Live operator cockpit (dashboard.html)</li>
 *   <li><b>GET /alerts:</b> Security incident triage center (alerts.html)</li>
 * </ul>
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
     * GET / - Renders the main monitoring dashboard.
     *
     * <p>Populates model attributes:</p>
     * <ul>
     *   <li><code>files</code>: All registered WatchedFile entities</li>
     *   <li><code>activeCount</code>: Total active monitored files</li>
     *   <li><code>baselines</code>: All current cryptographic BaselineEntry records</li>
     *   <li><code>unresolvedAlerts</code>: Active alerts requiring investigation</li>
     *   <li><code>alertCount</code>: Number of unresolved alerts</li>
     *   <li><code>tamperedCount</code>: Number of files currently marked TAMPERED</li>
     *   <li><code>systemHealthy</code>: Boolean flag indicating full system integrity</li>
     * </ul>
     *
     * @param model Spring UI Model
     * @return "dashboard" Thymeleaf template view name
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        List<WatchedFile> files = watchedFileRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        long activeCount = watchedFileRepository.countByActiveTrue();
        List<BaselineEntry> baselines = baselineEntryRepository.findByCurrentTrue();
        List<AlertEvent> unresolvedAlerts = alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc();
        long alertCount = alertEventRepository.countByResolvedFalse();
        long tamperedCount = watchedFileRepository.findByStatus(FileStatus.TAMPERED).size();
        boolean systemHealthy = (alertCount == 0 && tamperedCount == 0);

        model.addAttribute("files", files);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("baselines", baselines);
        model.addAttribute("unresolvedAlerts", unresolvedAlerts);
        model.addAttribute("alertCount", alertCount);
        model.addAttribute("tamperedCount", tamperedCount);
        model.addAttribute("systemHealthy", systemHealthy);

        return "dashboard";
    }

    /**
     * GET /alerts - Renders the dedicated incident triage and security alert log.
     *
     * <p>Populates model attributes:</p>
     * <ul>
     *   <li><code>alerts</code>: All recorded alert events, newest first</li>
     *   <li><code>unresolvedAlerts</code>: Unresolved security alerts</li>
     *   <li><code>alertCount</code>: Count of unresolved alerts</li>
     *   <li><code>criticalCount</code>: Count of CRITICAL severity alerts</li>
     * </ul>
     *
     * @param model Spring UI Model
     * @return "alerts" Thymeleaf template view name
     */
    @GetMapping("/alerts")
    public String alerts(Model model) {
        List<AlertEvent> alerts = alertEventRepository.findAll(Sort.by(Sort.Direction.DESC, "detectedAt"));
        List<AlertEvent> unresolvedAlerts = alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc();
        long alertCount = alertEventRepository.countByResolvedFalse();
        long criticalCount = alertEventRepository.findBySeverity(AlertSeverity.CRITICAL).size();

        model.addAttribute("alerts", alerts);
        model.addAttribute("unresolvedAlerts", unresolvedAlerts);
        model.addAttribute("alertCount", alertCount);
        model.addAttribute("criticalCount", criticalCount);

        return "alerts";
    }
}
