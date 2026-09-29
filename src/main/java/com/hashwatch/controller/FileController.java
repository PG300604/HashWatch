package com.hashwatch.controller;

import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.ComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 * DOMAIN: API
 * ASSIGNED TO: Riya (Sprint 3)
 * FOLDER / TARGET: src/main/java/com/hashwatch/controller/FileController.java
 * DOC TO UPDATE: docs/TRD.md (Section 5.1 REST API Specs)
 * =============================================================================
 *
 * Task Description:
 * Expose RESTful endpoints for registering, listing, and removing files
 * from the active file integrity monitoring watchlist.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final WatchedFileRepository watchedFileRepository;
    private final ComparisonService comparisonService;

    public FileController(WatchedFileRepository watchedFileRepository, ComparisonService comparisonService) {
        this.watchedFileRepository = watchedFileRepository;
        this.comparisonService = comparisonService;
    }

    /**
     * GET /api/files - List all watched files.
     */
    @GetMapping
    public List<WatchedFile> listFiles() {
        // TODO [Sprint 3 - API]: Assigned to Riya
        return watchedFileRepository.findAll();
    }

    /**
     * POST /api/files - Register a new file to watch and establish its baseline.
     */
    @PostMapping
    public ResponseEntity<?> registerFile(@RequestBody Map<String, String> payload) {
        // TODO [Sprint 3 - API]: Assigned to Riya
        // 1. Extract 'filePath' from payload and validate existence on disk.
        // 2. Persist WatchedFile entity.
        // 3. Call comparisonService.establishBaseline(file).
        // 4. Return ResponseEntity with 200 OK or appropriate 400 Bad Request error.
        String filePath = payload.get("filePath");
        return ResponseEntity.ok(Map.of("message", "TODO: Implement file registration in Sprint 3", "filePath", String.valueOf(filePath)));
    }

    /**
     * DELETE /api/files/{id} - Deactivate a watched file from monitoring.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> unwatchFile(@PathVariable Long id) {
        // TODO [Sprint 3 - API]: Assigned to Riya
        // Soft delete: set active = false and save.
        return ResponseEntity.ok(Map.of("message", "TODO: Implement unwatchFile in Sprint 3", "id", id));
    }
}
