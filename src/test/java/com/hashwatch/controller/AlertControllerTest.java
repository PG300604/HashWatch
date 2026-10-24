package com.hashwatch.controller;

import com.hashwatch.entity.AlertEvent;
import com.hashwatch.entity.AlertSeverity;
import com.hashwatch.entity.EventType;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.service.ComparisonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * =============================================================================
 * DOMAIN: API & Backend
 * ASSIGNED TO: Priyanshu / Riya (Sprint 3: S3-T2 & SA-4)
 * TARGET: src/test/java/com/hashwatch/controller/AlertControllerTest.java
 * =============================================================================
 *
 * MockMvc integration tests for {@link AlertController} REST endpoints:
 * - Unresolved incident feed (GET /api/alerts)
 * - Recent 50 audit history (GET /api/alerts/all)
 * - Triage alert resolution (POST /api/alerts/{id}/resolve)
 * - On-demand manual scan trigger (POST /api/alerts/scan-now)
 */
@WebMvcTest(AlertController.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertEventRepository alertEventRepository;

    @MockBean
    private ComparisonService comparisonService;

    @Test
    @DisplayName("GET /api/alerts returns 200 OK with list of unresolved alerts")
    void testGetUnresolvedAlerts() throws Exception {
        AlertEvent alert = new AlertEvent(null, "/etc/shadow", EventType.UNAUTHORIZED_MODIFICATION,
                AlertSeverity.CRITICAL, "expected-hash", "actual-hash", "Tamper detected");
        alert.setId(10L);

        when(alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc()).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].filePath").value("/etc/shadow"))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].resolved").value(false));

        verify(alertEventRepository, times(1)).findByResolvedFalseOrderByDetectedAtDesc();
    }

    @Test
    @DisplayName("GET /api/alerts/all returns 200 OK with recent 50 alerts")
    void testGetAllAlerts() throws Exception {
        AlertEvent a1 = new AlertEvent(null, "/etc/f1", EventType.MISMATCH, AlertSeverity.HIGH, "h1", "h2", "msg1");
        a1.setId(1L);
        AlertEvent a2 = new AlertEvent(null, "/etc/f2", EventType.MISSING_FILE, AlertSeverity.MEDIUM, "h3", null, "msg2");
        a2.setId(2L);

        when(alertEventRepository.findTop50ByOrderByDetectedAtDesc()).thenReturn(List.of(a1, a2));

        mockMvc.perform(get("/api/alerts/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(alertEventRepository, times(1)).findTop50ByOrderByDetectedAtDesc();
    }

    @Test
    @DisplayName("POST /api/alerts/{id}/resolve marks alert resolved and returns 200 OK")
    void testResolveAlertSuccess() throws Exception {
        AlertEvent alert = new AlertEvent(null, "/etc/passwd", EventType.MISMATCH, AlertSeverity.HIGH, "exp", "act", "msg");
        alert.setId(5L);
        alert.setResolved(false);

        when(alertEventRepository.findById(5L)).thenReturn(Optional.of(alert));
        when(alertEventRepository.save(any(AlertEvent.class))).thenReturn(alert);

        mockMvc.perform(post("/api/alerts/5/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertId").value(5))
                .andExpect(jsonPath("$.resolved").value(true));

        verify(alertEventRepository, times(1)).save(alert);
    }

    @Test
    @DisplayName("POST /api/alerts/{id}/resolve returns 404 Not Found when alert does not exist")
    void testResolveAlertNotFound() throws Exception {
        when(alertEventRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/alerts/999/resolve"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("POST /api/alerts/scan-now triggers immediate verification scan and returns 200 OK")
    void testTriggerManualScan() throws Exception {
        doNothing().when(comparisonService).runVerificationScan();

        mockMvc.perform(post("/api/alerts/scan-now"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(comparisonService, times(1)).runVerificationScan();
    }
}
