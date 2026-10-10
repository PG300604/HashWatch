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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * =============================================================================
 * DOMAIN: Backend
 * ASSIGNED TO: Priyanshu (Sprint 2: S2-T1)
 * FOLDER / TARGET: src/main/java/com/hashwatch/service/ComparisonService.java
 * DOC TO UPDATE: docs/DFD.md (Process 1.0 & Process 2.0), docs/TRD.md
 * =============================================================================
 *
 * Core verification engine that compares disk files against cryptographically
 * signed baselines and updates file status / logs alerts upon mismatch or tampering.
 *
 * Implements:
 * 1. Triple-Lock Canonical Envelope: Domain tag + Normalized Path + SHA-256 + Size
 * 2. Fingerprint-Pinned Model: Verifies public key fingerprint before signature check
 * 3. 5-State Integrity Machine: UNTRACKED, VERIFIED, TAMPERED, MISSING, SIGNATURE_INVALID
 * 4. State-Transition Alert Gating: Deduplicates alert spam during periodic scans
 * 5. Batch Verification Fault Isolation: Resilient scan execution across watched files
 */
@Service
public class ComparisonService {

    private static final Logger log = LoggerFactory.getLogger(ComparisonService.class);

    private final HashingService hashingService;
    private final SigningService signingService;
    private final WatchedFileRepository watchedFileRepository;
    private final BaselineEntryRepository baselineEntryRepository;
    private final AlertEventRepository alertEventRepository;

    public ComparisonService(HashingService hashingService,
                             SigningService signingService,
                             WatchedFileRepository watchedFileRepository,
                             BaselineEntryRepository baselineEntryRepository,
                             AlertEventRepository alertEventRepository) {
        this.hashingService = hashingService;
        this.signingService = signingService;
        this.watchedFileRepository = watchedFileRepository;
        this.baselineEntryRepository = baselineEntryRepository;
        this.alertEventRepository = alertEventRepository;
    }

    /**
     * Builds the canonical "Triple-Lock" envelope payload to be signed by Ed25519.
     * Format: HashWatch:v1:<normalizedPath>:<sha256Hex>:<fileSizeBytes>
     *
     * @param filePath raw file path
     * @param sha256Hash 64-character hex hash
     * @param fileSize size in bytes
     * @return normalized canonical payload string
     */
    public String buildCanonicalPayload(String filePath, String sha256Hash, long fileSize) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null when building canonical payload");
        }
        String normalizedPath = Paths.get(filePath).toAbsolutePath().normalize().toString().replace('\\', '/');
        return String.format("HashWatch:v1:%s:%s:%d", normalizedPath, sha256Hash, fileSize);
    }

    /**
     * Creates or updates a cryptographic baseline for a given watched file.
     * Computes streaming SHA-256, signs the Triple-Lock envelope, retires any
     * previous active baseline (isCurrent = false), and marks the file VERIFIED.
     *
     * @param watchedFile the file entity to establish baseline for
     * @return the newly created and persisted BaselineEntry
     * @throws IOException if file cannot be read
     * @throws GeneralSecurityException if cryptographic signing fails
     */
    @Transactional
    public BaselineEntry establishBaseline(WatchedFile watchedFile) throws IOException, GeneralSecurityException {
        if (watchedFile == null || watchedFile.getFilePath() == null) {
            throw new IllegalArgumentException("WatchedFile and filePath must not be null");
        }

        Path path = Paths.get(watchedFile.getFilePath());
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new FileNotFoundException("Cannot establish baseline: file does not exist or is not a regular file: "
                    + watchedFile.getFilePath());
        }

        // 1. Stream 64 KB buffered SHA-256
        String sha256Hash = hashingService.hashFile(path.toFile());
        long fileSize = Files.size(path);
        LocalDateTime lastModified = LocalDateTime.ofInstant(
                Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault()
        );

        // 2. Build Triple-Lock canonical envelope and sign
        String canonicalPayload = buildCanonicalPayload(watchedFile.getFilePath(), sha256Hash, fileSize);
        String signature = signingService.sign(canonicalPayload);
        String publicKeyId = signingService.getKeyFingerprint();

        // 3. Retire previous active baseline to preserve forensic history
        Optional<BaselineEntry> existingActive = baselineEntryRepository.findByWatchedFileAndCurrentTrue(watchedFile);
        if (existingActive.isPresent()) {
            BaselineEntry oldBaseline = existingActive.get();
            oldBaseline.setCurrent(false);
            baselineEntryRepository.save(oldBaseline);
            log.info("Retired previous baseline ID {} for {}", oldBaseline.getId(), watchedFile.getFilePath());
        }

        // 4. Persist new BaselineEntry (isCurrent = true)
        BaselineEntry newBaseline = new BaselineEntry(watchedFile, sha256Hash, signature, publicKeyId);
        newBaseline.setCurrent(true);
        newBaseline = baselineEntryRepository.save(newBaseline);

        // 5. Update WatchedFile entity attributes
        watchedFile.setFileSize(fileSize);
        watchedFile.setLastModified(lastModified);
        watchedFile.setStatus(FileStatus.VERIFIED);
        watchedFileRepository.save(watchedFile);

        log.info("Established new cryptographic baseline ID {} for file: {} (hash: {})",
                newBaseline.getId(), watchedFile.getFilePath(), sha256Hash);

        return newBaseline;
    }

    /**
     * Scans and verifies all active watched files.
     * Execution wraps each file in an isolated try-catch block so transient file-locks
     * or permissions errors on one file never abort the verification of other files.
     */
    public void runVerificationScan() {
        List<WatchedFile> activeFiles = watchedFileRepository.findByActiveTrue();
        log.info("Running integrity verification scan across {} active watched files", activeFiles.size());

        for (WatchedFile file : activeFiles) {
            try {
                verifyFile(file);
            } catch (Exception e) {
                log.warn("Error during verification scan for file {}: {}", file.getFilePath(), e.getMessage());
            }
        }

        log.info("Completed integrity verification scan.");
    }

    /**
     * Verifies a single file against its active cryptographic baseline.
     * Enforces the Zero-Trust verification flow:
     * 1. Baseline existence check (UNTRACKED if absent)
     * 2. Disk existence check (MISSING + CRITICAL alert if missing)
     * 3. Ed25519 signature & fingerprint verification (SIGNATURE_INVALID + CRITICAL alert if tampered DB)
     * 4. Current disk hash comparison (VERIFIED on match, TAMPERED + HIGH alert on mismatch)
     *
     * @param fileEntity the WatchedFile to verify
     */
    @Transactional
    public void verifyFile(WatchedFile fileEntity) {
        if (fileEntity == null || !fileEntity.isActive()) {
            log.debug("Skipping verification for null or inactive file entity");
            return;
        }

        // 1. Fetch current active baseline
        Optional<BaselineEntry> baselineOpt = baselineEntryRepository.findByWatchedFileAndCurrentTrue(fileEntity);
        if (baselineOpt.isEmpty()) {
            log.warn("File {} is marked active but has no active baseline entry. Marking UNTRACKED.",
                    fileEntity.getFilePath());
            fileEntity.setStatus(FileStatus.UNTRACKED);
            watchedFileRepository.save(fileEntity);
            return;
        }

        BaselineEntry baseline = baselineOpt.get();
        Path path = Paths.get(fileEntity.getFilePath());

        // 2. Check if file exists on disk
        if (!Files.exists(path)) {
            log.error("Watched file is MISSING from filesystem: {}", fileEntity.getFilePath());
            fileEntity.setStatus(FileStatus.MISSING);
            watchedFileRepository.save(fileEntity);
            createAlertIfNotPresent(
                    fileEntity,
                    EventType.MISSING_FILE,
                    AlertSeverity.CRITICAL,
                    baseline.getSha256Hash(),
                    null,
                    "Monitored file is missing from disk: " + fileEntity.getFilePath()
            );
            return;
        }

        // 3. Zero-Trust Verification: Verify Ed25519 signature & public key fingerprint FIRST
        long recordedSize = (fileEntity.getFileSize() != null) ? fileEntity.getFileSize() : 0L;
        String canonicalPayload = buildCanonicalPayload(fileEntity.getFilePath(), baseline.getSha256Hash(), recordedSize);

        boolean signatureValid;
        try {
            signatureValid = signingService.verify(canonicalPayload, baseline.getSignature(), baseline.getPublicKeyId());
        } catch (Exception e) {
            log.error("Cryptographic verification failure for {}: {}", fileEntity.getFilePath(), e.getMessage());
            signatureValid = false;
        }

        if (!signatureValid) {
            log.error("DATABASE TAMPERING DETECTED! Ed25519 signature verification failed for baseline ID {} of file: {}",
                    baseline.getId(), fileEntity.getFilePath());
            fileEntity.setStatus(FileStatus.SIGNATURE_INVALID);
            watchedFileRepository.save(fileEntity);
            createAlertIfNotPresent(
                    fileEntity,
                    EventType.SIGNATURE_INVALID,
                    AlertSeverity.CRITICAL,
                    baseline.getSha256Hash(),
                    null,
                    "Baseline digital signature verification failed! Possible database tampering or rogue key substitution: "
                            + fileEntity.getFilePath()
            );
            return; // Never trust an unverified baseline to compare disk bytes against
        }

        // 4. Stream current file hash from disk
        String currentHash;
        long currentSize;
        try {
            currentHash = hashingService.hashFile(path.toFile());
            currentSize = Files.size(path);
        } catch (IOException e) {
            log.warn("Transient I/O error or file lock while accessing {}: {}", fileEntity.getFilePath(), e.getMessage());
            return; // Retain current status during transient I/O lock
        }

        // 5. Compare current hash with authentic baseline hash
        if (currentHash.equalsIgnoreCase(baseline.getSha256Hash())) {
            fileEntity.setStatus(FileStatus.VERIFIED);
            fileEntity.setFileSize(currentSize);
            try {
                fileEntity.setLastModified(LocalDateTime.ofInstant(
                        Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault()
                ));
            } catch (IOException ignored) {}
            watchedFileRepository.save(fileEntity);
            log.debug("File integrity VERIFIED for: {}", fileEntity.getFilePath());
        } else {
            log.error("FILE TAMPERING DETECTED! Current hash ({}) does not match expected baseline ({}) for: {}",
                    currentHash, baseline.getSha256Hash(), fileEntity.getFilePath());
            fileEntity.setStatus(FileStatus.TAMPERED);
            watchedFileRepository.save(fileEntity);
            createAlertIfNotPresent(
                    fileEntity,
                    EventType.UNAUTHORIZED_MODIFICATION,
                    AlertSeverity.HIGH,
                    baseline.getSha256Hash(),
                    currentHash,
                    "File integrity violation! Current hash differs from verified baseline for: " + fileEntity.getFilePath()
            );
        }
    }

    /**
     * State-transition alert deduplicator:
     * Only persists a new AlertEvent if no unresolved alert with the same eventType
     * already exists for this file path. This prevents alert fatigue in 30s polling loops.
     */
    private void createAlertIfNotPresent(WatchedFile watchedFile,
                                         EventType eventType,
                                         AlertSeverity severity,
                                         String expectedHash,
                                         String actualHash,
                                         String message) {
        boolean alertAlreadyPending = alertEventRepository.findByResolvedFalseOrderByDetectedAtDesc()
                .stream()
                .anyMatch(a -> a.getFilePath().equals(watchedFile.getFilePath()) && a.getEventType() == eventType);

        if (!alertAlreadyPending) {
            AlertEvent alert = new AlertEvent(
                    watchedFile,
                    watchedFile.getFilePath(),
                    eventType,
                    severity,
                    expectedHash,
                    actualHash,
                    message
            );
            alertEventRepository.save(alert);
            log.warn("Created new AlertEvent [{} | {}] for file {}", severity, eventType, watchedFile.getFilePath());
        } else {
            log.debug("Active unresolved alert for {} already exists; skipping duplicate alert creation",
                    watchedFile.getFilePath());
        }
    }
}
