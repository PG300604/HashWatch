package com.hashwatch.repository;

import com.hashwatch.entity.*;
import jakarta.persistence.EntityManager;
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
 * foreign-key cascade / set-null semantics, and repository query methods.
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

    @Autowired
    private EntityManager entityManager;

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
    @DisplayName("S1-T4 [RepoMind #01]: Persist WatchedFile with each FileStatus value and assert enum string storage and retrieval")
    void testPersistAllFileStatusEnumValues() {
        for (FileStatus status : FileStatus.values()) {
            String path = "/etc/test/status_" + status.name().toLowerCase() + ".conf";
            WatchedFile file = new WatchedFile(path, 256L, LocalDateTime.now(), status);
            WatchedFile saved = watchedFileRepository.saveAndFlush(file);
            entityManager.clear();

            // Verify JPA enum retrieval
            List<WatchedFile> foundByStatus = watchedFileRepository.findByStatus(status);
            assertEquals(1, foundByStatus.size());
            assertEquals(status, foundByStatus.get(0).getStatus());

            // Verify underlying database column stores the exact enum string name
            String rawDbStatus = (String) entityManager
                    .createNativeQuery("SELECT CAST(status AS VARCHAR) FROM watched_files WHERE id = :id")
                    .setParameter("id", saved.getId())
                    .getSingleResult();
            assertEquals(status.name(), rawDbStatus);
        }
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

    @Test
    @DisplayName("S1-T4 [RepoMind #02]: Deleting WatchedFile cascades to BaselineEntry and sets watched_file_id NULL on AlertEvent")
    void testDeleteWatchedFileCascadesToBaselineAndSetsNullOnAlertEvent() {
        WatchedFile file = watchedFileRepository.saveAndFlush(
                new WatchedFile("/etc/sudoers", 1024L, LocalDateTime.now(), FileStatus.VERIFIED)
        );

        BaselineEntry baseline = baselineEntryRepository.saveAndFlush(
                new BaselineEntry(file, SAMPLE_SHA256, SAMPLE_SIG, "key-v1")
        );

        AlertEvent alert = alertEventRepository.saveAndFlush(
                new AlertEvent(
                        file,
                        file.getFilePath(),
                        EventType.MISMATCH,
                        AlertSeverity.HIGH,
                        SAMPLE_SHA256,
                        ALT_SHA256,
                        "Integrity anomaly on /etc/sudoers"
                )
        );

        Long fileId = file.getId();
        Long baselineId = baseline.getId();
        Long alertId = alert.getId();

        entityManager.flush();
        entityManager.clear();

        // Delete parent WatchedFile and flush to trigger DB foreign key actions
        assertDoesNotThrow(() -> {
            watchedFileRepository.deleteById(fileId);
            watchedFileRepository.flush();
        });
        entityManager.clear();

        // 1. WatchedFile is deleted
        assertFalse(watchedFileRepository.findById(fileId).isPresent());

        // 2. Associated BaselineEntry is deleted via ON DELETE CASCADE
        assertFalse(baselineEntryRepository.findById(baselineId).isPresent(),
                "BaselineEntry should be cascade-deleted when parent WatchedFile is removed");

        // 3. Related AlertEvent survives with watched_file_id set to NULL via ON DELETE SET NULL
        Optional<AlertEvent> survivingAlertOpt = alertEventRepository.findById(alertId);
        assertTrue(survivingAlertOpt.isPresent(),
                "AlertEvent must survive parent WatchedFile deletion for audit history");
        AlertEvent survivingAlert = survivingAlertOpt.get();
        assertNull(survivingAlert.getWatchedFile(),
                "watched_file_id must be set to NULL after WatchedFile deletion");
        assertEquals("/etc/sudoers", survivingAlert.getFilePath(),
                "Historical filePath snapshot must remain intact");
    }

    @Test
    @DisplayName("S1-T4 [RepoMind #03]: AlertEvent creation with null watched_file_id records file_path snapshot properly")
    void testAlertEventCreationWithNullWatchedFileReference() {
        AlertEvent detachedAlert = new AlertEvent(
                null,
                "/opt/app/deleted_config.yaml",
                EventType.MISSING_FILE,
                AlertSeverity.HIGH,
                SAMPLE_SHA256,
                null,
                "Alert logged for unlinked/removed file record"
        );

        AlertEvent saved = assertDoesNotThrow(() -> alertEventRepository.saveAndFlush(detachedAlert));
        entityManager.clear();

        Optional<AlertEvent> retrievedOpt = alertEventRepository.findById(saved.getId());
        assertTrue(retrievedOpt.isPresent());
        AlertEvent retrieved = retrievedOpt.get();

        assertNull(retrieved.getWatchedFile());
        assertEquals("/opt/app/deleted_config.yaml", retrieved.getFilePath());
        assertEquals(EventType.MISSING_FILE, retrieved.getEventType());
        assertEquals(AlertSeverity.HIGH, retrieved.getSeverity());
        assertEquals(SAMPLE_SHA256, retrieved.getExpectedHash());
        assertNull(retrieved.getActualHash());
        assertNotNull(retrieved.getDetectedAt());
    }
}
