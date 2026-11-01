package com.hashwatch.controller;

import com.hashwatch.entity.AlertEvent;
import com.hashwatch.entity.AlertSeverity;
import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.EventType;
import com.hashwatch.entity.FileStatus;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * =============================================================================
 * DOMAIN: Frontend & Web MVC Testing
 * ASSIGNED TO: Riya (Sprint 4: S4-T1, S4-T2 / SB-2, SB-3)
 * TARGET: src/test/java/com/hashwatch/controller/DashboardViewControllerTest.java
 * =============================================================================
 *
 * MockMvc unit test suite verifying the DashboardViewController endpoints:
 * <ul>
 *   <li>GET / -> returns "dashboard" view with files, baselines, and health flags</li>
 *   <li>GET /alerts -> returns "alerts" view with triage records and counts</li>
 * </ul>
 */
@WebMvcTest(DashboardViewController.class)
class DashboardViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WatchedFileRepository watchedFileRepository;

    @MockBean
    private BaselineEntryRepository baselineEntryRepository;

    @MockBean
    private AlertEventRepository alertEventRepository;

    @Test
    @DisplayName("GET / - Should render dashboard view with healthy system state")
    void testDashboardHealthy() throws Exception {
        WatchedFile file = new WatchedFile("/var/log/secure.log", 1024L, LocalDateTime.now(), FileStatus.VERIFIED);
        file.setId(1L);
        file.setActive(true);

        when(watchedFileRepository.findAll(any(Sort.class))).thenReturn(List.of(file));
        when(watchedFileRepository.countByActiveTrue()).thenReturn(1L);
        when(watchedFileRepository.findByStatus(FileStatus.TAMPERED)).thenReturn(Collections.emptyList());

        String sha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        BaselineEntry baseline = new BaselineEntry(file, sha256, "sig456ed25519", "pubkey789");
        baseline.setId(1L);
        when(baselineEntryRepository.findByCurrentTrue()).thenReturn(List.of(baseline));

        when(alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc()).thenReturn(Collections.emptyList());
        when(alertEventRepository.countByResolvedFalse()).thenReturn(0L);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("files", "activeCount", "baselines", "unresolvedAlerts", "alertCount", "tamperedCount", "systemHealthy"))
                .andExpect(model().attribute("activeCount", equalTo(1L)))
                .andExpect(model().attribute("alertCount", equalTo(0L)))
                .andExpect(model().attribute("tamperedCount", equalTo(0L)))
                .andExpect(model().attribute("systemHealthy", equalTo(true)))
                .andExpect(model().attribute("files", hasSize(1)))
                .andExpect(model().attribute("baselines", hasSize(1)));
    }

    @Test
    @DisplayName("GET / - Should reflect compromised health when unresolved alerts exist")
    void testDashboardCompromisedHealth() throws Exception {
        WatchedFile file = new WatchedFile("/etc/shadow", 2048L, LocalDateTime.now(), FileStatus.TAMPERED);
        file.setId(2L);
        file.setActive(true);

        when(watchedFileRepository.findAll(any(Sort.class))).thenReturn(List.of(file));
        when(watchedFileRepository.countByActiveTrue()).thenReturn(1L);
        when(watchedFileRepository.findByStatus(FileStatus.TAMPERED)).thenReturn(List.of(file));

        AlertEvent alert = new AlertEvent(file, "/etc/shadow", EventType.MISMATCH, AlertSeverity.CRITICAL, "expectedHash", "actualHash", "Hash mismatch");
        alert.setId(10L);
        when(alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc()).thenReturn(List.of(alert));
        when(alertEventRepository.countByResolvedFalse()).thenReturn(1L);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("systemHealthy", equalTo(false)))
                .andExpect(model().attribute("alertCount", equalTo(1L)))
                .andExpect(model().attribute("tamperedCount", equalTo(1L)));
    }

    @Test
    @DisplayName("GET /alerts - Should render alerts view with triage metrics")
    void testAlertsView() throws Exception {
        WatchedFile f1 = new WatchedFile("/etc/hosts", 512L, LocalDateTime.now(), FileStatus.TAMPERED);
        f1.setId(1L);
        WatchedFile f2 = new WatchedFile("/etc/sudoers", 1024L, LocalDateTime.now(), FileStatus.TAMPERED);
        f2.setId(2L);

        AlertEvent alert1 = new AlertEvent(f1, "/etc/hosts", EventType.MISMATCH, AlertSeverity.HIGH, "exp1", "act1", "Modified");
        alert1.setId(101L);
        AlertEvent alert2 = new AlertEvent(f2, "/etc/sudoers", EventType.UNAUTHORIZED_MODIFICATION, AlertSeverity.CRITICAL, "exp2", "act2", "Tampered");
        alert2.setId(102L);

        when(alertEventRepository.findAll(any(Sort.class))).thenReturn(List.of(alert2, alert1));
        when(alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc()).thenReturn(List.of(alert2, alert1));
        when(alertEventRepository.countByResolvedFalse()).thenReturn(2L);
        when(alertEventRepository.findBySeverity(AlertSeverity.CRITICAL)).thenReturn(List.of(alert2));

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("alerts"))
                .andExpect(model().attributeExists("alerts", "unresolvedAlerts", "alertCount", "criticalCount"))
                .andExpect(model().attribute("alerts", hasSize(2)))
                .andExpect(model().attribute("alertCount", equalTo(2L)))
                .andExpect(model().attribute("criticalCount", equalTo(1L)));
    }

    @Test
    @DisplayName("GET /alerts - Should render empty state when no alerts exist")
    void testAlertsViewEmpty() throws Exception {
        when(alertEventRepository.findAll(any(Sort.class))).thenReturn(Collections.emptyList());
        when(alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc()).thenReturn(Collections.emptyList());
        when(alertEventRepository.countByResolvedFalse()).thenReturn(0L);
        when(alertEventRepository.findBySeverity(AlertSeverity.CRITICAL)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("alerts"))
                .andExpect(model().attribute("alerts", hasSize(0)))
                .andExpect(model().attribute("alertCount", equalTo(0L)))
                .andExpect(model().attribute("criticalCount", equalTo(0L)));
    }
}
