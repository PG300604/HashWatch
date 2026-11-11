package com.hashwatch.repository;

import com.hashwatch.entity.AlertEvent;
import com.hashwatch.entity.AlertSeverity;
import com.hashwatch.entity.EventType;
import com.hashwatch.entity.WatchedFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * =============================================================================
 * DOMAIN: DBMS & API
 * ASSIGNED TO: Riya (Sprint 2: S2-T3)
 * TARGET: src/main/java/com/hashwatch/repository/AlertEventRepository.java
 * DOC TO UPDATE: docs/ERD.md (Table 3.3)
 * =============================================================================
 *
 * Spring Data JPA repository for {@link AlertEvent} entities.
 *
 * <p>Provides derived query methods for alert triage, severity filtering,
 * event-type classification, date-range searches, per-file alert lookups,
 * and paginated result sets needed by the REST API (Sprint 3) and
 * dashboard (Sprint 4).</p>
 *
 * <h3>Query Method Index</h3>
 * <ul>
 *   <li><b>Unresolved Alerts:</b> {@link #findByResolvedFalseOrderByDetectedAtDesc()},
 *       {@link #findByResolvedFalseAndSeverityOrderByDetectedAtDesc(AlertSeverity)}</li>
 *   <li><b>Severity Filters:</b> {@link #findBySeverity(AlertSeverity)},
 *       {@link #findBySeverityAndResolvedFalse(AlertSeverity)}</li>
 *   <li><b>Event-Type Filters:</b> {@link #findByEventType(EventType)},
 *       {@link #findByEventTypeAndSeverity(EventType, AlertSeverity)}</li>
 *   <li><b>Date-Range:</b> {@link #findByDetectedAtBetween(LocalDateTime, LocalDateTime)}</li>
 *   <li><b>Per-File:</b> {@link #findByWatchedFileOrderByDetectedAtDesc(WatchedFile)},
 *       {@link #findByFilePathOrderByDetectedAtDesc(String)}</li>
 *   <li><b>Pagination:</b> {@link #findByResolvedFalse(Pageable)},
 *       {@link #findBySeverity(AlertSeverity, Pageable)}</li>
 *   <li><b>Aggregation:</b> {@link #countByResolvedFalse()},
 *       {@link #countBySeverity(AlertSeverity)},
 *       {@link #countByEventType(EventType)}</li>
 * </ul>
 */
@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {

    // ── Existing queries (Sprint 1: S1-T4, preserved) ──────────────────────

    /** Returns all unresolved alerts, newest first. Used by ComparisonService alert gating. */
    List<AlertEvent> findByResolvedFalseOrderByDetectedAtDesc();

    /** Returns the 50 most recent alerts regardless of resolution status. */
    List<AlertEvent> findTop50ByOrderByDetectedAtDesc();

    /** Returns all alerts matching the given severity level. */
    List<AlertEvent> findBySeverity(AlertSeverity severity);

    /** Counts the number of currently unresolved alerts. */
    long countByResolvedFalse();

    // ── New queries (Sprint 2: S2-T3) ──────────────────────────────────────

    // --- Combined severity + resolution filters ---

    /**
     * Returns unresolved alerts of a specific severity, newest first.
     * Powers the dashboard triage view where operators filter by CRITICAL/HIGH.
     */
    List<AlertEvent> findByResolvedFalseAndSeverityOrderByDetectedAtDesc(AlertSeverity severity);

    /**
     * Alias: severity-first filter for unresolved alerts.
     * Returns all alerts matching severity that are still unresolved.
     */
    List<AlertEvent> findBySeverityAndResolvedFalse(AlertSeverity severity);

    // --- Event-type filters ---

    /** Returns all alerts of a given event type (e.g., MISMATCH, MISSING_FILE). */
    List<AlertEvent> findByEventType(EventType eventType);

    /** Returns alerts matching both event type and severity for fine-grained classification. */
    List<AlertEvent> findByEventTypeAndSeverity(EventType eventType, AlertSeverity severity);

    // --- Date-range queries ---

    /**
     * Returns alerts detected within the given time window (inclusive).
     * Supports time-bounded audit reports and periodic summaries.
     */
    List<AlertEvent> findByDetectedAtBetween(LocalDateTime start, LocalDateTime end);

    // --- Per-file alert lookups ---

    /**
     * Returns all alerts linked to a specific WatchedFile entity, newest first.
     * Used to display alert history on the file detail page.
     */
    List<AlertEvent> findByWatchedFileOrderByDetectedAtDesc(WatchedFile watchedFile);

    /**
     * Returns all alerts matching a file path snapshot, newest first.
     * Useful when the WatchedFile entity has been deleted (ON DELETE SET NULL)
     * but the file_path snapshot survives in the alert record.
     */
    List<AlertEvent> findByFilePathOrderByDetectedAtDesc(String filePath);

    // --- Paginated queries (REST API support for Sprint 3) ---

    /** Paginated view of unresolved alerts for the REST API. */
    Page<AlertEvent> findByResolvedFalse(Pageable pageable);

    /** Paginated view of alerts filtered by severity for the REST API. */
    Page<AlertEvent> findBySeverity(AlertSeverity severity, Pageable pageable);

    // --- Aggregation counts ---

    /** Counts alerts by severity level for dashboard summary cards. */
    long countBySeverity(AlertSeverity severity);

    /** Counts alerts by event type for classification analytics. */
    long countByEventType(EventType eventType);
}
