package com.hashwatch.controller;

import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.ComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final WatchedFileRepository watchedFileRepository;
    private final ComparisonService comparisonService;

    public FileController(WatchedFileRepository watchedFileRepository, ComparisonService comparisonService) {
        this.watchedFileRepository = watchedFileRepository;
        this.comparisonService = comparisonService;
    }

    @GetMapping
    public List<WatchedFile> listFiles() {
        return watchedFileRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> registerFile(@RequestBody Map<String, String> payload) {
        String filePath = payload.get("filePath");
        if (filePath == null || filePath.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "filePath is required"));
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File does not exist: " + filePath));
        }

        WatchedFile watchedFile = watchedFileRepository.findByFilePath(filePath)
                .orElse(new WatchedFile(filePath, file.length(), LocalDateTime.now(), "UNTRACKED"));

        watchedFile.setFileSize(file.length());
        watchedFile.setActive(true);
        watchedFile = watchedFileRepository.save(watchedFile);

        try {
            comparisonService.establishBaseline(watchedFile);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "File registered but baseline failed: " + e.getMessage()));
        }

        return ResponseEntity.ok(watchedFile);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> unwatchFile(@PathVariable Long id) {
        return watchedFileRepository.findById(id).map(file -> {
            file.setActive(false);
            watchedFileRepository.save(file);
            return ResponseEntity.ok(Map.of("message", "File deactivated from monitoring"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
