package com.hashwatch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * =============================================================================
 * DOMAIN: Cryptology
 * CONTRIBUTORS: Riya / Priyanshu (Sprint 1: S1-T2), Samarjeet (Sprint 3: S3-T3)
 * Unit tests for Ed25519 digital signature key management, signing, verification,
 * key rotation, PEM/DER encoding, and detached signature validation.
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

    // =========================================================================
    // SPRINT 1 TESTS (S1-T2)
    // =========================================================================

    @Test
    @DisplayName("sign and verify should succeed for genuine data")
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
    @DisplayName("verify should reject tampered data")
    void testTamperedDataFailsVerification() throws Exception {
        signingService.ensureKeysLoaded();
        String originalHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String signature = signingService.sign(originalHash);

        String tamperedHash = "a3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        boolean valid = signingService.verify(tamperedHash, signature);
        assertFalse(valid, "Verification must fail on altered data");
    }

    @Test
    @DisplayName("key persistence across restart should preserve verification capability")
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
    @DisplayName("invalid or corrupted signatures should return false gracefully")
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
    @DisplayName("getPublicKeyBase64 should return valid base64 key representation")
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
    @DisplayName("key fingerprinting and pinned verification should stop key substitution")
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

    // =========================================================================
    // SPRINT 3 TESTS (S3-T3 — Samarjeet)
    // =========================================================================

    @Test
    @DisplayName("S3-T3: rotateKeys should safely archive prior keypair and generate fresh keys")
    void testRotateKeysWithArchive() throws Exception {
        signingService.ensureKeysLoaded();
        String originalFingerprint = signingService.getKeyFingerprint();
        String originalPublicKeyBase64 = signingService.getPublicKeyBase64();
        PublicKey originalPublicKey = signingService.getKeyPair().getPublic();

        // Sign a payload under the initial key
        String testPayload = "HashWatch:v1:/etc/hosts:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855:512";
        String signatureUnderOldKey = signingService.sign(testPayload);
        assertTrue(signingService.verify(testPayload, signatureUnderOldKey, originalFingerprint));

        // Perform key rotation
        String newFingerprint = signingService.rotateKeys(true);

        assertNotNull(newFingerprint);
        assertNotEquals(originalFingerprint, newFingerprint, "Rotated keypair must yield a different fingerprint");
        assertNotEquals(originalPublicKeyBase64, signingService.getPublicKeyBase64(),
                "Active public key must be updated to the new keypair");

        // Verify archive directory structure: keys/archive/<timestamp>/
        Path archiveDir = tempKeyDir.resolve("archive");
        assertTrue(Files.exists(archiveDir), "Archive directory must be created upon rotation");
        try (Stream<Path> archiveEntries = Files.list(archiveDir)) {
            List<Path> archivedFolders = archiveEntries.toList();
            assertEquals(1, archivedFolders.size(), "One archived keypair folder should exist");
            Path archivedFolder = archivedFolders.get(0);
            assertTrue(Files.exists(archivedFolder.resolve("ed25519_private.key")));
            assertTrue(Files.exists(archivedFolder.resolve("ed25519_public.pub")));
            assertEquals(originalPublicKeyBase64, Files.readString(archivedFolder.resolve("ed25519_public.pub")).trim(),
                    "Archived public key must match the original key");
        }

        // Active key files on disk must hold the new keys
        assertEquals(signingService.getPublicKeyBase64(),
                Files.readString(tempKeyDir.resolve("ed25519_public.pub")).trim());

        // Verify active signing uses the fresh key
        String newSignature = signingService.sign(testPayload);
        assertTrue(signingService.verify(testPayload, newSignature, newFingerprint));

        // The old signature cannot be verified using the active new key's pinned fingerprint
        assertFalse(signingService.verify(testPayload, signatureUnderOldKey, signingService.getKeyFingerprint()),
                "Old signature should fail verification against active rotated key");

        // But detached validation against the original public key still succeeds (historical validation)
        assertTrue(signingService.verifyDetached(testPayload, signatureUnderOldKey, originalPublicKey),
                "Historical baseline signature must remain verifiable via detached validation");
    }

    @Test
    @DisplayName("S3-T3: rotateKeys(false) should rotate without creating an archive folder")
    void testRotateKeysWithoutArchive() throws Exception {
        signingService.ensureKeysLoaded();
        String initialFingerprint = signingService.getKeyFingerprint();

        String rotatedFingerprint = signingService.rotateKeys(false);

        assertNotEquals(initialFingerprint, rotatedFingerprint);
        Path archiveDir = tempKeyDir.resolve("archive");
        assertFalse(Files.exists(archiveDir), "Archive directory should not exist when backupPrevious=false");
    }

    @Test
    @DisplayName("S3-T3: RFC 7468 PEM export and import roundtrip for public and private keys")
    void testPemExportAndImportRoundtrip() throws Exception {
        signingService.ensureKeysLoaded();

        String pubPem = signingService.getPublicKeyPem();
        assertNotNull(pubPem);
        assertTrue(pubPem.startsWith("-----BEGIN PUBLIC KEY-----"));
        assertTrue(pubPem.contains("-----END PUBLIC KEY-----"));

        String privPem = signingService.getPrivateKeyPem();
        assertNotNull(privPem);
        assertTrue(privPem.startsWith("-----BEGIN PRIVATE KEY-----"));
        assertTrue(privPem.contains("-----END PRIVATE KEY-----"));

        // Load keys back from PEM representations
        PublicKey loadedPubKey = signingService.loadPublicKeyFromPem(pubPem);
        PrivateKey loadedPrivKey = signingService.loadPrivateKeyFromPem(privPem);

        assertNotNull(loadedPubKey);
        assertNotNull(loadedPrivKey);
        assertArrayEquals(signingService.getKeyPair().getPublic().getEncoded(), loadedPubKey.getEncoded());
        assertArrayEquals(signingService.getKeyPair().getPrivate().getEncoded(), loadedPrivKey.getEncoded());

        // Test static export methods
        String reExportedPubPem = SigningService.exportPublicKeyToPem(loadedPubKey);
        assertEquals(pubPem.trim(), reExportedPubPem.trim());

        String reExportedPrivPem = SigningService.exportPrivateKeyToPem(loadedPrivKey);
        assertEquals(privPem.trim(), reExportedPrivPem.trim());
    }

    @Test
    @DisplayName("S3-T3: DER export and import roundtrip for public and private keys")
    void testDerExportAndImportRoundtrip() throws Exception {
        signingService.ensureKeysLoaded();

        byte[] pubDer = signingService.getPublicKeyDer();
        byte[] privDer = signingService.getPrivateKeyDer();

        assertNotNull(pubDer);
        assertTrue(pubDer.length > 0);
        assertNotNull(privDer);
        assertTrue(privDer.length > 0);

        PublicKey loadedPubKey = signingService.loadPublicKeyFromDer(pubDer);
        PrivateKey loadedPrivKey = signingService.loadPrivateKeyFromDer(privDer);

        assertNotNull(loadedPubKey);
        assertNotNull(loadedPrivKey);
        assertArrayEquals(pubDer, loadedPubKey.getEncoded());
        assertArrayEquals(privDer, loadedPrivKey.getEncoded());
    }

    @Test
    @DisplayName("S3-T3: detached signature verification using PublicKey, Base64, and PEM")
    void testDetachedSignatureValidation() throws Exception {
        signingService.ensureKeysLoaded();
        String testData = "HashWatch:v1:data.bin:f2ca1bb6c7e907d06dafe4687e579fce76b37e4e93b7605022da52e6ccc26fd2:2048";
        String signature = signingService.sign(testData);

        PublicKey activePublicKey = signingService.getKeyPair().getPublic();
        String activePublicKeyBase64 = signingService.getPublicKeyBase64();
        String activePublicKeyPem = signingService.getPublicKeyPem();

        // 1. Detached verification with PublicKey instance
        assertTrue(signingService.verifyDetached(testData, signature, activePublicKey));

        // 2. Detached verification with Base64-encoded public key
        assertTrue(signingService.verifyDetached(testData, signature, activePublicKeyBase64));

        // 3. Detached verification with PEM public key
        assertTrue(signingService.verifyDetachedPem(testData, signature, activePublicKeyPem));

        // 4. Detached verification should reject altered data
        String alteredData = testData + "-tampered";
        assertFalse(signingService.verifyDetached(alteredData, signature, activePublicKey));
        assertFalse(signingService.verifyDetached(alteredData, signature, activePublicKeyBase64));
        assertFalse(signingService.verifyDetachedPem(alteredData, signature, activePublicKeyPem));

        // 5. Null and malformed input handling
        assertFalse(signingService.verifyDetached(null, signature, activePublicKey));
        assertFalse(signingService.verifyDetached(testData, null, activePublicKey));
        assertFalse(signingService.verifyDetached(testData, signature, (PublicKey) null));
        assertFalse(signingService.verifyDetached(testData, signature, (String) null));
        assertFalse(signingService.verifyDetached(testData, signature, "invalid-base64-key!@#"));
        assertFalse(signingService.verifyDetachedPem(testData, signature, "invalid-pem-content"));
    }

    @Test
    @DisplayName("S3-T3: computeFingerprint should correctly produce 64-char hex SHA-256")
    void testComputeFingerprint() throws Exception {
        signingService.ensureKeysLoaded();
        PublicKey activePubKey = signingService.getKeyPair().getPublic();

        String fingerprint = SigningService.computeFingerprint(activePubKey);
        assertNotNull(fingerprint);
        assertEquals(64, fingerprint.length());
        assertTrue(fingerprint.matches("^[0-9a-f]{64}$"), "Fingerprint must be 64-character lowercase hexadecimal");

        assertEquals("UNKNOWN", SigningService.computeFingerprint(null));
    }
}
