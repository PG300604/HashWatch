package com.hashwatch.service;

import com.hashwatch.entity.AlertEvent;
import com.hashwatch.entity.AlertSeverity;
import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.EventType;
import com.hashwatch.entity.FileStatus;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * =============================================================================
 * DOMAIN: Backend
 * ASSIGNED TO: Priyanshu (Sprint 2: S2-T1)
 * TARGET: src/test/java/com/hashwatch/service/ComparisonServiceTest.java
 * =============================================================================
 *
 * Comprehensive unit and verification tests for ComparisonService:
 * 1. establishBaseline: SHA-256 streaming, Ed25519 signing, and status transitions
 * 2. verifyFile: Zero-trust signature verification, tampering detection, and missing file handling
 * 3. Fingerprint-pinned validation and rogue key substitution defense
 * 4. State-transition alert deduplication during periodic scans
 * 5. Batch scan fault isolation
 */
class ComparisonServiceTest {

    private HashingService hashingService;
    private SigningService signingService;
    private WatchedFileRepository watchedFileRepository;
    private BaselineEntryRepository baselineEntryRepository;
    private AlertEventRepository alertEventRepository;
    private ComparisonService comparisonService;

    private Path tempKeyDir;
    private Path tempWatchDir;

    @BeforeEach
    void setUp(@TempDir Path tempKeyDir, @TempDir Path tempWatchDir) throws Exception {
        this.tempKeyDir = tempKeyDir;
        this.tempWatchDir = tempWatchDir;

        this.hashingService = new HashingService();
        this.signingService = new SigningService();
        ReflectionTestUtils.setField(signingService, "keyDirectoryPath", tempKeyDir.toString());
        signingService.ensureKeysLoaded();

        this.watchedFileRepository = mock(WatchedFileRepository.class);
        this.baselineEntryRepository = mock(BaselineEntryRepository.class);
        this.alertEventRepository = mock(AlertEventRepository.class);

        this.comparisonService = new ComparisonService(
                hashingService,
                signingService,
                watchedFileRepository,
                baselineEntryRepository,
                alertEventRepository
        );

        // Default mock behaviors
        when(watchedFileRepository.save(any(WatchedFile.class))).thenAnswer(i -> i.getArgument(0));
        when(baselineEntryRepository.save(any(BaselineEntry.class))).thenAnswer(i -> {
            BaselineEntry entry = i.getArgument(0);
            ReflectionTestUtils.setField(entry, "id", 101L);
            return entry;
        });
        when(alertEventRepository.save(any(AlertEvent.class))).thenAnswer(i -> {
            AlertEvent alert = i.getArgument(0);
            ReflectionTestUtils.setField(alert, "id", 501L);
            return alert;
        });
        when(alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc()).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("establishBaseline should hash file, sign Triple-Lock payload, and set status to VERIFIED")
    void shouldEstablishBaselineSuccessfully() throws Exception {
        Path testFile = tempWatchDir.resolve("server.conf");
        Files.writeString(testFile, "port=8080\nenv=production");

        WatchedFile watchedFile = new WatchedFile(testFile.toString(), null, null, FileStatus.UNTRACKED);
        when(baselineEntryRepository.findByWatchedFileAndCurrentTrue(watchedFile)).thenReturn(Optional.empty());

        BaselineEntry baseline = comparisonService.establishBaseline(watchedFile);

        assertNotNull(baseline);
        assertTrue(baseline.isCurrent());
        assertEquals(64, baseline.getSha256Hash().length(), "SHA-256 hash must be 64 hex characters");
        assertNotNull(baseline.getSignature(), "Ed25519 signature must not be null");
        assertFalse(baseline.getSignature().isBlank());
        assertEquals(signingService.getKeyFingerprint(), baseline.getPublicKeyId(),
                "Baseline must record active public key fingerprint");

        assertEquals(FileStatus.VERIFIED, watchedFile.getStatus());
        assertEquals(Files.size(testFile), watchedFile.getFileSize());
        assertNotNull(watchedFile.getLastModified());

        verify(baselineEntryRepository, times(1)).save(any(BaselineEntry.class));
        verify(watchedFileRepository, times(1)).save(watchedFile);
    }

    @Test
    @DisplayName("establishBaseline should throw FileNotFoundException if file does not exist on disk")
    void shouldThrowWhenFileDoesNotExistOnDisk() {
        WatchedFile watchedFile = new WatchedFile(tempWatchDir.resolve("non_existent.txt").toString(), null, null, null);
        assertThrows(FileNotFoundException.class, () -> comparisonService.establishBaseline(watchedFile));
    }

    @Test
    @DisplayName("establishBaseline should retire previous baseline when re-baselining an existing file")
    void shouldRetirePreviousBaselineOnRebaseline() throws Exception {
        Path testFile = tempWatchDir.resolve("app.properties");
        Files.writeString(testFile, "spring.datasource.url=jdbc:h2:mem:test");

        WatchedFile watchedFile = new WatchedFile(testFile.toString(), null, null, FileStatus.VERIFIED);
        BaselineEntry oldBaseline = new BaselineEntry(watchedFile, "oldHash64Chars0000000000000000000000000000000000000000000000000000", "sig", "keyId");
        oldBaseline.setCurrent(true);

        when(baselineEntryRepository.findByWatchedFileAndCurrentTrue(watchedFile)).thenReturn(Optional.of(oldBaseline));

        BaselineEntry newBaseline = comparisonService.establishBaseline(watchedFile);

        assertNotNull(newBaseline);
        assertFalse(oldBaseline.isCurrent(), "Old baseline must be retired (isCurrent = false)");
        assertTrue(newBaseline.isCurrent(), "New baseline must be marked current");

        verify(baselineEntryRepository, times(2)).save(any(BaselineEntry.class));
    }

    @Test
    @DisplayName("verifyFile should keep status VERIFIED and create zero alerts when disk file matches baseline")
    void shouldVerifyIntactFileAsVerified() throws Exception {
        Path testFile = tempWatchDir.resolve("intact.bin");
        Files.writeString(testFile, "genuine unaltered content");

        WatchedFile watchedFile = new WatchedFile(testFile.toString(), Files.size(testFile), null, FileStatus.VERIFIED);
        String hash = hashingService.hashFile(testFile.toFile());
        String payload = comparisonService.buildCanonicalPayload(testFile.toString(), hash, Files.size(testFile));
        String signature = signingService.sign(payload);
        String keyFingerprint = signingService.getKeyFingerprint();

        BaselineEntry baseline = new BaselineEntry(watchedFile, hash, signature, keyFingerprint);
        when(baselineEntryRepository.findByWatchedFileAndCurrentTrue(watchedFile)).thenReturn(Optional.of(baseline));

        comparisonService.verifyFile(watchedFile);

        assertEquals(FileStatus.VERIFIED, watchedFile.getStatus());
        verify(watchedFileRepository, times(1)).save(watchedFile);
        verify(alertEventRepository, never()).save(any(AlertEvent.class));
    }

    @Test
    @DisplayName("verifyFile should detect altered bytes, set TAMPERED, and raise HIGH severity AlertEvent")
    void shouldDetectTamperedFileAndGenerateHighAlert() throws Exception {
        Path testFile = tempWatchDir.resolve("sensitive.data");
        Files.writeString(testFile, "original confidential data");

        WatchedFile watchedFile = new WatchedFile(testFile.toString(), Files.size(testFile), null, FileStatus.VERIFIED);
        String originalHash = hashingService.hashFile(testFile.toFile());
        String payload = comparisonService.buildCanonicalPayload(testFile.toString(), originalHash, Files.size(testFile));
        String signature = signingService.sign(payload);
        String keyFingerprint = signingService.getKeyFingerprint();

        BaselineEntry baseline = new BaselineEntry(watchedFile, originalHash, signature, keyFingerprint);
        when(baselineEntryRepository.findByWatchedFileAndCurrentTrue(watchedFile)).thenReturn(Optional.of(baseline));

        // Inject tamper bytes into file on disk
        Files.writeString(testFile, "malicious modified payload");

        comparisonService.verifyFile(watchedFile);

        assertEquals(FileStatus.TAMPERED, watchedFile.getStatus());

        ArgumentCaptor<AlertEvent> alertCaptor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertEventRepository, times(1)).save(alertCaptor.capture());

        AlertEvent alert = alertCaptor.getValue();
        assertEquals(EventType.UNAUTHORIZED_MODIFICATION, alert.getEventType());
        assertEquals(AlertSeverity.HIGH, alert.getSeverity());
        assertEquals(originalHash, alert.getExpectedHash());
        assertNotNull(alert.getActualHash());
        assertNotEquals(originalHash, alert.getActualHash());
    }

    @Test
    @DisplayName("verifyFile should detect deleted file, set MISSING, and raise CRITICAL AlertEvent")
    void shouldDetectMissingFileAndGenerateCriticalAlert() throws Exception {
        Path testFile = tempWatchDir.resolve("ephemeral.log");
        Files.writeString(testFile, "log entries");

        WatchedFile watchedFile = new WatchedFile(testFile.toString(), Files.size(testFile), null, FileStatus.VERIFIED);
        String hash = hashingService.hashFile(testFile.toFile());
        String payload = comparisonService.buildCanonicalPayload(testFile.toString(), hash, Files.size(testFile));
        String signature = signingService.sign(payload);

        BaselineEntry baseline = new BaselineEntry(watchedFile, hash, signature, signingService.getKeyFingerprint());
        when(baselineEntryRepository.findByWatchedFileAndCurrentTrue(watchedFile)).thenReturn(Optional.of(baseline));

        // Delete file from filesystem
        Files.delete(testFile);

        comparisonService.verifyFile(watchedFile);

        assertEquals(FileStatus.MISSING, watchedFile.getStatus());

        ArgumentCaptor<AlertEvent> alertCaptor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertEventRepository, times(1)).save(alertCaptor.capture());

        AlertEvent alert = alertCaptor.getValue();
        assertEquals(EventType.MISSING_FILE, alert.getEventType());
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
    }

    @Test
    @DisplayName("verifyFile should detect tampered baseline signature, set SIGNATURE_INVALID, and raise CRITICAL alert")
    void shouldDetectDatabaseTamperingViaSignatureInvalid() throws Exception {
        Path testFile = tempWatchDir.resolve("secure.key");
        Files.writeString(testFile, "key content");

        WatchedFile watchedFile = new WatchedFile(testFile.toString(), Files.size(testFile), null, FileStatus.VERIFIED);
        String hash = hashingService.hashFile(testFile.toFile());

        // Corrupted/tampered signature in database
        String corruptedSignature = "AAAA" + signingService.sign("dummy").substring(4);
        BaselineEntry baseline = new BaselineEntry(watchedFile, hash, corruptedSignature, signingService.getKeyFingerprint());
        when(baselineEntryRepository.findByWatchedFileAndCurrentTrue(watchedFile)).thenReturn(Optional.of(baseline));

        comparisonService.verifyFile(watchedFile);

        assertEquals(FileStatus.SIGNATURE_INVALID, watchedFile.getStatus());

        ArgumentCaptor<AlertEvent> alertCaptor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertEventRepository, times(1)).save(alertCaptor.capture());

        AlertEvent alert = alertCaptor.getValue();
        assertEquals(EventType.SIGNATURE_INVALID, alert.getEventType());
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
    }

    @Test
    @DisplayName("verifyFile should detect rogue public key substitution via fingerprint mismatch")
    void shouldDetectRogueKeySubstitutionViaFingerprintMismatch() throws Exception {
        Path testFile = tempWatchDir.resolve("finance.csv");
        Files.writeString(testFile, "account,balance\n123,5000");

        WatchedFile watchedFile = new WatchedFile(testFile.toString(), Files.size(testFile), null, FileStatus.VERIFIED);
        String hash = hashingService.hashFile(testFile.toFile());
        String payload = comparisonService.buildCanonicalPayload(testFile.toString(), hash, Files.size(testFile));
        String signature = signingService.sign(payload);

        // Rogue fingerprint that does not match pinned active public key
        String rogueFingerprint = "0000000000000000000000000000000000000000000000000000000000000000";
        BaselineEntry baseline = new BaselineEntry(watchedFile, hash, signature, rogueFingerprint);
        when(baselineEntryRepository.findByWatchedFileAndCurrentTrue(watchedFile)).thenReturn(Optional.of(baseline));

        comparisonService.verifyFile(watchedFile);

        assertEquals(FileStatus.SIGNATURE_INVALID, watchedFile.getStatus());

        ArgumentCaptor<AlertEvent> alertCaptor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertEventRepository, times(1)).save(alertCaptor.capture());
        assertEquals(EventType.SIGNATURE_INVALID, alertCaptor.getValue().getEventType());
    }

    @Test
    @DisplayName("verifyFile should deduplicate alerts across consecutive scans when unresolved alert exists")
    void shouldDeduplicateAlertsAcrossMultipleScans() throws Exception {
        Path testFile = tempWatchDir.resolve("dedup.txt");
        Files.writeString(testFile, "initial text");

        WatchedFile watchedFile = new WatchedFile(testFile.toString(), Files.size(testFile), null, FileStatus.VERIFIED);
        String hash = hashingService.hashFile(testFile.toFile());
        String payload = comparisonService.buildCanonicalPayload(testFile.toString(), hash, Files.size(testFile));
        String signature = signingService.sign(payload);

        BaselineEntry baseline = new BaselineEntry(watchedFile, hash, signature, signingService.getKeyFingerprint());
        when(baselineEntryRepository.findByWatchedFileAndCurrentTrue(watchedFile)).thenReturn(Optional.of(baseline));

        // Tamper with file
        Files.writeString(testFile, "tampered text");

        // Scan 1: No previous alerts exist -> Creates AlertEvent
        when(alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc()).thenReturn(Collections.emptyList());
        comparisonService.verifyFile(watchedFile);
        verify(alertEventRepository, times(1)).save(any(AlertEvent.class));

        // Scan 2: Unresolved alert now exists in repository -> Skips duplicate creation
        AlertEvent existingAlert = new AlertEvent(watchedFile, testFile.toString(), EventType.UNAUTHORIZED_MODIFICATION,
                AlertSeverity.HIGH, hash, "newHash", "tampered");
        when(alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc()).thenReturn(List.of(existingAlert));

        comparisonService.verifyFile(watchedFile);
        comparisonService.verifyFile(watchedFile);

        // Alert repository save count should still remain 1, not 3!
        verify(alertEventRepository, times(1)).save(any(AlertEvent.class));
    }

    @Test
    @DisplayName("runVerificationScan should verify all active files and isolate errors per file")
    void shouldIsolateExceptionsDuringBatchVerificationScan() throws Exception {
        Path healthyFile = tempWatchDir.resolve("healthy.txt");
        Files.writeString(healthyFile, "all good");

        WatchedFile file1 = new WatchedFile(healthyFile.toString(), Files.size(healthyFile), null, FileStatus.VERIFIED);
        String hash1 = hashingService.hashFile(healthyFile.toFile());
        String payload1 = comparisonService.buildCanonicalPayload(healthyFile.toString(), hash1, Files.size(healthyFile));
        BaselineEntry baseline1 = new BaselineEntry(file1, hash1, signingService.sign(payload1), signingService.getKeyFingerprint());

        WatchedFile file2 = new WatchedFile(tempWatchDir.resolve("missing.txt").toString(), 10L, null, FileStatus.VERIFIED);
        BaselineEntry baseline2 = new BaselineEntry(file2, "dummyHash", "dummySig", signingService.getKeyFingerprint());

        when(watchedFileRepository.findByActiveTrue()).thenReturn(List.of(file1, file2));
        when(baselineEntryRepository.findByWatchedFileAndCurrentTrue(file1)).thenReturn(Optional.of(baseline1));
        when(baselineEntryRepository.findByWatchedFileAndCurrentTrue(file2)).thenReturn(Optional.of(baseline2));

        assertDoesNotThrow(() -> comparisonService.runVerificationScan());

        assertEquals(FileStatus.VERIFIED, file1.getStatus());
        assertEquals(FileStatus.MISSING, file2.getStatus());
    }
}
