package com.hashwatch.controller;

import com.hashwatch.entity.AlertEvent;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.service.ComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertEventRepository alertEventRepository;
    private final ComparisonService comparisonService;

    public AlertController(AlertEventRepository alertEventRepository, ComparisonService comparisonService) {
        this.alertEventRepository = alertEventRepository;
        this.comparisonService = comparisonService;
    }

    @GetMapping
    public List<AlertEvent> getUnresolvedAlerts() {
        return alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc();
    }

    @GetMapping("/all")
    public List<AlertEvent> getAllAlerts() {
        return alertEventRepository.findTop50ByOrderByDetectedAtDesc();
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolveAlert(@PathVariable Long id) {
        return alertEventRepository.findById(id).map(alert -> {
            alert.setResolved(true);
            alertEventRepository.save(alert);
            return ResponseEntity.ok(Map.of("message", "Alert resolved"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/scan-now")
    public ResponseEntity<?> triggerManualScan() {
        comparisonService.runVerificationScan();
        return ResponseEntity.ok(Map.of("message", "Manual integrity verification scan completed"));
    }
}
