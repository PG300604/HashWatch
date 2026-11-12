package com.hashwatch.integration;

import com.hashwatch.entity.AlertEvent;
import com.hashwatch.entity.AlertSeverity;
import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.EventType;
import com.hashwatch.entity.FileStatus;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.ComparisonService;
import com.hashwatch.service.HashingService;
import com.hashwatch.service.SigningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * =============================================================================
 * DOMAIN: Cryptology & API Integration
 * ASSIGNED TO: Riya (Sprint 5: S5-T4)
 * TARGET: src/test/java/com/hashwatch/integration/TamperDetectionIntegrationTest.java
 * DOC TO UPDATE: docs/PRD.md (Testing & Verification)
 * =============================================================================
 *
 * End-to-end integration test suite verifying tamper detection and cryptographic
 * defense mechanisms under live filesystem alterations and database intrusions:
 *
 * 1. Byte-level content tampering injection on monitored disk files.
 * 2. Single-bit flipping sensitivity verification (100% detection rate conforming to PRD).
 * 3. Offline database baseline tampering & forged signature invalidation.
 * 4. Missing / deleted critical file detection.
 * 5. SecOps authorized re-baselining and historical baseline retirement.
 * 6. Alert deduplication across repeated Quartz polling verification passes.
 * 7. Inactive watched file exclusion from background verification scans.
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class TamperDetectionIntegrationTest {

    @Autowired
    private ComparisonService comparisonService;

    @Autowired
    private HashingService hashingService;

    @Autowired
    private SigningService signingService;

    @Autowired
    private WatchedFileRepository watchedFileRepository;

    @Autowired
    private BaselineEntryRepository baselineEntryRepository;

    @Autowired
    private AlertEventRepository alertEventRepository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void cleanState() {
        alertEventRepository.deleteAll();
        baselineEntryRepository.deleteAll();
        watchedFileRepository.deleteAll();
    }

    @Test
    @DisplayName("S5-T4.1: Inject malicious payload into file -> Detect TAMPERED status and emit UNAUTHORIZED_MODIFICATION alert")
    void testEndToEndTamperInjection() throws IOException, GeneralSecurityException {
        // 1. Create a genuine critical system configuration file
        Path filePath = tempDir.resolve("critical_config.conf");
        Files.writeString(filePath, "database_url=jdbc:postgresql://prod-db:5432/main\nenable_ssl=true\n");

        WatchedFile watchedFile = new WatchedFile(
                filePath.toAbsolutePath().toString(),
                Files.size(filePath),
                LocalDateTime.now(),
                FileStatus.UNTRACKED
        );
        watchedFile = watchedFileRepository.save(watchedFile);

        // 2. Establish cryptographic baseline (SHA-256 + Ed25519)
        BaselineEntry baseline = comparisonService.establishBaseline(watchedFile);
        assertNotNull(baseline);
        assertEquals(FileStatus.VERIFIED, watchedFile.getStatus());
        assertTrue(baseline.isCurrent());
        assertEquals(0, alertEventRepository.count());

        // 3. Inject malicious unauthorized content into the file on disk
        Files.writeString(
                filePath,
                "database_url=jdbc:postgresql://attacker-c2-server:5432/backdoor\nenable_ssl=false\n",
                StandardOpenOption.TRUNCATE_EXISTING
        );

        // 4. Run automated integrity verification scan
        comparisonService.runVerificationScan();

        // 5. Assert WatchedFile state transitions to TAMPERED
        WatchedFile refreshedFile = watchedFileRepository.findById(watchedFile.getId()).orElseThrow();
        assertEquals(FileStatus.TAMPERED, refreshedFile.getStatus(),
                "Watched file status must transition to TAMPERED when disk content is modified");

        // 6. Assert AlertEvent persistence with accurate forensic diff
        List<AlertEvent> alerts = alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc();
        assertEquals(1, alerts.size(), "Exactly one unresolved alert must be created for the tampered file");

        AlertEvent alert = alerts.get(0);
        assertEquals(EventType.UNAUTHORIZED_MODIFICATION, alert.getEventType());
        assertEquals(AlertSeverity.HIGH, alert.getSeverity());
        assertEquals(baseline.getSha256Hash(), alert.getExpectedHash(), "Alert must reference original signed baseline digest");
        assertNotNull(alert.getActualHash());
        assertNotEquals(alert.getExpectedHash(), alert.getActualHash(), "Actual hash must differ from baseline hash");
        assertEquals(hashingService.hashFile(filePath.toFile()), alert.getActualHash());
        assertFalse(alert.isResolved());
    }

    @Test
    @DisplayName("S5-T4.2: Single-bit flip sensitivity -> Verify 100% detection rate for minimal 1-bit alteration")
    void testSingleBitFlipDetectionSensitivity() throws IOException, GeneralSecurityException {
        // 1. Create 4 KB binary file
        Path filePath = tempDir.resolve("binary_payload.dat");
        byte[] originalBytes = new byte[4096];
        for (int i = 0; i < originalBytes.length; i++) {
            originalBytes[i] = (byte) (i % 256);
        }
        Files.write(filePath, originalBytes);

        WatchedFile watchedFile = new WatchedFile(
                filePath.toAbsolutePath().toString(),
                (long) originalBytes.length,
                LocalDateTime.now(),
                FileStatus.UNTRACKED
        );
        watchedFile = watchedFileRepository.save(watchedFile);
        comparisonService.establishBaseline(watchedFile);

        // 2. Flip exactly 1 single bit in byte position 2048
        byte[] alteredBytes = Files.readAllBytes(filePath);
        alteredBytes[2048] ^= 0x01; // Invert least significant bit
        Files.write(filePath, alteredBytes);

        // 3. Verify single file
        comparisonService.verifyFile(watchedFile);

        // 4. Assert detection
        WatchedFile refreshed = watchedFileRepository.findById(watchedFile.getId()).orElseThrow();
        assertEquals(FileStatus.TAMPERED, refreshed.getStatus(),
                "Even a single bit flip must trigger TAMPERED state (Avalanche effect in SHA-256)");
        assertEquals(1, alertEventRepository.countByResolvedFalse());
    }

    @Test
    @DisplayName("S5-T4.3: Direct database baseline corruption -> Detect SIGNATURE_INVALID and flag CRITICAL alert")
    void testDatabaseTamperingAndSignatureRejection() throws IOException, GeneralSecurityException {
        // 1. Create and baseline file
        Path filePath = tempDir.resolve("payroll_records.csv");
        Files.writeString(filePath, "emp_id,name,salary\n101,Alice,95000\n102,Bob,82000\n");

        WatchedFile watchedFile = new WatchedFile(
                filePath.toAbsolutePath().toString(),
                Files.size(filePath),
                LocalDateTime.now(),
                FileStatus.UNTRACKED
        );
        watchedFile = watchedFileRepository.save(watchedFile);
        comparisonService.establishBaseline(watchedFile);

        // 2. Simulate attacker tampering directly with PostgreSQL/H2 database record
        // An attacker modifies the baseline digest or substitutes the Ed25519 signature
        BaselineEntry baseline = baselineEntryRepository.findByWatchedFileAndCurrentTrue(watchedFile).orElseThrow();
        baseline.setSha256Hash("0000000000000000000000000000000000000000000000000000000000000000");
        baselineEntryRepository.save(baseline);

        // 3. Run verification
        comparisonService.verifyFile(watchedFile);

        // 4. Assert Zero-Trust defense rejects forged baseline
        WatchedFile refreshed = watchedFileRepository.findById(watchedFile.getId()).orElseThrow();
        assertEquals(FileStatus.SIGNATURE_INVALID, refreshed.getStatus(),
                "File status must be SIGNATURE_INVALID when baseline envelope does not match Ed25519 signature");

        List<AlertEvent> alerts = alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc();
        assertEquals(1, alerts.size());
        AlertEvent alert = alerts.get(0);
        assertEquals(EventType.SIGNATURE_INVALID, alert.getEventType());
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity(),
                "Baseline signature corruption represents database compromise and must be CRITICAL");
    }

    @Test
    @DisplayName("S5-T4.4: File deletion on disk -> Detect MISSING status and emit MISSING_FILE alert")
    void testMissingFileDetection() throws IOException, GeneralSecurityException {
        // 1. Create and baseline file
        Path filePath = tempDir.resolve("service_private.key");
        Files.writeString(filePath, "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASC\n-----END PRIVATE KEY-----\n");

        WatchedFile watchedFile = new WatchedFile(
                filePath.toAbsolutePath().toString(),
                Files.size(filePath),
                LocalDateTime.now(),
                FileStatus.UNTRACKED
        );
        watchedFile = watchedFileRepository.save(watchedFile);
        comparisonService.establishBaseline(watchedFile);

        // 2. Delete the file from the filesystem
        Files.delete(filePath);
        assertFalse(Files.exists(filePath));

        // 3. Run verification scan
        comparisonService.runVerificationScan();

        // 4. Assert status transitions to MISSING
        WatchedFile refreshed = watchedFileRepository.findById(watchedFile.getId()).orElseThrow();
        assertEquals(FileStatus.MISSING, refreshed.getStatus());

        List<AlertEvent> alerts = alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc();
        assertEquals(1, alerts.size());
        AlertEvent alert = alerts.get(0);
        assertEquals(EventType.MISSING_FILE, alert.getEventType());
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
        assertNull(alert.getActualHash(), "Actual hash must be null when file is absent from disk");
    }

    @Test
    @DisplayName("S5-T4.5: Authorized re-baselining -> Retires historical baseline and restores VERIFIED state")
    void testAuthorizedRebaselineLifecycle() throws IOException, GeneralSecurityException {
        // 1. Initial baseline establishment
        Path filePath = tempDir.resolve("application.properties");
        Files.writeString(filePath, "app.version=1.0.0\n");

        WatchedFile watchedFile = new WatchedFile(
                filePath.toAbsolutePath().toString(),
                Files.size(filePath),
                LocalDateTime.now(),
                FileStatus.UNTRACKED
        );
        watchedFile = watchedFileRepository.save(watchedFile);
        BaselineEntry baselineV1 = comparisonService.establishBaseline(watchedFile);
        Long v1Id = baselineV1.getId();

        // 2. Authorized software release modifies the configuration
        Files.writeString(filePath, "app.version=2.0.0\nfeatures.dark_mode=true\n");

        // 3. Periodic scan detects mismatch before SecOps re-baselines
        comparisonService.runVerificationScan();
        assertEquals(FileStatus.TAMPERED, watchedFileRepository.findById(watchedFile.getId()).orElseThrow().getStatus());

        // 4. SecOps conducts authorized re-baseline
        BaselineEntry baselineV2 = comparisonService.establishBaseline(watchedFile);
        assertNotEquals(v1Id, baselineV2.getId());
        assertTrue(baselineV2.isCurrent());

        // 5. Assert historical baseline V1 is retired (current = false)
        BaselineEntry retiredV1 = baselineEntryRepository.findById(v1Id).orElseThrow();
        assertFalse(retiredV1.isCurrent(), "Superseded baseline must be marked current = false for forensic auditability");

        // 6. Assert file status is restored to VERIFIED
        WatchedFile refreshed = watchedFileRepository.findById(watchedFile.getId()).orElseThrow();
        assertEquals(FileStatus.VERIFIED, refreshed.getStatus());

        // 7. Subsequent verification scan confirms integrity against new baseline V2
        comparisonService.runVerificationScan();
        assertEquals(FileStatus.VERIFIED, watchedFileRepository.findById(watchedFile.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("S5-T4.6: Repeated verification passes -> State-transition deduplication prevents alert spam")
    void testAlertDeduplicationAcrossPollingIntervals() throws IOException, GeneralSecurityException {
        Path filePath = tempDir.resolve("audit_trail.log");
        Files.writeString(filePath, "2026-11-01 10:00:00 INITIALIZED\n");

        WatchedFile watchedFile = new WatchedFile(
                filePath.toAbsolutePath().toString(),
                Files.size(filePath),
                LocalDateTime.now(),
                FileStatus.UNTRACKED
        );
        watchedFile = watchedFileRepository.save(watchedFile);
        comparisonService.establishBaseline(watchedFile);

        // Modify file
        Files.writeString(filePath, "2026-11-01 10:00:00 COMPROMISED\n");

        // First verification pass
        comparisonService.runVerificationScan();
        assertEquals(1, alertEventRepository.countByResolvedFalse());

        // Second verification pass (e.g. 30 seconds later in Quartz loop)
        comparisonService.runVerificationScan();
        assertEquals(1, alertEventRepository.countByResolvedFalse(),
                "Duplicate alerts must not be spammed for unchanged unresolved integrity events");

        // Third verification pass
        comparisonService.runVerificationScan();
        assertEquals(1, alertEventRepository.countByResolvedFalse());
    }

    @Test
    @DisplayName("S5-T4.7: Inactive watched file -> Skipped during batch verification scans")
    void testInactiveFileExcludedFromScan() throws IOException, GeneralSecurityException {
        Path filePath = tempDir.resolve("legacy_script.sh");
        Files.writeString(filePath, "#!/bin/bash\necho 'deprecated'\n");

        WatchedFile watchedFile = new WatchedFile(
                filePath.toAbsolutePath().toString(),
                Files.size(filePath),
                LocalDateTime.now(),
                FileStatus.UNTRACKED
        );
        watchedFile = watchedFileRepository.save(watchedFile);
        comparisonService.establishBaseline(watchedFile);

        // Deactivate file monitoring
        watchedFile.setActive(false);
        watchedFile = watchedFileRepository.save(watchedFile);

        // Modify file content on disk
        Files.writeString(filePath, "#!/bin/bash\necho 'attacker shell'\n");

        // Run batch scan
        comparisonService.runVerificationScan();

        // Inactive file should be skipped, maintaining its previous status and creating zero alerts
        WatchedFile refreshed = watchedFileRepository.findById(watchedFile.getId()).orElseThrow();
        assertEquals(FileStatus.VERIFIED, refreshed.getStatus());
        assertEquals(0, alertEventRepository.count());
    }
}
