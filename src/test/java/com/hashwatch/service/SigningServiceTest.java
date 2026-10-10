package com.hashwatch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * =============================================================================
 * DOMAIN: Cryptology
 * ASSIGNED TO: Riya / Priyanshu (Sprint 1: S1-T2)
 * Unit tests for Ed25519 digital signature key management, signing, and verification.
 * =============================================================================
 */
class SigningServiceTest {

    private SigningService signingService;
    private Path tempKeyDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        this.tempKeyDir = tempDir;
        signingService = new SigningService();
        ReflectionTestUtils.setField(signingService, "keyDirectoryPath", tempDir.toString());
    }

    @Test
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
    void testTamperedDataFailsVerification() throws Exception {
        signingService.ensureKeysLoaded();
        String originalHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String signature = signingService.sign(originalHash);

        String tamperedHash = "a3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        boolean valid = signingService.verify(tamperedHash, signature);
        assertFalse(valid, "Verification must fail on altered data");
    }

    @Test
    void testKeyPersistenceAcrossReload() throws Exception {
        // Step 1: Initialize first service and sign a hash
        signingService.ensureKeysLoaded();
        String originalPublicKey = signingService.getPublicKeyBase64();
        assertFalse(originalPublicKey.isBlank());

        Path privateKeyFile = tempKeyDir.resolve("ed25519_private.key");
        Path publicKeyFile = tempKeyDir.resolve("ed25519_public.pub");
        assertTrue(Files.exists(privateKeyFile), "Private key file must be persisted");
        assertTrue(Files.exists(publicKeyFile), "Public key file must be persisted");

        String testHash = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";
        String signatureFromFirst = signingService.sign(testHash);

        // Step 2: Initialize a second service instance pointing to the same directory
        SigningService secondService = new SigningService();
        ReflectionTestUtils.setField(secondService, "keyDirectoryPath", tempKeyDir.toString());
        secondService.ensureKeysLoaded();

        // Step 3: Verify keys match and cross-instance verification succeeds
        assertEquals(originalPublicKey, secondService.getPublicKeyBase64(),
                "Reloaded public key should match the original persisted key");
        boolean valid = secondService.verify(testHash, signatureFromFirst);
        assertTrue(valid, "Second service instance must verify signatures created before restart");
    }

    @Test
    void testInvalidSignatureFormatsReturnFalse() throws Exception {
        signingService.ensureKeysLoaded();
        String testHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        assertFalse(signingService.verify(testHash, null), "Null signature must return false");
        assertFalse(signingService.verify(testHash, ""), "Empty signature must return false");
        assertFalse(signingService.verify(testHash, "   "), "Blank signature must return false");
        assertFalse(signingService.verify(null, "someValidLookingSignature"), "Null data must return false");
        assertFalse(signingService.verify(testHash, "not-a-valid-base64-string!@#"),
                "Malformed Base64 must return false without throwing unhandled exceptions");
        assertFalse(signingService.verify(testHash, "dGVzdA=="),
                "Invalid length/corrupted signature bytes must return false");
    }

    @Test
    void testGetPublicKeyBase64() throws Exception {
        signingService.ensureKeysLoaded();
        String publicKey = signingService.getPublicKeyBase64();

        assertNotNull(publicKey);
        assertFalse(publicKey.isBlank());
        assertNotNull(signingService.getKeyPair());
        assertNotNull(signingService.getKeyPair().getPublic());
        assertNotNull(signingService.getKeyPair().getPrivate());
    }

    @Test
    void testKeyFingerprintAndPinnedVerification() throws Exception {
        signingService.ensureKeysLoaded();
        String fingerprint = signingService.getKeyFingerprint();

        assertNotNull(fingerprint);
        assertEquals(64, fingerprint.length(), "SHA-256 fingerprint hex string must be exactly 64 characters");

        String testData = "HashWatch:v1:test/file.txt:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855:1024";
        String signature = signingService.sign(testData);

        // Verification with matching pinned fingerprint should succeed
        assertTrue(signingService.verify(testData, signature, fingerprint));

        // Verification with rogue/mismatched fingerprint should fail
        String rogueFingerprint = "0000000000000000000000000000000000000000000000000000000000000000";
        assertFalse(signingService.verify(testData, signature, rogueFingerprint),
                "Pinned verification must fail when public key fingerprint does not match");
        assertFalse(signingService.verify(testData, signature, null),
                "Pinned verification must fail when expected public key ID is null");
    }
}
