package com.hashwatch.controller;

import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.FileStatus;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.ComparisonService;
import com.hashwatch.service.SigningService;
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
 * DOMAIN: API & Cryptology
 * ASSIGNED TO: Riya (Sprint 3: S3-T1)
 * TARGET: src/test/java/com/hashwatch/controller/BaselineControllerTest.java
 * =============================================================================
 *
 * MockMvc integration tests for {@link BaselineController} REST endpoints:
 * - Active baseline inventory (GET /api/baselines)
 * - Ed25519 public key & fingerprint retrieval (GET /api/baselines/public-key)
 * - Single file re-baseline trigger (POST /api/baselines/generate/{fileId})
 * - Batch re-baseline across all active files (POST /api/baselines/generate-all)
 */
@WebMvcTest(BaselineController.class)
class BaselineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BaselineEntryRepository baselineEntryRepository;

    @MockBean
    private WatchedFileRepository watchedFileRepository;

    @MockBean
    private ComparisonService comparisonService;

    @MockBean
    private SigningService signingService;

    private static final String SAMPLE_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String SAMPLE_SIG = "dGVzdC1lZDI1NTE5LXNpZ25hdHVyZQ==";
    private static final String SAMPLE_PUBKEY = "MCowBQYDK2VwAyEA0123456789abcdef0123456789abcdef0123456789a=";
    private static final String SAMPLE_FINGERPRINT = "a1b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef";

    @Test
    @DisplayName("GET /api/baselines returns 200 OK with list of current baselines")
    void testGetActiveBaselines() throws Exception {
        WatchedFile file = new WatchedFile("/etc/nginx/nginx.conf", 1024L, LocalDateTime.now(), FileStatus.VERIFIED);
        file.setId(1L);

        BaselineEntry baseline = new BaselineEntry(file, SAMPLE_SHA256, SAMPLE_SIG, "key-v1");
        baseline.setId(10L);
        baseline.setCurrent(true);

        when(baselineEntryRepository.findByCurrentTrue()).thenReturn(List.of(baseline));

        mockMvc.perform(get("/api/baselines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sha256Hash").value(SAMPLE_SHA256))
                .andExpect(jsonPath("$[0].current").value(true));

        verify(baselineEntryRepository, times(1)).findByCurrentTrue();
    }

    @Test
    @DisplayName("GET /api/baselines/public-key returns 200 OK with Ed25519 public key and fingerprint")
    void testGetPublicKey() throws Exception {
        when(signingService.getPublicKeyBase64()).thenReturn(SAMPLE_PUBKEY);
        when(signingService.getKeyFingerprint()).thenReturn(SAMPLE_FINGERPRINT);

        mockMvc.perform(get("/api/baselines/public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("Ed25519"))
                .andExpect(jsonPath("$.publicKey").value(SAMPLE_PUBKEY))
                .andExpect(jsonPath("$.fingerprint").value(SAMPLE_FINGERPRINT));
    }

    @Test
    @DisplayName("POST /api/baselines/generate/{fileId} returns 200 OK with regenerated baseline")
    void testRebaselineFileSuccess() throws Exception {
        WatchedFile file = new WatchedFile("/etc/hosts", 512L, LocalDateTime.now(), FileStatus.VERIFIED);
        file.setId(3L);

        BaselineEntry newBaseline = new BaselineEntry(file, SAMPLE_SHA256, SAMPLE_SIG, "key-fingerprint");
        newBaseline.setId(55L);

        when(watchedFileRepository.findById(3L)).thenReturn(Optional.of(file));
        when(comparisonService.establishBaseline(file)).thenReturn(newBaseline);

        mockMvc.perform(post("/api/baselines/generate/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("regenerated and signed successfully")))
                .andExpect(jsonPath("$.baseline.id").value(55))
                .andExpect(jsonPath("$.baseline.sha256Hash").value(SAMPLE_SHA256));

        verify(comparisonService, times(1)).establishBaseline(file);
    }

    @Test
    @DisplayName("POST /api/baselines/generate/{fileId} returns 404 Not Found when fileId does not exist")
    void testRebaselineFileNotFound() throws Exception {
        when(watchedFileRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/baselines/generate/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("POST /api/baselines/generate-all returns 200 OK with batch operation summary")
    void testRebaselineAll() throws Exception {
        WatchedFile f1 = new WatchedFile("/etc/f1.conf", 100L, LocalDateTime.now(), FileStatus.VERIFIED);
        f1.setId(1L);
        WatchedFile f2 = new WatchedFile("/etc/f2.conf", 200L, LocalDateTime.now(), FileStatus.VERIFIED);
        f2.setId(2L);

        when(watchedFileRepository.findByActiveTrue()).thenReturn(List.of(f1, f2));
        when(comparisonService.establishBaseline(any(WatchedFile.class))).thenReturn(new BaselineEntry());

        mockMvc.perform(post("/api/baselines/generate-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActive").value(2))
                .andExpect(jsonPath("$.successfullyBaselined").value(2));

        verify(comparisonService, times(2)).establishBaseline(any(WatchedFile.class));
    }
}
