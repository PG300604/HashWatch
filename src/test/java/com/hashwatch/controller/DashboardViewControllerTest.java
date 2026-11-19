package com.hashwatch.controller;

import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class DashboardViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WatchedFileRepository watchedFileRepository;

    @Autowired
    private AlertEventRepository alertEventRepository;

    @Autowired
    private BaselineEntryRepository baselineEntryRepository;

    @Test
    @DisplayName("GET / - Should render dashboard.html with model attributes")
    void testDashboardView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("files"))
                .andExpect(model().attributeExists("activeCount"))
                .andExpect(model().attributeExists("baselines"))
                .andExpect(model().attributeExists("alertCount"))
                .andExpect(model().attributeExists("tamperedCount"))
                .andExpect(model().attributeExists("missingCount"))
                .andExpect(model().attributeExists("keyFingerprint"));
    }

    @Test
    @DisplayName("GET /alerts - Should render alerts.html with alert history")
    void testAlertsView() throws Exception {
        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("alerts"))
                .andExpect(model().attributeExists("alerts"))
                .andExpect(model().attributeExists("unresolvedAlerts"))
                .andExpect(model().attributeExists("alertCount"))
                .andExpect(model().attributeExists("activeCount"));
    }
}
