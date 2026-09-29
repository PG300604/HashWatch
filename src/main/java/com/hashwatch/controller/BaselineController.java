package com.hashwatch.controller;

import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import com.hashwatch.service.ComparisonService;
import com.hashwatch.service.SigningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping
    public List<BaselineEntry> getActiveBaselines() {
        return baselineEntryRepository.findByCurrentTrue();
    }

    @GetMapping("/public-key")
    public Map<String, String> getPublicKey() {
        return Map.of(
                "algorithm", "Ed25519",
                "publicKey", signingService.getPublicKeyBase64()
        );
    }

    @PostMapping("/generate/{fileId}")
    public ResponseEntity<?> rebaselineFile(@PathVariable Long fileId) {
        return watchedFileRepository.findById(fileId).map(file -> {
            try {
                BaselineEntry entry = comparisonService.establishBaseline(file);
                return ResponseEntity.ok(entry);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/generate-all")
    public ResponseEntity<?> rebaselineAll() {
        List<WatchedFile> active = watchedFileRepository.findByActiveTrue();
        int count = 0;
        for (WatchedFile file : active) {
            try {
                comparisonService.establishBaseline(file);
                count++;
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of("message", "Re-baselined files", "count", count));
    }
}
