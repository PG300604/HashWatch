package com.hashwatch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_events")
public class AlertEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watched_file_id")
    private WatchedFile watchedFile;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // 'MISMATCH', 'UNAUTHORIZED_MODIFICATION', 'MISSING_FILE', 'SIGNATURE_INVALID'

    @Column(name = "severity", nullable = false, length = 20)
    private String severity; // 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'

    @Column(name = "expected_hash", length = 64)
    private String expectedHash;

    @Column(name = "actual_hash", length = 64)
    private String actualHash;

    @Column(name = "message", length = 2048)
    private String message;

    @Column(name = "is_resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt = LocalDateTime.now();

    public AlertEvent() {}

    public AlertEvent(WatchedFile watchedFile, String filePath, String eventType, String severity,
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
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
