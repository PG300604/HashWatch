package com.hashwatch.controller;

import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("files", watchedFileRepository.findAll());
        model.addAttribute("activeCount", watchedFileRepository.findByActiveTrue().size());
        model.addAttribute("baselines", baselineEntryRepository.findByCurrentTrue());
        model.addAttribute("unresolvedAlerts", alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc());
        model.addAttribute("alertCount", alertEventRepository.countByResolvedFalse());
        return "dashboard";
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("alerts", alertEventRepository.findTop50ByOrderByDetectedAtDesc());
        model.addAttribute("alertCount", alertEventRepository.countByResolvedFalse());
        return "alerts";
    }
}
