package com.hashwatch.controller;

import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.FileStatus;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.ComparisonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * =============================================================================
 * DOMAIN: API
 * ASSIGNED TO: Riya (Sprint 3: S3-T1)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/FileController.java
 * DOC TO UPDATE: docs/TRD.md (Section 5.1 REST API Specs)
 * =============================================================================
 *
 * RESTful controller exposing endpoints for registering, listing, and
 * deactivating monitored files in the HashWatch integrity watchlist.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li><b>GET /api/files:</b> List all registered files (optionally filtered by active state)</li>
 *   <li><b>GET /api/files/{id}:</b> Retrieve a single watched file record by primary key</li>
 *   <li><b>POST /api/files:</b> Register a new file, calculate SHA-256, and establish Ed25519 baseline</li>
 *   <li><b>DELETE /api/files/{id}:</b> Deactivate a watched file from monitoring (soft delete)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final WatchedFileRepository watchedFileRepository;
    private final ComparisonService comparisonService;

    public FileController(WatchedFileRepository watchedFileRepository, ComparisonService comparisonService) {
        this.watchedFileRepository = watchedFileRepository;
        this.comparisonService = comparisonService;
    }

    /**
     * GET /api/files - List monitored files.
     *
     * @param activeOnly optional boolean flag (default: false); when true, returns only active files
     * @return list of WatchedFile records
     */
    @GetMapping
    public List<WatchedFile> listFiles(@RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return watchedFileRepository.findByActiveTrue();
        }
        return watchedFileRepository.findAll();
    }

    /**
     * GET /api/files/{id} - Retrieve a single watched file record by ID.
     *
     * @param id the primary key of the WatchedFile
     * @return 200 OK with entity, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<WatchedFile> getFileById(@PathVariable Long id) {
        return watchedFileRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST /api/files - Register a new file to watch and establish its initial cryptographic baseline.
     *
     * <p>Expected JSON body:</p>
     * <pre>{"filePath": "/path/to/target.conf"}</pre>
     *
     * @param payload JSON object containing the required "filePath" key
     * @return 201 Created with metadata, 400 Bad Request if missing or non-existent, 409 Conflict if duplicate
     */
    @PostMapping
    public ResponseEntity<?> registerFile(@RequestBody Map<String, String> payload) {
        if (payload == null || !payload.containsKey("filePath")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", "Bad Request",
                    "message", "Missing required 'filePath' parameter in request body"
            ));
        }

        String rawPath = payload.get("filePath");
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", "Bad Request",
                    "message", "File path must not be empty or whitespace"
            ));
        }

        // 1. Resolve and normalize absolute file path
        Path normalizedPath = Paths.get(rawPath.trim()).toAbsolutePath().normalize();
        String normalizedPathStr = normalizedPath.toString().replace('\\', '/');

        // 2. Validate filesystem existence and file type
        if (!Files.exists(normalizedPath)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", "Bad Request",
                    "message", "File does not exist on disk: " + normalizedPathStr
            ));
        }

        if (!Files.isRegularFile(normalizedPath)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", "Bad Request",
                    "message", "Specified path is not a regular file (directories and device files cannot be watched directly): " + normalizedPathStr
            ));
        }

        // 3. Check for duplicates in repository
        Optional<WatchedFile> existingOpt = watchedFileRepository.findByFilePath(normalizedPathStr);
        if (existingOpt.isPresent()) {
            WatchedFile existing = existingOpt.get();
            if (existing.isActive()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "status", HttpStatus.CONFLICT.value(),
                        "error", "Conflict",
                        "message", "File is already being watched: " + normalizedPathStr,
                        "fileId", existing.getId()
                ));
            } else {
                // Reactivate previously unwatched file
                existing.setActive(true);
                existing.setStatus(FileStatus.UNTRACKED);
                try {
                    BaselineEntry newBaseline = comparisonService.establishBaseline(existing);
                    return ResponseEntity.ok(Map.of(
                            "message", "Reactivated monitoring for existing file and refreshed cryptographic baseline",
                            "file", existing,
                            "baseline", newBaseline
                    ));
                } catch (IOException | GeneralSecurityException e) {
                    log.error("Failed to establish baseline during file reactivation: {}", e.getMessage(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "error", "Internal Server Error",
                            "message", "Cryptographic baseline establishment failed: " + e.getMessage()
                    ));
                }
            }
        }

        // 4. Construct and persist new WatchedFile entity
        try {
            long fileSize = Files.size(normalizedPath);
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(normalizedPath).toInstant(), ZoneId.systemDefault()
            );

            WatchedFile newFile = new WatchedFile(normalizedPathStr, fileSize, lastModified, FileStatus.UNTRACKED);
            newFile.setActive(true);
            WatchedFile savedFile = watchedFileRepository.save(newFile);

            // 5. Establish initial cryptographic baseline (SHA-256 + Ed25519 signature)
            BaselineEntry initialBaseline = comparisonService.establishBaseline(savedFile);

            log.info("Registered file for monitoring: ID={} path={}", savedFile.getId(), normalizedPathStr);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "File successfully registered and initial cryptographic baseline established",
                    "file", savedFile,
                    "baseline", initialBaseline
            ));
        } catch (IOException | GeneralSecurityException e) {
            log.error("Error creating baseline for newly registered file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "error", "Internal Server Error",
                    "message", "Failed to calculate cryptographic baseline: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/files/{id} - Deactivate a watched file from monitoring (soft delete).
     *
     * @param id the primary key of the file to deactivate
     * @return 200 OK upon deactivation, or 404 Not Found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> unwatchFile(@PathVariable Long id) {
        return watchedFileRepository.findById(id)
                .map(file -> {
                    file.setActive(false);
                    watchedFileRepository.save(file);
                    log.info("Deactivated file ID {} ({}) from active monitoring", id, file.getFilePath());
                    return ResponseEntity.ok(Map.of(
                            "message", "File deactivated from monitoring successfully",
                            "fileId", id,
                            "active", false
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", HttpStatus.NOT_FOUND.value(),
                        "error", "Not Found",
                        "message", "WatchedFile with ID " + id + " not found"
                )));
    }
}
