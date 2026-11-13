package com.hashwatch.controller;

import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.ComparisonService;
import com.hashwatch.service.SigningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 * DOMAIN: API & Cryptology
 * ASSIGNED TO: Riya (Sprint 3: S3-T1)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/BaselineController.java
 * DOC TO UPDATE: docs/TRD.md (Section 5.2 Baselines API)
 * =============================================================================
 *
 * RESTful controller for managing cryptographic baselines, re-baselining
 * files upon intentional modification, and exposing public key material
 * for third-party audit verification.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li><b>GET /api/baselines:</b> List all currently active baseline records</li>
 *   <li><b>GET /api/baselines/public-key:</b> Export the Ed25519 public key and fingerprint</li>
 *   <li><b>POST /api/baselines/generate/{fileId}:</b> Regenerate and resign baseline for a file</li>
 *   <li><b>POST /api/baselines/generate-all:</b> Regenerate baselines for all active files</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/baselines")
public class BaselineController {

    private static final Logger log = LoggerFactory.getLogger(BaselineController.class);

    private final BaselineEntryRepository baselineEntryRepository;
    private final WatchedFileRepository watchedFileRepository;
    private final ComparisonService comparisonService;
    private final SigningService signingService;

    public BaselineController(BaselineEntryRepository baselineEntryRepository,
                              WatchedFileRepository watchedFileRepository,
                              ComparisonService comparisonService,
                              SigningService signingService) {
        this.baselineEntryRepository = baselineEntryRepository;
        this.watchedFileRepository = watchedFileRepository;
        this.comparisonService = comparisonService;
        this.signingService = signingService;
    }

    /**
     * GET /api/baselines - List all currently active cryptographic baseline records.
     *
     * @return list of active BaselineEntry entities
     */
    @GetMapping
    public List<BaselineEntry> getActiveBaselines() {
        return baselineEntryRepository.findByCurrentTrue();
    }

    /**
     * GET /api/baselines/public-key - Expose the SunEC Ed25519 public key for external verification.
     *
     * @return JSON object with algorithm name, Base64 X.509 public key, and SHA-256 fingerprint
     */
    @GetMapping("/public-key")
    public Map<String, String> getPublicKey() {
        return Map.of(
                "algorithm", "Ed25519",
                "format", "X.509 SubjectPublicKeyInfo (Base64)",
                "publicKey", signingService.getPublicKeyBase64(),
                "publicKeyPem", signingService.getPublicKeyPem(),
                "fingerprint", signingService.getKeyFingerprint()
        );
    }

    /**
     * POST /api/baselines/generate/{fileId} - Regenerate and cryptographically sign a new baseline
     * for a specific monitored file (e.g. after authorized administrative updates).
     *
     * @param fileId the primary key of the WatchedFile
     * @return 200 OK with the new BaselineEntry, or 404 Not Found
     */
    @PostMapping("/generate/{fileId}")
    public ResponseEntity<?> rebaselineFile(@PathVariable Long fileId) {
        return watchedFileRepository.findById(fileId)
                .map(file -> {
                    try {
                        BaselineEntry updatedBaseline = comparisonService.establishBaseline(file);
                        log.info("Manually re-baselined file ID {} ({})", fileId, file.getFilePath());
                        return ResponseEntity.ok(Map.of(
                                "message", "Cryptographic baseline regenerated and signed successfully",
                                "baseline", updatedBaseline
                        ));
                    } catch (IOException | GeneralSecurityException e) {
                        log.error("Failed to re-baseline file ID {}: {}", fileId, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "error", "Internal Server Error",
                                "message", "Re-baselining failed: " + e.getMessage()
                        ));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", HttpStatus.NOT_FOUND.value(),
                        "error", "Not Found",
                        "message", "WatchedFile with ID " + fileId + " not found"
                )));
    }

    /**
     * POST /api/baselines/generate-all - Regenerate and resign baselines for all active watched files.
     *
     * @return 200 OK with summary of successful and failed re-baselines
     */
    @PostMapping("/generate-all")
    public ResponseEntity<?> rebaselineAll() {
        List<WatchedFile> activeFiles = watchedFileRepository.findByActiveTrue();
        int successCount = 0;
        List<String> failedPaths = new ArrayList<>();

        for (WatchedFile file : activeFiles) {
            try {
                comparisonService.establishBaseline(file);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to re-baseline file {}: {}", file.getFilePath(), e.getMessage());
                failedPaths.add(file.getFilePath());
            }
        }

        log.info("Batch re-baselined {} / {} active files", successCount, activeFiles.size());
        return ResponseEntity.ok(Map.of(
                "message", "Batch re-baseline operation completed",
                "totalActive", activeFiles.size(),
                "successfullyBaselined", successCount,
                "failedFiles", failedPaths
        ));
    }
}
