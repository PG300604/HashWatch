package com.hashwatch.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a file registered for integrity monitoring (Sprint 1: S1-T4).
 */
@Entity
@Table(
    name = "watched_files",
    indexes = {
        @Index(name = "idx_watched_files_path", columnList = "file_path", unique = true),
        @Index(name = "idx_watched_files_active", columnList = "is_active"),
        @Index(name = "idx_watched_files_status", columnList = "status")
    }
)
public class WatchedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "File path must not be blank")
    @Column(name = "file_path", nullable = false, unique = true, length = 1024)
    private String filePath;

    @PositiveOrZero(message = "File size must be zero or positive")
    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @NotNull(message = "File status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FileStatus status = FileStatus.UNTRACKED;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WatchedFile() {}

    public WatchedFile(String filePath, Long fileSize, LocalDateTime lastModified, FileStatus status) {
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.status = (status != null) ? status : FileStatus.UNTRACKED;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = FileStatus.UNTRACKED;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
