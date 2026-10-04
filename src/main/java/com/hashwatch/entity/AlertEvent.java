package com.hashwatch.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a security alert / integrity violation event (Sprint 1: S1-T4).
 */
@Entity
@Table(
    name = "alert_events",
    indexes = {
        @Index(name = "idx_alerts_unresolved", columnList = "is_resolved, detected_at"),
        @Index(name = "idx_alerts_severity", columnList = "severity")
    }
)
public class AlertEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watched_file_id", foreignKey = @ForeignKey(name = "fk_alert_watched_file"))
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private WatchedFile watchedFile;

    @NotBlank(message = "File path is required")
    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @NotNull(message = "Event type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @NotNull(message = "Alert severity is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "expected_hash", length = 64)
    private String expectedHash;

    @Column(name = "actual_hash", length = 64)
    private String actualHash;

    @Column(name = "message", length = 2048)
    private String message;

    @Column(name = "is_resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    public AlertEvent() {}

    public AlertEvent(WatchedFile watchedFile, String filePath, EventType eventType, AlertSeverity severity,
                      String expectedHash, String actualHash, String message) {
        this.watchedFile = watchedFile;
        this.filePath = filePath;
        this.eventType = eventType;
        this.severity = severity;
        this.expectedHash = expectedHash;
        this.actualHash = actualHash;
        this.message = message;
        this.resolved = false;
        this.detectedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.detectedAt == null) {
            this.detectedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WatchedFile getWatchedFile() {
        return watchedFile;
    }

    public void setWatchedFile(WatchedFile watchedFile) {
        this.watchedFile = watchedFile;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public String getExpectedHash() {
        return expectedHash;
    }

    public void setExpectedHash(String expectedHash) {
        this.expectedHash = expectedHash;
    }

    public String getActualHash() {
        return actualHash;
    }

    public void setActualHash(String actualHash) {
        this.actualHash = actualHash;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }
}
