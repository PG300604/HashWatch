package com.hashwatch.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a cryptographically signed SHA-256 baseline snapshot (Sprint 1: S1-T4).
 */
@Entity
@Table(
    name = "baseline_entries",
    indexes = {
        @Index(name = "idx_baseline_current", columnList = "watched_file_id, is_current"),
        @Index(name = "idx_baseline_created_at", columnList = "created_at")
    }
)
public class BaselineEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Associated watched file is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "watched_file_id", nullable = false, foreignKey = @ForeignKey(name = "fk_baseline_watched_file"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private WatchedFile watchedFile;

    @NotBlank(message = "SHA-256 hash is required")
    @Size(min = 64, max = 64, message = "SHA-256 hex digest must be exactly 64 characters")
    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @NotBlank(message = "Ed25519 signature is required")
    @Column(name = "signature", nullable = false, length = 512)
    private String signature; // Ed25519 signature encoded in Base64

    @NotBlank(message = "Public key identifier is required")
    @Column(name = "public_key_id", nullable = false, length = 128)
    private String publicKeyId;

    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BaselineEntry() {}

    public BaselineEntry(WatchedFile watchedFile, String sha256Hash, String signature, String publicKeyId) {
        this.watchedFile = watchedFile;
        this.sha256Hash = sha256Hash;
        this.signature = signature;
        this.publicKeyId = publicKeyId;
        this.current = true;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
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

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
