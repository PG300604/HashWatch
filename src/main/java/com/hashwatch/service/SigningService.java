package com.hashwatch.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * =============================================================================
 * DOMAIN: Cryptology
 * ASSIGNED TO: Riya / Priyanshu (Sprint 1)
 * FOLDER / TARGET: src/main/java/com/hashwatch/service/SigningService.java
 * DOC TO UPDATE: docs/TRD.md (Section 3.2)
 * =============================================================================
 *
 * Task Description:
 * Implement Ed25519 digital signature key management, data signing,
 * and signature verification.
 *
 * Acceptance Criteria:
 * 1. Generate or load an Ed25519 keypair into the configured key directory.
 * 2. sign(data) returns an Ed25519 digital signature encoded in Base64.
 * 3. verify(data, signature) accurately verifies genuine data and returns false on altered data.
 * 4. SigningServiceTest unit tests must pass.
 */
@Service
public class SigningService {

    private static final Logger log = LoggerFactory.getLogger(SigningService.class);
    private static final String ALGORITHM = "Ed25519";
    private static final String PRIVATE_KEY_FILENAME = "ed25519_private.key";
    private static final String PUBLIC_KEY_FILENAME = "ed25519_public.pub";

    @Value("${hashwatch.crypto.key-directory:keys/}")
    private String keyDirectoryPath;

    private KeyPair keyPair;

    @PostConstruct
    public void init() {
        log.info("Initializing SigningService with key directory: {}", keyDirectoryPath);
        try {
            ensureKeysLoaded();
            log.info("Ed25519 digital signing service initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize Ed25519 signing keys: {}", e.getMessage(), e);
            throw new IllegalStateException("Could not initialize Ed25519 keys on startup", e);
        }
    }

    /**
     * Loads existing Ed25519 keys from disk or generates a fresh keypair and persists them.
     */
    public synchronized void ensureKeysLoaded() throws Exception {
        if (this.keyPair != null && this.keyPair.getPrivate() != null && this.keyPair.getPublic() != null) {
            return;
        }

        Path dir = Paths.get(keyDirectoryPath);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path privateKeyFile = dir.resolve(PRIVATE_KEY_FILENAME);
        Path publicKeyFile = dir.resolve(PUBLIC_KEY_FILENAME);

        if (Files.exists(privateKeyFile) && Files.exists(publicKeyFile)) {
            log.info("Loading existing Ed25519 keypair from {}", dir.toAbsolutePath());
            String privateKeyBase64 = Files.readString(privateKeyFile).trim();
            String publicKeyBase64 = Files.readString(publicKeyFile).trim();

            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            this.keyPair = new KeyPair(publicKey, privateKey);
        } else {
            log.info("Generating new Ed25519 keypair and persisting to {}", dir.toAbsolutePath());
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            this.keyPair = keyPairGenerator.generateKeyPair();

            String privateKeyBase64 = Base64.getEncoder().encodeToString(this.keyPair.getPrivate().getEncoded());
            String publicKeyBase64 = Base64.getEncoder().encodeToString(this.keyPair.getPublic().getEncoded());

            Files.writeString(privateKeyFile, privateKeyBase64);
            Files.writeString(publicKeyFile, publicKeyBase64);
        }
    }

    /**
     * Signs data (such as a file SHA-256 hash) using the Ed25519 private key.
     *
     * @param data SHA-256 digest string to sign
     * @return Base64-encoded Ed25519 signature
     */
    public String sign(String data) throws GeneralSecurityException {
        if (data == null) {
            throw new IllegalArgumentException("Data to sign cannot be null");
        }
        if (keyPair == null || keyPair.getPrivate() == null) {
            try {
                ensureKeysLoaded();
            } catch (Exception e) {
                throw new GeneralSecurityException("Unable to initialize Ed25519 private key for signing", e);
            }
        }
        Signature signature = Signature.getInstance(ALGORITHM);
        signature.initSign(keyPair.getPrivate());
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signedBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signedBytes);
    }

    /**
     * Verifies data against a signature using the public key.
     *
     * @param data original hash data
     * @param base64Signature Base64-encoded signature
     * @return true if valid signature; false otherwise
     */
    public boolean verify(String data, String base64Signature) throws GeneralSecurityException {
        if (data == null || base64Signature == null || base64Signature.isBlank()) {
            return false;
        }
        if (keyPair == null || keyPair.getPublic() == null) {
            try {
                ensureKeysLoaded();
            } catch (Exception e) {
                throw new GeneralSecurityException("Unable to initialize Ed25519 public key for verification", e);
            }
        }
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(base64Signature.trim());
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(keyPair.getPublic());
            verifier.update(data.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signatureBytes);
        } catch (IllegalArgumentException e) {
            log.warn("Malformed Base64 signature encountered during verification: {}", e.getMessage());
            return false;
        } catch (GeneralSecurityException e) {
            log.warn("Cryptographic verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifies data against a signature using the public key and asserts that
     * the signature was produced under the expected public key fingerprint.
     *
     * @param data original hash/canonical payload data
     * @param base64Signature Base64-encoded signature
     * @param expectedPublicKeyId expected SHA-256 fingerprint of the public key
     * @return true if valid signature and key matches pinned fingerprint; false otherwise
     */
    public boolean verify(String data, String base64Signature, String expectedPublicKeyId) throws GeneralSecurityException {
        if (expectedPublicKeyId == null || expectedPublicKeyId.isBlank()) {
            log.warn("Missing expectedPublicKeyId during verification");
            return false;
        }
        String currentFingerprint = getKeyFingerprint();
        if (!currentFingerprint.equalsIgnoreCase(expectedPublicKeyId.trim())) {
            log.warn("Public key fingerprint mismatch! Expected: {}, Current: {}", expectedPublicKeyId, currentFingerprint);
            return false;
        }
        return verify(data, base64Signature);
    }

    /**
     * Returns the SHA-256 hex fingerprint of the active public key.
     * This acts as the immutable publicKeyId stored in BaselineEntry.
     */
    public String getKeyFingerprint() {
        if (keyPair == null || keyPair.getPublic() == null) {
            try {
                ensureKeysLoaded();
            } catch (Exception e) {
                log.error("Unable to load keys for getKeyFingerprint: {}", e.getMessage());
                return "UNKNOWN";
            }
        }
        if (keyPair == null || keyPair.getPublic() == null) {
            return "UNKNOWN";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyPair.getPublic().getEncoded());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (GeneralSecurityException e) {
            log.error("Failed to compute public key fingerprint: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    /**
     * Returns the Base64-encoded X.509 public key string.
     */
    public String getPublicKeyBase64() {
        if (keyPair == null || keyPair.getPublic() == null) {
            try {
                ensureKeysLoaded();
            } catch (Exception e) {
                log.error("Unable to load keys for getPublicKeyBase64: {}", e.getMessage());
                return "";
            }
        }
        return (keyPair != null && keyPair.getPublic() != null)
                ? Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
                : "";
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }
}
