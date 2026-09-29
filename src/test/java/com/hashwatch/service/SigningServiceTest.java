package com.hashwatch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * =============================================================================
 * DOMAIN: Cryptology
 * ASSIGNED TO: Riya / Priyanshu (Sprint 1)
 * Remove @Disabled once SigningService is implemented in Sprint 1.
 * =============================================================================
 */
class SigningServiceTest {

    private SigningService signingService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        signingService = new SigningService();
        ReflectionTestUtils.setField(signingService, "keyDirectoryPath", tempDir.toString());
    }

    @Test
    @Disabled("Pending Sprint 1 implementation by Riya / Priyanshu")
    void testSignAndVerify() throws Exception {
        signingService.ensureKeysLoaded();
        String testHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String signature = signingService.sign(testHash);

        assertNotNull(signature);
        assertFalse(signature.isBlank());

        boolean valid = signingService.verify(testHash, signature);
        assertTrue(valid, "Signature should be valid for unaltered data");
    }

    @Test
    @Disabled("Pending Sprint 1 implementation by Riya / Priyanshu")
    void testTamperedDataFailsVerification() throws Exception {
        signingService.ensureKeysLoaded();
        String originalHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String signature = signingService.sign(originalHash);

        String tamperedHash = "a3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        boolean valid = signingService.verify(tamperedHash, signature);
        assertFalse(valid, "Verification must fail on altered data");
    }
}
