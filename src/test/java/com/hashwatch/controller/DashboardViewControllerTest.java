package com.hashwatch.controller;

import com.hashwatch.entity.*;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.SigningService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * =============================================================================
 * DOMAIN: Frontend & Backend
 * ASSIGNED TO: Samarjeet (Sprint 4: S4-T3 / SB-1)
 * TARGET: src/test/java/com/hashwatch/controller/DashboardViewControllerTest.java
 * =============================================================================
 *
 * MockMvc integration tests for {@link DashboardViewController}:
 * - GET / (dashboard view, KPI counts, model attribute bindings, key fingerprint)
 * - GET /alerts (alerts incident view, historical log, unresolved alert counts)
 * - Optimized SQL count queries (countByStatus, countByActiveTrue)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
public class DashboardViewControllerTest {

    private static final String DUMMY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WatchedFileRepository watchedFileRepository;

    @Autowired
    private AlertEventRepository alertEventRepository;

    @Autowired
    private BaselineEntryRepository baselineEntryRepository;

    @Autowired
    private SigningService signingService;

    @Test
    @DisplayName("GET / - Should render dashboard.html with empty/default model attributes")
    void testDashboardView_EmptyDatabase() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("files"))
                .andExpect(model().attributeExists("activeCount"))
                .andExpect(model().attributeExists("baselines"))
                .andExpect(model().attributeExists("alertCount"))
                .andExpect(model().attributeExists("unresolvedAlerts"))
                .andExpect(model().attributeExists("tamperedCount"))
                .andExpect(model().attributeExists("missingCount"))
                .andExpect(model().attributeExists("keyFingerprint"))
                .andExpect(model().attribute("keyFingerprint", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("GET / - Should correctly bind populated database entities and KPI counts")
    void testDashboardView_WithPopulatedData() throws Exception {
        // Seed 1 active & verified file
        WatchedFile file1 = new WatchedFile("/var/log/syslog", 1024L, LocalDateTime.now(), FileStatus.VERIFIED);
        watchedFileRepository.save(file1);

        // Seed 1 tampered file
        WatchedFile file2 = new WatchedFile("/etc/shadow", 2048L, LocalDateTime.now(), FileStatus.TAMPERED);
        watchedFileRepository.save(file2);

        // Seed 1 missing file (inactive)
        WatchedFile file3 = new WatchedFile("/opt/app.conf", 512L, LocalDateTime.now(), FileStatus.MISSING);
        file3.setActive(false);
        watchedFileRepository.save(file3);

        // Seed current baseline
        BaselineEntry baseline = new BaselineEntry(file1, DUMMY_SHA256, "sig1", "pubkey1");
        baseline.setCurrent(true);
        baselineEntryRepository.save(baseline);

        // Seed 1 unresolved alert and 1 resolved alert
        AlertEvent alert1 = new AlertEvent(file2, "/etc/shadow", EventType.UNAUTHORIZED_MODIFICATION,
                AlertSeverity.CRITICAL, "hashA", "hashB", "Tamper detected");
        alert1.setResolved(false);
        alertEventRepository.save(alert1);

        AlertEvent alert2 = new AlertEvent(file1, "/var/log/syslog", EventType.MISMATCH,
                AlertSeverity.LOW, "hashC", "hashD", "Resolved mismatch");
        alert2.setResolved(true);
        alertEventRepository.save(alert2);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("activeCount", 2L))
                .andExpect(model().attribute("tamperedCount", 1L))
                .andExpect(model().attribute("missingCount", 1L))
                .andExpect(model().attribute("alertCount", 1L))
                .andExpect(model().attribute("files", hasSize(3)))
                .andExpect(model().attribute("baselines", hasSize(1)));
    }

    @Test
    @DisplayName("GET /alerts - Should render alerts.html with empty alert log")
    void testAlertsView_EmptyDatabase() throws Exception {
        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("alerts"))
                .andExpect(model().attributeExists("alerts"))
                .andExpect(model().attributeExists("unresolvedAlerts"))
                .andExpect(model().attributeExists("alertCount"))
                .andExpect(model().attributeExists("activeCount"))
                .andExpect(model().attribute("alertCount", 0L))
                .andExpect(model().attribute("activeCount", 0L));
    }

    @Test
    @DisplayName("GET /alerts - Should bind historical and unresolved alerts to model")
    void testAlertsView_WithPopulatedAlerts() throws Exception {
        WatchedFile file = new WatchedFile("/etc/passwd", 1024L, LocalDateTime.now(), FileStatus.VERIFIED);
        watchedFileRepository.save(file);

        AlertEvent alert1 = new AlertEvent(file, "/etc/passwd", EventType.UNAUTHORIZED_MODIFICATION,
                AlertSeverity.CRITICAL, "expected1", "actual1", "Critical hash change");
        alert1.setResolved(false);
        alertEventRepository.save(alert1);

        AlertEvent alert2 = new AlertEvent(file, "/etc/passwd", EventType.MISSING_FILE,
                AlertSeverity.HIGH, "expected2", "NONE", "File removed");
        alert2.setResolved(false);
        alertEventRepository.save(alert2);

        AlertEvent alert3 = new AlertEvent(file, "/etc/passwd", EventType.MISMATCH,
                AlertSeverity.LOW, "exp3", "act3", "Previous resolved incident");
        alert3.setResolved(true);
        alertEventRepository.save(alert3);

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("alerts"))
                .andExpect(model().attribute("alertCount", 2L))
                .andExpect(model().attribute("activeCount", 1L))
                .andExpect(model().attribute("alerts", hasSize(3)))
                .andExpect(model().attribute("unresolvedAlerts", hasSize(2)));
    }

    @Test
    @DisplayName("DBMS: countByStatus should return exact count without loading entities into heap")
    void testCountByStatusDirectQuery() {
        WatchedFile f1 = new WatchedFile("/a", 100L, LocalDateTime.now(), FileStatus.TAMPERED);
        watchedFileRepository.save(f1);

        WatchedFile f2 = new WatchedFile("/b", 200L, LocalDateTime.now(), FileStatus.TAMPERED);
        watchedFileRepository.save(f2);

        WatchedFile f3 = new WatchedFile("/c", 300L, LocalDateTime.now(), FileStatus.VERIFIED);
        watchedFileRepository.save(f3);

        assertEquals(2L, watchedFileRepository.countByStatus(FileStatus.TAMPERED));
        assertEquals(1L, watchedFileRepository.countByStatus(FileStatus.VERIFIED));
        assertEquals(0L, watchedFileRepository.countByStatus(FileStatus.MISSING));
    }
}
