package com.hashwatch.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * =============================================================================
 * DOMAIN: Cryptology
 * ASSIGNED TO: Samarjeet / Priyanshu (Sprint 1)
 * FOLDER / TARGET: src/main/java/com/hashwatch/service/HashingService.java
 * DOC TO UPDATE: docs/TRD.md (Section 3.1)
 * =============================================================================
 *
 * Task Description:
 * Implement streaming SHA-256 computation using a 64 KB buffer to ensure
 * minimal memory overhead when processing files of arbitrary sizes.
 *
 * Acceptance Criteria:
 * 1. hashFile() must return a 64-character lowercase hex string.
 * 2. Must use java.security.MessageDigest with buffered stream (e.g. 64KB chunks).
 * 3. HashingServiceTest unit tests must pass.
 */
@Service
public class HashingService {

    private static final int BUFFER_SIZE = 64 * 1024; // 64 KB chunk buffer

    /**
     * Compute SHA-256 hash of a file using buffered streaming.
     *
     * @param file the target file on disk
     * @return 64-character hexadecimal string
     * @throws IOException on file read failure
     */
    public String hashFile(File file) throws IOException {
        // TODO [Sprint 1 - Cryptology]: Assigned to Samarjeet / Priyanshu
        // 1. Validate that the file exists and is a readable file.
        // 2. Initialize MessageDigest for "SHA-256".
        // 3. Read the file through FileInputStream in 64 KB chunks into the digest.
        // 4. Return the hex formatted digest string.
        throw new UnsupportedOperationException("TODO: Implement streaming SHA-256 with 64KB buffer in Sprint 1");
    }

    /**
     * Compute SHA-256 hash of a plain text string.
     *
     * @param input text to hash
     * @return 64-character hexadecimal string
     */
    public String hashString(String input) {
        // TODO [Sprint 1 - Cryptology]: Assigned to Samarjeet / Priyanshu
        // Implement SHA-256 digest on input UTF-8 bytes and return hex format.
        throw new UnsupportedOperationException("TODO: Implement hashString in Sprint 1");
    }
}
