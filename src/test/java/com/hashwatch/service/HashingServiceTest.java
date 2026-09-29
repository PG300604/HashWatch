package com.hashwatch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HashingServiceTest {

    private HashingService hashingService;

    @BeforeEach
    void setUp() {
        hashingService = new HashingService();
    }

    @Test
    void testHashStringKnownVector() {
        // SHA-256 of empty string is e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        String hash = hashingService.hashString("");
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    void testHashFileMatchesExpected(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("sample.txt");
        Files.writeString(testFile, "Hello HashWatch File Integrity Monitoring");

        String hash = hashingService.hashFile(testFile.toFile());
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void testFileModificationChangesHash(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("sample.txt");
        Files.writeString(testFile, "Original Content");
        String hash1 = hashingService.hashFile(testFile.toFile());

        Files.writeString(testFile, "Tampered Content");
        String hash2 = hashingService.hashFile(testFile.toFile());

        assertNotEquals(hash1, hash2);
    }
}
