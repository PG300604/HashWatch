package com.hashwatch.repository;

import com.hashwatch.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * =============================================================================
 * DOMAIN: DBMS
 * ASSIGNED TO: Priyanshu (Sprint 1: S1-T4)
 * TARGET: src/test/java/com/hashwatch/repository/RepositoryIntegrationTest.java
 * =============================================================================
 *
 * Verifies JPA entity mappings, indexes, unique constraints, enum persistence,
 * and repository query methods against the H2 test database.
 */
@DataJpaTest
@ActiveProfiles("h2")
class RepositoryIntegrationTest {

    @Autowired
    private WatchedFileRepository watchedFileRepository;

    @Autowired
    private BaselineEntryRepository baselineEntryRepository;

    @Autowired
    private AlertEventRepository alertEventRepository;

    private static final String SAMPLE_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String ALT_SHA256    = "a3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String SAMPLE_SIG    = "dGVzdC1lZDI1NTE5LXNpZ25hdHVyZS1iYXNlNjQ=";

    @Test
    @DisplayName("S1-T4: Save WatchedFile and verify @PrePersist defaults and queries")
    void testSaveAndQueryWatchedFile() {
        WatchedFile file = new WatchedFile("/etc/ssh/sshd_config", 4096L, LocalDateTime.now(), FileStatus.VERIFIED);
        WatchedFile saved = watchedFileRepository.saveAndFlush(file);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(FileStatus.VERIFIED, saved.getStatus());
        assertTrue(saved.isActive());

        Optional<WatchedFile> byPath = watchedFileRepository.findByFilePath("/etc/ssh/sshd_config");
        assertTrue(byPath.isPresent());
        assertEquals(4096L, byPath.get().getFileSize());

        List<WatchedFile> verifiedFiles = watchedFileRepository.findByStatus(FileStatus.VERIFIED);
        assertEquals(1, verifiedFiles.size());
        assertEquals(1L, watchedFileRepository.countByActiveTrue());
    }

    @Test
    @DisplayName("S1-T4: Enforce unique constraint on watched_files.file_path")
    void testUniqueFilePathConstraint() {
        WatchedFile file1 = new WatchedFile("/var/log/auth.log", 1024L, LocalDateTime.now(), FileStatus.UNTRACKED);
        watchedFileRepository.saveAndFlush(file1);

        WatchedFile duplicate = new WatchedFile("/var/log/auth.log", 2048L, LocalDateTime.now(), FileStatus.VERIFIED);
        assertThrows(DataIntegrityViolationException.class, () -> {
            watchedFileRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    @DisplayName("S1-T4: Establish and rotate BaselineEntry for a WatchedFile")
    void testBaselineRotationAndLookup() {
        WatchedFile file = watchedFileRepository.saveAndFlush(
                new WatchedFile("/etc/hosts", 512L, LocalDateTime.now(), FileStatus.VERIFIED)
        );

        // Initial baseline
        BaselineEntry firstBaseline = new BaselineEntry(file, SAMPLE_SHA256, SAMPLE_SIG, "key-v1");
        baselineEntryRepository.saveAndFlush(firstBaseline);

        Optional<BaselineEntry> currentOpt = baselineEntryRepository.findByWatchedFileAndCurrentTrue(file);
        assertTrue(currentOpt.isPresent());
        assertEquals(SAMPLE_SHA256, currentOpt.get().getSha256Hash());

        // Rotate baseline: retire old and insert new
        firstBaseline.setCurrent(false);
        baselineEntryRepository.saveAndFlush(firstBaseline);

        BaselineEntry secondBaseline = new BaselineEntry(file, ALT_SHA256, SAMPLE_SIG, "key-v1");
        baselineEntryRepository.saveAndFlush(secondBaseline);

        Optional<BaselineEntry> updatedCurrent = baselineEntryRepository.findByWatchedFileAndCurrentTrue(file);
        assertTrue(updatedCurrent.isPresent());
        assertEquals(ALT_SHA256, updatedCurrent.get().getSha256Hash());

        List<BaselineEntry> history = baselineEntryRepository.findByWatchedFileOrderByCreatedAtDesc(file);
        assertEquals(2, history.size());
    }

    @Test
    @DisplayName("S1-T4: Persist AlertEvent with Enums and query unresolved alerts")
    void testAlertEventPersistenceAndTriage() {
        WatchedFile file = watchedFileRepository.saveAndFlush(
                new WatchedFile("/etc/passwd", 2048L, LocalDateTime.now(), FileStatus.TAMPERED)
        );

        AlertEvent alert1 = new AlertEvent(
                file,
                file.getFilePath(),
                EventType.UNAUTHORIZED_MODIFICATION,
                AlertSeverity.CRITICAL,
                SAMPLE_SHA256,
                ALT_SHA256,
                "Hash mismatch detected on /etc/passwd"
        );
        alertEventRepository.saveAndFlush(alert1);

        assertEquals(1L, alertEventRepository.countByResolvedFalse());

        List<AlertEvent> unresolved = alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc();
        assertEquals(1, unresolved.size());
        assertEquals(EventType.UNAUTHORIZED_MODIFICATION, unresolved.get(0).getEventType());
        assertEquals(AlertSeverity.CRITICAL, unresolved.get(0).getSeverity());

        // Resolve the alert
        alert1.setResolved(true);
        alertEventRepository.saveAndFlush(alert1);
        assertEquals(0L, alertEventRepository.countByResolvedFalse());
    }
}
