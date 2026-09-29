package com.hashwatch.service;

import com.hashwatch.entity.AlertEvent;
import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
     * Creates or updates a baseline for a given watched file.
     */
    @Transactional
    public BaselineEntry establishBaseline(WatchedFile watchedFile) throws IOException, GeneralSecurityException {
        File file = new File(watchedFile.getFilePath());
        if (!file.exists()) {
            throw new IllegalArgumentException("Cannot baseline non-existent file: " + watchedFile.getFilePath());
        }

        String hash = hashingService.hashFile(file);
        String signature = signingService.sign(hash);

        // Retire previous baseline entries
        Optional<BaselineEntry> existing = baselineEntryRepository.findByWatchedFileAndCurrentTrue(watchedFile);
        existing.ifPresent(b -> {
            b.setCurrent(false);
            baselineEntryRepository.save(b);
        });

        BaselineEntry baselineEntry = new BaselineEntry(
                watchedFile,
                hash,
                signature,
                "default-ed25519-key"
        );
        baselineEntryRepository.save(baselineEntry);

        watchedFile.setStatus("VERIFIED");
        watchedFile.setFileSize(file.length());
        watchedFile.setLastModified(LocalDateTime.now());
        watchedFileRepository.save(watchedFile);

        log.info("Established cryptographic baseline for file: {} (SHA-256: {})", watchedFile.getFilePath(), hash);
        return baselineEntry;
    }

    /**
     * Scans and verifies all active watched files.
     */
    @Transactional
    public void runVerificationScan() {
        List<WatchedFile> activeFiles = watchedFileRepository.findByActiveTrue();
        for (WatchedFile fileEntity : activeFiles) {
            verifyFile(fileEntity);
        }
    }

    /**
     * Verifies single file against baseline and logs alerts if tampered or missing.
     */
    @Transactional
    public void verifyFile(WatchedFile fileEntity) {
        File diskFile = new File(fileEntity.getFilePath());
        Optional<BaselineEntry> baselineOpt = baselineEntryRepository.findByWatchedFileAndCurrentTrue(fileEntity);

        if (baselineOpt.isEmpty()) {
            log.warn("No active baseline found for file: {}", fileEntity.getFilePath());
            fileEntity.setStatus("UNTRACKED");
            watchedFileRepository.save(fileEntity);
            return;
        }

        BaselineEntry baseline = baselineOpt.get();

        if (!diskFile.exists()) {
            fileEntity.setStatus("MISSING");
            watchedFileRepository.save(fileEntity);
            createAlert(fileEntity, "MISSING_FILE", "HIGH", baseline.getSha256Hash(), null,
                    "Watched file was removed or missing from disk: " + fileEntity.getFilePath());
            return;
        }

        try {
            // Step 1: Verify cryptographic signature of the baseline entry itself
            boolean sigValid = signingService.verify(baseline.getSha256Hash(), baseline.getSignature());
            if (!sigValid) {
                fileEntity.setStatus("SIGNATURE_INVALID");
                watchedFileRepository.save(fileEntity);
                createAlert(fileEntity, "SIGNATURE_INVALID", "CRITICAL", baseline.getSha256Hash(), null,
                        "Baseline cryptographic signature verification failed! Baseline may have been tampered with.");
                return;
            }

            // Step 2: Hash current file and compare
            String currentHash = hashingService.hashFile(diskFile);
            if (currentHash.equalsIgnoreCase(baseline.getSha256Hash())) {
                fileEntity.setStatus("VERIFIED");
                fileEntity.setFileSize(diskFile.length());
                fileEntity.setLastModified(LocalDateTime.now());
                watchedFileRepository.save(fileEntity);
            } else {
                fileEntity.setStatus("TAMPERED");
                fileEntity.setFileSize(diskFile.length());
                fileEntity.setLastModified(LocalDateTime.now());
                watchedFileRepository.save(fileEntity);

                createAlert(fileEntity, "UNAUTHORIZED_MODIFICATION", "CRITICAL",
                        baseline.getSha256Hash(), currentHash,
                        "File content altered! SHA-256 hash mismatch detected for " + fileEntity.getFilePath());
            }
        } catch (Exception e) {
            log.error("Error during verification scan of {}: {}", fileEntity.getFilePath(), e.getMessage());
        }
    }

    private void createAlert(WatchedFile fileEntity, String type, String severity,
                             String expectedHash, String actualHash, String message) {
        AlertEvent alert = new AlertEvent(fileEntity, fileEntity.getFilePath(), type, severity, expectedHash, actualHash, message);
        alertEventRepository.save(alert);
        log.warn("Alert generated [{} - {}]: {}", severity, type, message);
    }
}
