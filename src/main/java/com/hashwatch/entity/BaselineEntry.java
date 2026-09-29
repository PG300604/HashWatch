package com.hashwatch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "baseline_entries")
public class BaselineEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watched_file_id", nullable = false)
    private WatchedFile watchedFile;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Column(name = "signature", nullable = false, length = 512)
    private String signature; // Ed25519 signature encoded in Base64 or Hex

    @Column(name = "public_key_id", nullable = false, length = 128)
    private String publicKeyId;

    @Column(name = "is_current", nullable = false)
    private boolean current = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public BaselineEntry() {}

    public BaselineEntry(WatchedFile watchedFile, String sha256Hash, String signature, String publicKeyId) {
        this.watchedFile = watchedFile;
        this.sha256Hash = sha256Hash;
        this.signature = signature;
        this.publicKeyId = publicKeyId;
        this.current = true;
        this.createdAt = LocalDateTime.now();
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
