package com.hashwatch.service;

import com.hashwatch.entity.BaselineEntry;
import com.hashwatch.entity.WatchedFile;
import com.hashwatch.repository.AlertEventRepository;
import com.hashwatch.repository.BaselineEntryRepository;
import com.hashwatch.repository.WatchedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * =============================================================================
 * DOMAIN: Backend
 * ASSIGNED TO: Priyanshu / Samarjeet (Sprint 2)
 * FOLDER / TARGET: src/main/java/com/hashwatch/service/ComparisonService.java
 * DOC TO UPDATE: docs/DFD.md (Process 1.0 & Process 2.0)
 * =============================================================================
 *
 * Task Description:
 * Core verification engine that compares disk files against cryptographically
 * signed baselines and updates file status / logs alerts upon mismatch or tampering.
 *
 * Acceptance Criteria:
 * 1. establishBaseline(file): Computes SHA-256 via HashingService, signs with SigningService,
 *    and saves BaselineEntry to database.
 * 2. verifyFile(file): Verifies baseline signature first (detects DB tampering), then hashes
 *    current file and sets status (VERIFIED, TAMPERED, MISSING).
 * 3. Any violation generates an AlertEvent record.
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
     * Creates or updates a baseline for a given watched file.
     */
    public BaselineEntry establishBaseline(WatchedFile watchedFile) throws IOException, GeneralSecurityException {
        // TODO [Sprint 2 - Backend]: Assigned to Priyanshu / Samarjeet
        // 1. Read file from watchedFile.getFilePath()
        // 2. Call hashingService.hashFile(file)
        // 3. Call signingService.sign(hash)
        // 4. Retire previous baseline (setCurrent(false)) and insert new BaselineEntry
        // 5. Update watchedFile status to 'VERIFIED'
        throw new UnsupportedOperationException("TODO: Implement establishBaseline() in Sprint 2");
    }

    /**
     * Scans and verifies all active watched files.
     */
    public void runVerificationScan() {
        // TODO [Sprint 2 - Backend]: Assigned to Priyanshu / Samarjeet
        // 1. Query watchedFileRepository.findByActiveTrue()
        // 2. Iterate each file and call verifyFile(file)
        log.info("Integrity verification scan triggered (To be implemented in Sprint 2)");
    }

    /**
     * Verifies a single file against its active cryptographic baseline.
     */
    public void verifyFile(WatchedFile fileEntity) {
        // TODO [Sprint 2 - Backend]: Assigned to Priyanshu / Samarjeet
        // 1. Retrieve current active baseline from baselineEntryRepository.
        // 2. Check if file exists on disk (if not, flag MISSING, create AlertEvent).
        // 3. Verify Ed25519 signature of the stored baseline with signingService.verify().
        //    (If signature is invalid, flag SIGNATURE_INVALID, severity CRITICAL).
        // 4. Compute current SHA-256 hash and compare with baseline hash:
        //    - Match: status -> VERIFIED
        //    - Mismatch: status -> TAMPERED, create AlertEvent (severity CRITICAL/HIGH).
    }
}
