package com.hashwatch.repository;

import com.hashwatch.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * =============================================================================
 * DOMAIN: DBMS & API
 * ASSIGNED TO: Riya (Sprint 2: S2-T3)
 * TARGET: src/test/java/com/hashwatch/repository/AlertEventRepositoryTest.java
 * =============================================================================
 *
 * Comprehensive persistence tests for {@link AlertEventRepository} query methods
 * added in Sprint 2 (S2-T3). Validates unresolved-alert filters, severity and
 * event-type classification queries, date-range searches, per-file lookups,
 * paginated result sets, and aggregation counts against the H2 in-memory database.
 *
 * <p>These tests complement the existing {@link RepositoryIntegrationTest} (S1-T4)
 * which covers entity mapping, enum persistence, unique constraints, cascade
 * semantics, and basic alert triage. This class focuses exclusively on the
 * enriched AlertEventRepository query surface.</p>
 */
@DataJpaTest
@ActiveProfiles("h2")
class AlertEventRepositoryTest {

    @Autowired
    private AlertEventRepository alertEventRepository;

    @Autowired
    private WatchedFileRepository watchedFileRepository;

    @Autowired
    private EntityManager entityManager;

    private static final String SAMPLE_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String ALT_SHA256    = "a3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private WatchedFile filePasswd;
    private WatchedFile fileHosts;

    /**
     * Seed the database with two watched files and five alerts spanning
     * different severities, event types, and resolution states.
     *
     * Alert landscape after setup:
     *   alert1: CRITICAL / UNAUTHORIZED_MODIFICATION / unresolved  (filePasswd)
     *   alert2: HIGH     / MISMATCH                  / unresolved  (filePasswd)
     *   alert3: MEDIUM   / MISSING_FILE              / resolved    (fileHosts)
     *   alert4: CRITICAL / SIGNATURE_INVALID         / unresolved  (fileHosts)
     *   alert5: LOW      / MISMATCH                  / resolved    (filePasswd)
     */
    @BeforeEach
    void seedTestData() {
        alertEventRepository.deleteAllInBatch();
        watchedFileRepository.deleteAllInBatch();

        filePasswd = watchedFileRepository.saveAndFlush(
                new WatchedFile("/etc/passwd", 2048L, LocalDateTime.now(), FileStatus.TAMPERED));
        fileHosts = watchedFileRepository.saveAndFlush(
                new WatchedFile("/etc/hosts", 512L, LocalDateTime.now(), FileStatus.MISSING));

        // alert1: CRITICAL / UNAUTHORIZED_MODIFICATION / unresolved
        AlertEvent a1 = new AlertEvent(filePasswd, filePasswd.getFilePath(),
                EventType.UNAUTHORIZED_MODIFICATION, AlertSeverity.CRITICAL,
                SAMPLE_SHA256, ALT_SHA256, "Unauthorized change to /etc/passwd");
        a1.setDetectedAt(LocalDateTime.of(2026, 11, 1, 10, 0));
        alertEventRepository.saveAndFlush(a1);

        // alert2: HIGH / MISMATCH / unresolved
        AlertEvent a2 = new AlertEvent(filePasswd, filePasswd.getFilePath(),
                EventType.MISMATCH, AlertSeverity.HIGH,
                SAMPLE_SHA256, ALT_SHA256, "Hash mismatch on /etc/passwd");
        a2.setDetectedAt(LocalDateTime.of(2026, 11, 2, 14, 30));
        alertEventRepository.saveAndFlush(a2);

        // alert3: MEDIUM / MISSING_FILE / resolved
        AlertEvent a3 = new AlertEvent(fileHosts, fileHosts.getFilePath(),
                EventType.MISSING_FILE, AlertSeverity.MEDIUM,
                SAMPLE_SHA256, null, "File /etc/hosts not found on disk");
        a3.setDetectedAt(LocalDateTime.of(2026, 11, 3, 9, 15));
        a3.setResolved(true);
        alertEventRepository.saveAndFlush(a3);

        // alert4: CRITICAL / SIGNATURE_INVALID / unresolved
        AlertEvent a4 = new AlertEvent(fileHosts, fileHosts.getFilePath(),
                EventType.SIGNATURE_INVALID, AlertSeverity.CRITICAL,
                SAMPLE_SHA256, SAMPLE_SHA256, "Ed25519 signature verification failed for /etc/hosts");
        a4.setDetectedAt(LocalDateTime.of(2026, 11, 4, 16, 45));
        alertEventRepository.saveAndFlush(a4);

        // alert5: LOW / MISMATCH / resolved
        AlertEvent a5 = new AlertEvent(filePasswd, filePasswd.getFilePath(),
                EventType.MISMATCH, AlertSeverity.LOW,
                SAMPLE_SHA256, ALT_SHA256, "Minor mismatch on /etc/passwd (resolved)");
        a5.setDetectedAt(LocalDateTime.of(2026, 10, 28, 8, 0));
        a5.setResolved(true);
        alertEventRepository.saveAndFlush(a5);

        entityManager.flush();
        entityManager.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 1. Unresolved Alert Queries
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("S2-T3: Unresolved Alert Queries")
    class UnresolvedAlertTests {

        @Test
        @DisplayName("findByResolvedFalseOrderByDetectedAtDesc returns only unresolved alerts, newest first")
        void testFindUnresolvedAlertsOrderedByDetectedAtDesc() {
            List<AlertEvent> unresolved = alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc();

            assertEquals(3, unresolved.size(), "Exactly 3 alerts are unresolved");
            // Newest first: alert4 (Nov 4) > alert2 (Nov 2) > alert1 (Nov 1)
            assertEquals(AlertSeverity.CRITICAL, unresolved.get(0).getSeverity());
            assertEquals(EventType.SIGNATURE_INVALID, unresolved.get(0).getEventType());
            assertEquals(AlertSeverity.HIGH, unresolved.get(1).getSeverity());
            assertEquals(AlertSeverity.CRITICAL, unresolved.get(2).getSeverity());

            // Verify none are resolved
            unresolved.forEach(a -> assertFalse(a.isResolved(), "All returned alerts must be unresolved"));
        }

        @Test
        @DisplayName("countByResolvedFalse returns correct count of unresolved alerts")
        void testCountUnresolved() {
            assertEquals(3L, alertEventRepository.countByResolvedFalse());
        }

        @Test
        @DisplayName("findByResolvedFalseAndSeverityOrderByDetectedAtDesc filters unresolved by CRITICAL severity")
        void testFindUnresolvedBySeverity() {
            List<AlertEvent> criticalUnresolved =
                    alertEventRepository.findByResolvedFalseAndSeverityOrderByDetectedAtDesc(AlertSeverity.CRITICAL);

            assertEquals(2, criticalUnresolved.size(), "Two CRITICAL alerts are unresolved");
            criticalUnresolved.forEach(a -> {
                assertFalse(a.isResolved());
                assertEquals(AlertSeverity.CRITICAL, a.getSeverity());
            });
            // Newest first: alert4 (Nov 4) > alert1 (Nov 1)
            assertTrue(criticalUnresolved.get(0).getDetectedAt()
                    .isAfter(criticalUnresolved.get(1).getDetectedAt()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. Severity Filter Queries
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("S2-T3: Severity Filter Queries")
    class SeverityFilterTests {

        @Test
        @DisplayName("findBySeverity returns all alerts of a given severity regardless of resolution state")
        void testFindBySeverity() {
            List<AlertEvent> criticalAlerts = alertEventRepository.findBySeverity(AlertSeverity.CRITICAL);
            assertEquals(2, criticalAlerts.size(), "Two CRITICAL alerts exist (1 unresolved + 1 unresolved)");

            List<AlertEvent> lowAlerts = alertEventRepository.findBySeverity(AlertSeverity.LOW);
            assertEquals(1, lowAlerts.size(), "One LOW alert exists (resolved)");

            List<AlertEvent> mediumAlerts = alertEventRepository.findBySeverity(AlertSeverity.MEDIUM);
            assertEquals(1, mediumAlerts.size(), "One MEDIUM alert exists (resolved)");
        }

        @Test
        @DisplayName("findBySeverityAndResolvedFalse returns only unresolved alerts of given severity")
        void testFindBySeverityAndUnresolved() {
            List<AlertEvent> highUnresolved =
                    alertEventRepository.findBySeverityAndResolvedFalse(AlertSeverity.HIGH);
            assertEquals(1, highUnresolved.size());
            assertEquals(EventType.MISMATCH, highUnresolved.get(0).getEventType());

            // LOW has one alert but it's resolved — should return empty
            List<AlertEvent> lowUnresolved =
                    alertEventRepository.findBySeverityAndResolvedFalse(AlertSeverity.LOW);
            assertTrue(lowUnresolved.isEmpty(), "No unresolved LOW alerts exist");
        }

        @Test
        @DisplayName("countBySeverity returns accurate count per severity level")
        void testCountBySeverity() {
            assertEquals(2L, alertEventRepository.countBySeverity(AlertSeverity.CRITICAL));
            assertEquals(1L, alertEventRepository.countBySeverity(AlertSeverity.HIGH));
            assertEquals(1L, alertEventRepository.countBySeverity(AlertSeverity.MEDIUM));
            assertEquals(1L, alertEventRepository.countBySeverity(AlertSeverity.LOW));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. Event-Type Filter Queries
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("S2-T3: Event-Type Filter Queries")
    class EventTypeFilterTests {

        @Test
        @DisplayName("findByEventType returns all alerts of a given event classification")
        void testFindByEventType() {
            List<AlertEvent> mismatches = alertEventRepository.findByEventType(EventType.MISMATCH);
            assertEquals(2, mismatches.size(), "Two MISMATCH alerts exist");

            List<AlertEvent> missingFile = alertEventRepository.findByEventType(EventType.MISSING_FILE);
            assertEquals(1, missingFile.size());

            List<AlertEvent> sigInvalid = alertEventRepository.findByEventType(EventType.SIGNATURE_INVALID);
            assertEquals(1, sigInvalid.size());

            List<AlertEvent> unauthorized = alertEventRepository.findByEventType(EventType.UNAUTHORIZED_MODIFICATION);
            assertEquals(1, unauthorized.size());
        }

        @Test
        @DisplayName("findByEventTypeAndSeverity returns alerts matching both event type and severity")
        void testFindByEventTypeAndSeverity() {
            // MISMATCH + HIGH = alert2
            List<AlertEvent> mismatchHigh =
                    alertEventRepository.findByEventTypeAndSeverity(EventType.MISMATCH, AlertSeverity.HIGH);
            assertEquals(1, mismatchHigh.size());
            assertEquals("/etc/passwd", mismatchHigh.get(0).getFilePath());

            // MISMATCH + LOW = alert5
            List<AlertEvent> mismatchLow =
                    alertEventRepository.findByEventTypeAndSeverity(EventType.MISMATCH, AlertSeverity.LOW);
            assertEquals(1, mismatchLow.size());
            assertTrue(mismatchLow.get(0).isResolved());

            // MISMATCH + CRITICAL = none
            List<AlertEvent> mismatchCritical =
                    alertEventRepository.findByEventTypeAndSeverity(EventType.MISMATCH, AlertSeverity.CRITICAL);
            assertTrue(mismatchCritical.isEmpty(), "No MISMATCH+CRITICAL combination exists");
        }

        @Test
        @DisplayName("countByEventType returns accurate count per event classification")
        void testCountByEventType() {
            assertEquals(2L, alertEventRepository.countByEventType(EventType.MISMATCH));
            assertEquals(1L, alertEventRepository.countByEventType(EventType.UNAUTHORIZED_MODIFICATION));
            assertEquals(1L, alertEventRepository.countByEventType(EventType.MISSING_FILE));
            assertEquals(1L, alertEventRepository.countByEventType(EventType.SIGNATURE_INVALID));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. Date-Range Queries
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("S2-T3: Date-Range Queries")
    class DateRangeTests {

        @Test
        @DisplayName("findByDetectedAtBetween returns alerts within the given time window")
        void testFindByDateRange() {
            LocalDateTime rangeStart = LocalDateTime.of(2026, 11, 1, 0, 0);
            LocalDateTime rangeEnd = LocalDateTime.of(2026, 11, 3, 23, 59);

            List<AlertEvent> inRange = alertEventRepository.findByDetectedAtBetween(rangeStart, rangeEnd);
            assertEquals(3, inRange.size(), "Three alerts fall within Nov 1-3, 2026");
        }

        @Test
        @DisplayName("findByDetectedAtBetween returns empty for a window with no alerts")
        void testFindByDateRangeEmpty() {
            LocalDateTime rangeStart = LocalDateTime.of(2026, 12, 1, 0, 0);
            LocalDateTime rangeEnd = LocalDateTime.of(2026, 12, 31, 23, 59);

            List<AlertEvent> inRange = alertEventRepository.findByDetectedAtBetween(rangeStart, rangeEnd);
            assertTrue(inRange.isEmpty(), "No alerts exist in December 2026");
        }

        @Test
        @DisplayName("findByDetectedAtBetween includes boundary timestamps")
        void testDateRangeBoundaryInclusion() {
            // Exact timestamp of alert1: 2026-11-01T10:00
            LocalDateTime exactStart = LocalDateTime.of(2026, 11, 1, 10, 0);
            LocalDateTime exactEnd = LocalDateTime.of(2026, 11, 1, 10, 0);

            List<AlertEvent> boundary = alertEventRepository.findByDetectedAtBetween(exactStart, exactEnd);
            assertEquals(1, boundary.size(), "Exact boundary match should return the alert");
            assertEquals(EventType.UNAUTHORIZED_MODIFICATION, boundary.get(0).getEventType());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. Per-File Alert Lookups
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("S2-T3: Per-File Alert Lookups")
    class PerFileLookupTests {

        @Test
        @DisplayName("findByWatchedFileOrderByDetectedAtDesc returns alerts for a specific WatchedFile entity")
        void testFindByWatchedFile() {
            List<AlertEvent> passwdAlerts =
                    alertEventRepository.findByWatchedFileOrderByDetectedAtDesc(filePasswd);
            assertEquals(3, passwdAlerts.size(), "Three alerts linked to /etc/passwd");
            // Verify newest-first ordering
            assertTrue(passwdAlerts.get(0).getDetectedAt()
                    .isAfter(passwdAlerts.get(1).getDetectedAt()));

            List<AlertEvent> hostsAlerts =
                    alertEventRepository.findByWatchedFileOrderByDetectedAtDesc(fileHosts);
            assertEquals(2, hostsAlerts.size(), "Two alerts linked to /etc/hosts");
        }

        @Test
        @DisplayName("findByFilePathOrderByDetectedAtDesc returns alerts by file path string")
        void testFindByFilePath() {
            List<AlertEvent> passwdAlerts =
                    alertEventRepository.findByFilePathOrderByDetectedAtDesc("/etc/passwd");
            assertEquals(3, passwdAlerts.size());

            List<AlertEvent> hostsAlerts =
                    alertEventRepository.findByFilePathOrderByDetectedAtDesc("/etc/hosts");
            assertEquals(2, hostsAlerts.size());

            // Non-existent path
            List<AlertEvent> noAlerts =
                    alertEventRepository.findByFilePathOrderByDetectedAtDesc("/nonexistent/file");
            assertTrue(noAlerts.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 6. Paginated Queries
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("S2-T3: Paginated Queries")
    class PaginationTests {

        @Test
        @DisplayName("findByResolvedFalse(Pageable) returns paginated unresolved alerts")
        void testPaginatedUnresolvedAlerts() {
            PageRequest pageRequest = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "detectedAt"));

            Page<AlertEvent> page = alertEventRepository.findByResolvedFalse(pageRequest);
            assertEquals(3, page.getTotalElements(), "Total unresolved = 3");
            assertEquals(2, page.getTotalPages(), "Two pages of size 2");
            assertEquals(2, page.getContent().size(), "First page has 2 items");

            // Second page
            Page<AlertEvent> page2 = alertEventRepository.findByResolvedFalse(
                    PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "detectedAt")));
            assertEquals(1, page2.getContent().size(), "Second page has 1 item");
        }

        @Test
        @DisplayName("findBySeverity(severity, Pageable) returns paginated severity-filtered alerts")
        void testPaginatedSeverityFilter() {
            PageRequest pageRequest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "detectedAt"));

            Page<AlertEvent> criticalPage = alertEventRepository.findBySeverity(AlertSeverity.CRITICAL, pageRequest);
            assertEquals(2, criticalPage.getTotalElements(), "Two CRITICAL alerts total");
            assertEquals(2, criticalPage.getTotalPages(), "Two pages of size 1");
            assertEquals(1, criticalPage.getContent().size());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 7. Recent Alerts & Edge Cases
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("S2-T3: Recent Alerts & Edge Cases")
    class RecentAndEdgeCaseTests {

        @Test
        @DisplayName("findTop50ByOrderByDetectedAtDesc returns all 5 seeded alerts in descending order")
        void testFindTop50Recent() {
            List<AlertEvent> recent = alertEventRepository.findTop50ByOrderByDetectedAtDesc();
            assertEquals(5, recent.size(), "All 5 seeded alerts fit within Top 50");
            // Verify descending order
            for (int i = 0; i < recent.size() - 1; i++) {
                assertTrue(
                        recent.get(i).getDetectedAt().isAfter(recent.get(i + 1).getDetectedAt())
                                || recent.get(i).getDetectedAt().isEqual(recent.get(i + 1).getDetectedAt()),
                        "Alerts must be in descending detectedAt order");
            }
        }

        @Test
        @DisplayName("Queries return empty lists (not null) when no matches exist")
        void testEmptyResultsAreNotNull() {
            // Use a severity/event-type combination that doesn't exist in seed data
            List<AlertEvent> empty = alertEventRepository.findByEventTypeAndSeverity(
                    EventType.MISSING_FILE, AlertSeverity.LOW);
            assertNotNull(empty, "Query must return empty list, not null");
            assertTrue(empty.isEmpty());

            List<AlertEvent> emptyFile = alertEventRepository.findByFilePathOrderByDetectedAtDesc("/does/not/exist");
            assertNotNull(emptyFile);
            assertTrue(emptyFile.isEmpty());
        }
    }
}
