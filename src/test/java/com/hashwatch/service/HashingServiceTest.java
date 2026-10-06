package com.hashwatch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * =============================================================================
 * DOMAIN: Cryptology
 * ASSIGNED TO: Samarjeet / Priyanshu (Sprint 1)
 * Remove @Disabled once HashingService is implemented in Sprint 1.
 * =============================================================================
 */
class HashingServiceTest {

    private HashingService hashingService;

    @BeforeEach
    void setUp() {
        hashingService = new HashingService();
    }

    @Test
    void testHashStringKnownVector() {
        // Standard NIST/RFC test vector: SHA-256 of empty string is e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        String hash = hashingService.hashString("");
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    void testHashFileMatchesExpected(@TempDir Path tempDir) throws IOException {
        // Verify regular file streaming correctly produces a 64-character hex digest
        Path testFile = tempDir.resolve("sample.txt");
        Files.writeString(testFile, "Hello HashWatch File Integrity Monitoring");

        String hash = hashingService.hashFile(testFile.toFile());
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void testFileModificationChangesHash(@TempDir Path tempDir) throws IOException {
        // Verify avalanche effect: altering file contents produces a different hash value
        Path testFile = tempDir.resolve("sample.txt");
        Files.writeString(testFile, "Original Content");
        String hash1 = hashingService.hashFile(testFile.toFile());

        Files.writeString(testFile, "Tampered Content");
        String hash2 = hashingService.hashFile(testFile.toFile());

        assertNotEquals(hash1, hash2);
    }

    @Test
    void testHashFileThrowsOnNullOrNonExistent() {
        // Verify defensive boundaries for null input and missing file references
        assertThrows(IllegalArgumentException.class, () -> hashingService.hashFile(null));
        assertThrows(IllegalArgumentException.class, () -> hashingService.hashFile(new File("non_existent_file_12345.xyz")));
    }

    @Test
    void testHashStringThrowsOnNull() {
        // Verify defensive boundary for null string input
        assertThrows(IllegalArgumentException.class, () -> hashingService.hashString(null));
    }
}
