package com.hashwatch.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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
     * @throws IOException on file read failure or if file is invalid
     */
    public String hashFile(File file) throws IOException {
        // Defensive validation: Ensure target exists, is accessible, and is not a directory
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File must exist and be a regular file: " + (file != null ? file.getAbsolutePath() : "null"));
        }

        try {
            // SHA-256 standard cryptographic message digest
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Process file using a bounded 64 KB buffer to maintain O(1) memory footprint regardless of file size
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            // Convert raw 32-byte digest into a standard lowercase 64-character hexadecimal representation
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current security provider environment", e);
        }
    }

    /**
     * Compute SHA-256 hash of a plain text string.
     *
     * @param input text to hash
     * @return 64-character hexadecimal string
     */
    public String hashString(String input) {
        // Input validation
        if (input == null) {
            throw new IllegalArgumentException("Input string cannot be null");
        }

        try {
            // Compute digest over standard UTF-8 encoded byte array
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current security provider environment", e);
        }
    }
}
