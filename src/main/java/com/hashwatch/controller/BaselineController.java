package com.hashwatch.controller;

import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.ComparisonService;
import com.hashwatch.service.SigningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 * DOMAIN: API & Cryptology
 * ASSIGNED TO: Riya / Priyanshu (Sprint 3)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/BaselineController.java
 * DOC TO UPDATE: docs/TRD.md (Section 5.2 Baselines API)
 * =============================================================================
 *
 * Task Description:
 * Endpoints for managing cryptographic baselines, re-baselining files,
 * and exposing the Ed25519 public key.
 */
@RestController
@RequestMapping("/api/baselines")
public class BaselineController {

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
     * GET /api/baselines - List all currently active baseline records.
     */
    @GetMapping
    public List<BaselineEntry> getActiveBaselines() {
        // TODO [Sprint 3 - API]: Assigned to Riya / Priyanshu
        return baselineEntryRepository.findByCurrentTrue();
    }

    /**
     * GET /api/baselines/public-key - Expose the Ed25519 public key for external verification.
     */
    @GetMapping("/public-key")
    public Map<String, String> getPublicKey() {
        // TODO [Sprint 3 - Cryptology & API]: Assigned to Riya / Priyanshu
        return Map.of(
                "algorithm", "Ed25519",
                "publicKey", signingService.getPublicKeyBase64()
        );
    }

    /**
     * POST /api/baselines/generate/{fileId} - Regenerate and sign baseline for a specific file.
     */
    @PostMapping("/generate/{fileId}")
    public ResponseEntity<?> rebaselineFile(@PathVariable Long fileId) {
        // TODO [Sprint 3 - API]: Assigned to Riya / Priyanshu
        // 1. Fetch file by ID.
        // 2. Call comparisonService.establishBaseline(file).
        // 3. Return updated baseline.
        return ResponseEntity.ok(Map.of("message", "TODO: Implement rebaselineFile in Sprint 3", "fileId", fileId));
    }

    /**
     * POST /api/baselines/generate-all - Regenerate baselines for all active watched files.
     */
    @PostMapping("/generate-all")
    public ResponseEntity<?> rebaselineAll() {
        // TODO [Sprint 3 - API]: Assigned to Riya / Priyanshu
        return ResponseEntity.ok(Map.of("message", "TODO: Implement rebaselineAll in Sprint 3"));
    }
}
