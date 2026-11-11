package com.hashwatch.controller;

import com.hashwatch.dto.ApiResponse;
import com.hashwatch.dto.BatchRegistrationResult;
import com.hashwatch.dto.FileRegistrationRequest;
import com.hashwatch.entity.FileStatus;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.ComparisonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * =============================================================================
 * DOMAIN: API
 * ASSIGNED TO: Priyanshu (Sprint A: SA-2)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/FileController.java
 * DOC TO UPDATE: docs/TRD.md (Section 5.1 REST API Specs)
 * =============================================================================
 *
 * RESTful controller exposing endpoints for registering, listing, inspecting,
 * and deactivating/removing files from active integrity monitoring.
 *
 * Architectural Features (Sprint A Enhancements):
 * 1. Batch & Single-file Registration Support.
 * 2. Strict OS Canonicalization + Existence + isRegularFile + isReadable permission checks.
 * 3. 100 MB Synchronous Baseline Safety Cap to avoid Tomcat thread exhaustion.
 * 4. Intelligent Reactivation of soft-deleted files preserving database unique constraints.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    /**
     * Maximum file size allowed for synchronous SHA-256 digest calculation & Ed25519 signing (100 MB).
     * Prevents HTTP connection timeouts and worker thread exhaustion on large multi-GB binaries.
     */
    public static final long MAX_SYNC_REGISTRATION_SIZE_BYTES = 100L * 1024 * 1024;

    private final WatchedFileRepository watchedFileRepository;
    private final ComparisonService comparisonService;

    public FileController(WatchedFileRepository watchedFileRepository, ComparisonService comparisonService) {
        this.watchedFileRepository = watchedFileRepository;
        this.comparisonService = comparisonService;
    }

    /**
     * GET /api/files - List watched files.
     *
     * @param activeOnly optional filter: when true, returns only active monitoring targets
     * @return 200 OK with list of WatchedFile records
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WatchedFile>>> listFiles(
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly) {
        List<WatchedFile> files = activeOnly
                ? watchedFileRepository.findByActiveTrue()
                : watchedFileRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(files, "Retrieved " + files.size() + " watched file(s)"));
    }

    /**
     * GET /api/files/{id} - Retrieve details for a specific watched file.
     *
     * @param id watched file database ID
     * @return 200 OK with WatchedFile entity, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WatchedFile>> getFileById(@PathVariable Long id) {
        WatchedFile file = watchedFileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Watched file not found with ID: " + id));
        return ResponseEntity.ok(ApiResponse.success(file));
    }

    /**
     * POST /api/files - Register a single file for monitoring and establish initial cryptographic baseline.
     *
     * @param request JSON payload containing filePath
     * @return 201 Created for new file, or 200 OK for reactivated file
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WatchedFile>> registerFile(
            @RequestBody FileRegistrationRequest request) throws IOException, GeneralSecurityException {

        if (request == null || request.getFilePath() == null || request.getFilePath().trim().isEmpty()) {
            throw new IllegalArgumentException("File path must not be blank");
        }

        RegistrationOutcome outcome = registerOrReactivateSingleFile(request.getFilePath().trim());
        HttpStatus status = outcome.isReactivated() ? HttpStatus.OK : HttpStatus.CREATED;
        String message = outcome.isReactivated()
                ? "File reactivated and cryptographic baseline established"
                : "File registered and cryptographic baseline established";

        return ResponseEntity.status(status).body(ApiResponse.success(outcome.getWatchedFile(), message));
    }

    /**
     * POST /api/files/batch - Batch register multiple files into monitoring.
     * Processes each file with fault isolation so one failing path does not abort the entire batch.
     *
     * @param request JSON payload containing list of filePaths
     * @return 200 OK with BatchRegistrationResult summary
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<BatchRegistrationResult>> registerFilesBatch(
            @RequestBody FileRegistrationRequest request) {

        List<String> pathsToProcess = new ArrayList<>();
        if (request != null) {
            if (request.getFilePaths() != null && !request.getFilePaths().isEmpty()) {
                pathsToProcess.addAll(request.getFilePaths());
            } else if (request.getFilePath() != null && !request.getFilePath().trim().isEmpty()) {
                pathsToProcess.add(request.getFilePath().trim());
            }
        }

        if (pathsToProcess.isEmpty()) {
            throw new IllegalArgumentException("Batch registration requires at least one valid filePath");
        }

        BatchRegistrationResult result = new BatchRegistrationResult();
        result.setTotalSubmitted(pathsToProcess.size());

        for (String rawPath : pathsToProcess) {
            try {
                if (rawPath == null || rawPath.trim().isEmpty()) {
                    result.addFailure("", "File path cannot be blank");
                    continue;
                }
                RegistrationOutcome outcome = registerOrReactivateSingleFile(rawPath.trim());
                result.addSuccess(outcome.getWatchedFile());
            } catch (Exception ex) {
                log.warn("Batch registration failed for path: {} - {}", rawPath, ex.getMessage());
                result.addFailure(rawPath, ex.getMessage());
            }
        }

        String summary = String.format("Batch completed: %d succeeded, %d failed out of %d",
                result.getSuccessCount(), result.getFailureCount(), result.getTotalSubmitted());
        return ResponseEntity.ok(ApiResponse.success(result, summary));
    }

    /**
     * DELETE /api/files/{id} - Remove or deactivate a file from active monitoring.
     *
     * @param id watched file database ID
     * @param hard when true, permanently deletes record and cascades to baselines; when false (default), soft-deactivates
     * @return 200 OK with status message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<WatchedFile>> unwatchFile(
            @PathVariable Long id,
            @RequestParam(name = "hard", defaultValue = "false") boolean hard) {

        WatchedFile watchedFile = watchedFileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Watched file not found with ID: " + id));

        if (hard) {
            watchedFileRepository.delete(watchedFile);
            log.info("Hard deleted watched file ID: {} ({})", id, watchedFile.getFilePath());
            return ResponseEntity.ok(ApiResponse.success(null, "File permanently deleted from monitoring"));
        } else {
            watchedFile.setActive(false);
            WatchedFile updated = watchedFileRepository.save(watchedFile);
            log.info("Soft-deactivated watched file ID: {} ({})", id, watchedFile.getFilePath());
            return ResponseEntity.ok(ApiResponse.success(updated, "File monitoring paused (deactivated)"));
        }
    }

    /**
     * PATCH /api/files/{id}/watch-status - Toggle monitoring active flag.
     *
     * @param id watched file database ID
     * @param active new active state
     * @return 200 OK with updated entity
     */
    @PatchMapping("/{id}/watch-status")
    public ResponseEntity<ApiResponse<WatchedFile>> updateWatchStatus(
            @PathVariable Long id,
            @RequestParam(name = "active") boolean active) {

        WatchedFile watchedFile = watchedFileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Watched file not found with ID: " + id));

        watchedFile.setActive(active);
        WatchedFile updated = watchedFileRepository.save(watchedFile);
        log.info("Updated watch status for file ID: {} to active={}", id, active);
        return ResponseEntity.ok(ApiResponse.success(updated, "Watch status updated to active=" + active));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private RegistrationOutcome registerOrReactivateSingleFile(String rawPathStr)
            throws IOException, GeneralSecurityException {

        Path rawPath = Paths.get(rawPathStr);
        Path canonicalPath = rawPath.toAbsolutePath().normalize();
        String normalizedPathStr = canonicalPath.toString().replace('\\', '/');

        // Validation 1: Physical existence check
        if (!Files.exists(canonicalPath)) {
            throw new FileNotFoundException("Cannot register file: file does not exist on disk: " + normalizedPathStr);
        }

        // Validation 2: Regular file check (rejects directories, device nodes)
        if (!Files.isRegularFile(canonicalPath)) {
            throw new IllegalArgumentException("Cannot register file: target path is not a regular file (e.g. directory or symlink): " + normalizedPathStr);
        }

        // Validation 3: Readability & Permissions check (Idea 2-A)
        if (!Files.isReadable(canonicalPath)) {
            throw new IllegalArgumentException("Cannot register file: target file exists on disk but HashWatch lacks OS read permissions: " + normalizedPathStr);
        }

        // Validation 4: Synchronous Size Threshold Guard (Idea 3-A)
        long fileSize = Files.size(canonicalPath);
        if (fileSize > MAX_SYNC_REGISTRATION_SIZE_BYTES) {
            throw new IllegalArgumentException(String.format(
                    "Cannot register file: size (%d MB) exceeds the 100 MB synchronous baseline registration threshold. Target: %s",
                    fileSize / (1024 * 1024), normalizedPathStr
            ));
        }

        LocalDateTime lastModified = LocalDateTime.ofInstant(
                Files.getLastModifiedTime(canonicalPath).toInstant(), ZoneId.systemDefault()
        );

        // Validation 5 & Intelligent Reactivation (Idea 4-A)
        Optional<WatchedFile> existingOpt = watchedFileRepository.findByFilePath(normalizedPathStr);
        if (existingOpt.isPresent()) {
            WatchedFile existing = existingOpt.get();
            if (existing.isActive()) {
                throw new IllegalStateException("File is already actively monitored with ID: " + existing.getId());
            }

            // Reactivate previously soft-deleted/paused file
            existing.setActive(true);
            existing.setStatus(FileStatus.UNTRACKED);
            existing.setFileSize(fileSize);
            existing.setLastModified(lastModified);
            existing = watchedFileRepository.save(existing);

            // Establish fresh cryptographic baseline
            comparisonService.establishBaseline(existing);
            log.info("Reactivated and re-baselined existing watched file: {}", normalizedPathStr);
            return new RegistrationOutcome(existing, true);
        }

        // Fresh registration
        WatchedFile newFile = new WatchedFile(normalizedPathStr, fileSize, lastModified, FileStatus.UNTRACKED);
        WatchedFile savedFile = watchedFileRepository.save(newFile);

        // Establish cryptographic baseline
        comparisonService.establishBaseline(savedFile);
        log.info("Registered new watched file: {} (ID: {})", normalizedPathStr, savedFile.getId());
        return new RegistrationOutcome(savedFile, false);
    }

    private static class RegistrationOutcome {
        private final WatchedFile watchedFile;
        private final boolean reactivated;

        public RegistrationOutcome(WatchedFile watchedFile, boolean reactivated) {
            this.watchedFile = watchedFile;
            this.reactivated = reactivated;
        }

        public WatchedFile getWatchedFile() {
            return watchedFile;
        }

        public boolean isReactivated() {
            return reactivated;
        }
    }
}
