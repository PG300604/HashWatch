package com.hashwatch.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * =============================================================================
 * DOMAIN: Cryptology
 * CONTRIBUTORS: Riya / Priyanshu (Sprint 1: S1-T2), Samarjeet (Sprint 3: S3-T3)
 * FOLDER / TARGET: src/main/java/com/hashwatch/service/SigningService.java
 * DOC TO UPDATE: docs/TRD.md (Section 3.2 Key Storage & Cryptographic Primitives)
 * =============================================================================
 *
 * Core Cryptographic Engine for HashWatch:
 * 1. Ed25519 digital signature keypair generation, loading, and persistence.
 * 2. UTF-8 payload digital signing and tamper-resilient signature verification.
 * 3. Fingerprint pinning via SHA-256 digests preventing rogue key substitution.
 * 4. Key Rotation (S3-T3 - Samarjeet): Automated archive backup and generation
 *    of fresh Ed25519 keypairs without leaving orphaned state.
 * 5. Standard PEM / DER Encoding (S3-T3 - Samarjeet): RFC 7468 and RFC 8032
 *    compliant import/export for X.509 and PKCS#8 keys.
 * 6. Detached Signature Verification (S3-T3 - Samarjeet): Independent verification
 *    against external, historical, or rotated public keys.
 */
@Service
public class SigningService {

    private static final Logger log = LoggerFactory.getLogger(SigningService.class);
    private static final String ALGORITHM = "Ed25519";
    private static final String PRIVATE_KEY_FILENAME = "ed25519_private.key";
    private static final String PUBLIC_KEY_FILENAME = "ed25519_public.pub";
    private static final String ARCHIVE_DIRECTORY = "archive";

    private static final String PEM_PUBLIC_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PEM_PUBLIC_FOOTER = "-----END PUBLIC KEY-----";
    private static final String PEM_PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PEM_PRIVATE_FOOTER = "-----END PRIVATE KEY-----";

    private static final DateTimeFormatter ARCHIVE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");

    @Value("${hashwatch.crypto.key-directory:keys/}")
    private String keyDirectoryPath;

    private KeyPair keyPair;
    private final Map<String, PublicKey> historicalKeyCache = new ConcurrentHashMap<>();

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

    // =========================================================================
    // SPRINT 3 (S3-T3) — KEY ROTATION (Samarjeet)
    // =========================================================================

    /**
     * Rotates the active Ed25519 keypair and archives the existing keys with a timestamp.
     *
     * @return SHA-256 fingerprint of the newly active public key
     * @throws Exception if key generation, archiving, or disk persistence fails
     */
    public synchronized String rotateKeys() throws Exception {
        return rotateKeys(true);
    }

    /**
     * Rotates the active Ed25519 keypair with an option to archive the current keys.
     *
     * @param backupPrevious true if previous keys should be safely archived
     * @return SHA-256 fingerprint of the newly active public key
     * @throws Exception if key generation, archiving, or disk persistence fails
     */
    public synchronized String rotateKeys(boolean backupPrevious) throws Exception {
        Path dir = Paths.get(keyDirectoryPath);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path privateKeyFile = dir.resolve(PRIVATE_KEY_FILENAME);
        Path publicKeyFile = dir.resolve(PUBLIC_KEY_FILENAME);

        String previousFingerprint = (this.keyPair != null && this.keyPair.getPublic() != null)
                ? getKeyFingerprint()
                : "NONE";

        // Cache historical key in-memory before rotation (supports both backupPrevious=true and false)
        if (this.keyPair != null && this.keyPair.getPublic() != null) {
            historicalKeyCache.put(previousFingerprint.toLowerCase(), this.keyPair.getPublic());
        }

        // Step 1: Safely archive existing keys into keys/archive/<timestamp>/
        if (backupPrevious && Files.exists(privateKeyFile) && Files.exists(publicKeyFile)) {
            String timestamp = LocalDateTime.now().format(ARCHIVE_TIMESTAMP_FORMATTER);
            Path archiveDir = dir.resolve(ARCHIVE_DIRECTORY).resolve(timestamp);
            Files.createDirectories(archiveDir);

            Files.copy(privateKeyFile, archiveDir.resolve(PRIVATE_KEY_FILENAME), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(publicKeyFile, archiveDir.resolve(PUBLIC_KEY_FILENAME), StandardCopyOption.REPLACE_EXISTING);

            log.info("Archived previous Ed25519 keypair (fingerprint: {}) to {}", previousFingerprint, archiveDir.toAbsolutePath());
        }

        // Step 2: Generate fresh Ed25519 keypair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        KeyPair freshKeyPair = keyPairGenerator.generateKeyPair();

        String privateKeyBase64 = Base64.getEncoder().encodeToString(freshKeyPair.getPrivate().getEncoded());
        String publicKeyBase64 = Base64.getEncoder().encodeToString(freshKeyPair.getPublic().getEncoded());

        // Step 3: Atomically overwrite active keys on disk
        Files.writeString(privateKeyFile, privateKeyBase64);
        Files.writeString(publicKeyFile, publicKeyBase64);

        // Step 4: Swap active in-memory reference
        this.keyPair = freshKeyPair;
        String newFingerprint = getKeyFingerprint();

        log.info("Rotated Ed25519 keypair successfully. Previous fingerprint: {}, New fingerprint: {}",
                previousFingerprint, newFingerprint);

        return newFingerprint;
    }

    // =========================================================================
    // SPRINT 3 (S3-T3) — PEM / DER ENCODING & EXPORT UTILITIES (Samarjeet)
    // =========================================================================

    /**
     * Exports the active Ed25519 public key formatted as an RFC 7468 PEM string.
     *
     * @return PEM-encoded public key string
     */
    public String getPublicKeyPem() {
        if (keyPair == null || keyPair.getPublic() == null) {
            try {
                ensureKeysLoaded();
            } catch (Exception e) {
                log.error("Unable to load keys for getPublicKeyPem: {}", e.getMessage());
                return "";
            }
        }
        return (keyPair != null && keyPair.getPublic() != null)
                ? exportPublicKeyToPem(keyPair.getPublic())
                : "";
    }

    /**
     * Exports the active Ed25519 private key formatted as an RFC 7468 PEM string.
     *
     * @return PEM-encoded private key string
     */
    public String getPrivateKeyPem() {
        if (keyPair == null || keyPair.getPrivate() == null) {
            try {
                ensureKeysLoaded();
            } catch (Exception e) {
                log.error("Unable to load keys for getPrivateKeyPem: {}", e.getMessage());
                return "";
            }
        }
        return (keyPair != null && keyPair.getPrivate() != null)
                ? exportPrivateKeyToPem(keyPair.getPrivate())
                : "";
    }

    /**
     * Converts any standard X.509 PublicKey into an RFC 7468 compliant PEM string.
     *
     * @param publicKey the public key to format
     * @return PEM formatted representation
     */
    public static String exportPublicKeyToPem(PublicKey publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("PublicKey cannot be null");
        }
        return formatPem(PEM_PUBLIC_HEADER, PEM_PUBLIC_FOOTER, publicKey.getEncoded());
    }

    /**
     * Converts any standard PKCS#8 PrivateKey into an RFC 7468 compliant PEM string.
     *
     * @param privateKey the private key to format
     * @return PEM formatted representation
     */
    public static String exportPrivateKeyToPem(PrivateKey privateKey) {
        if (privateKey == null) {
            throw new IllegalArgumentException("PrivateKey cannot be null");
        }
        return formatPem(PEM_PRIVATE_HEADER, PEM_PRIVATE_FOOTER, privateKey.getEncoded());
    }

    /**
     * Parses an RFC 7468 standard PEM string into an Ed25519 X.509 PublicKey.
     *
     * @param pemString PEM representation containing -----BEGIN PUBLIC KEY-----
     * @return reconstructed PublicKey instance
     */
    public PublicKey loadPublicKeyFromPem(String pemString) throws GeneralSecurityException {
        byte[] derBytes = cleanAndDecodePem(pemString, PEM_PUBLIC_HEADER, PEM_PUBLIC_FOOTER);
        return loadPublicKeyFromDer(derBytes);
    }

    /**
     * Parses an RFC 7468 standard PEM string into an Ed25519 PKCS#8 PrivateKey.
     *
     * @param pemString PEM representation containing -----BEGIN PRIVATE KEY-----
     * @return reconstructed PrivateKey instance
     */
    public PrivateKey loadPrivateKeyFromPem(String pemString) throws GeneralSecurityException {
        byte[] derBytes = cleanAndDecodePem(pemString, PEM_PRIVATE_HEADER, PEM_PRIVATE_FOOTER);
        return loadPrivateKeyFromDer(derBytes);
    }

    /**
     * Returns raw X.509 DER-encoded bytes of the active public key.
     */
    public byte[] getPublicKeyDer() {
        if (keyPair == null || keyPair.getPublic() == null) {
            try {
                ensureKeysLoaded();
            } catch (Exception e) {
                log.error("Unable to load keys for getPublicKeyDer: {}", e.getMessage());
                return new byte[0];
            }
        }
        return (keyPair != null && keyPair.getPublic() != null)
                ? keyPair.getPublic().getEncoded()
                : new byte[0];
    }

    /**
     * Returns raw PKCS#8 DER-encoded bytes of the active private key.
     */
    public byte[] getPrivateKeyDer() {
        if (keyPair == null || keyPair.getPrivate() == null) {
            try {
                ensureKeysLoaded();
            } catch (Exception e) {
                log.error("Unable to load keys for getPrivateKeyDer: {}", e.getMessage());
                return new byte[0];
            }
        }
        return (keyPair != null && keyPair.getPrivate() != null)
                ? keyPair.getPrivate().getEncoded()
                : new byte[0];
    }

    /**
     * Reconstructs an Ed25519 PublicKey from standard X.509 DER-encoded bytes.
     *
     * @param derBytes raw ASN.1 DER byte array
     * @return PublicKey instance
     */
    public PublicKey loadPublicKeyFromDer(byte[] derBytes) throws GeneralSecurityException {
        if (derBytes == null || derBytes.length == 0) {
            throw new IllegalArgumentException("DER byte array cannot be null or empty");
        }
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(new X509EncodedKeySpec(derBytes));
    }

    /**
     * Reconstructs an Ed25519 PrivateKey from standard PKCS#8 DER-encoded bytes.
     *
     * @param derBytes raw ASN.1 DER byte array
     * @return PrivateKey instance
     */
    public PrivateKey loadPrivateKeyFromDer(byte[] derBytes) throws GeneralSecurityException {
        if (derBytes == null || derBytes.length == 0) {
            throw new IllegalArgumentException("DER byte array cannot be null or empty");
        }
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(derBytes));
    }

    // =========================================================================
    // SPRINT 3 (S3-T3) — DETACHED SIGNATURE VALIDATION (Samarjeet)
    // =========================================================================

    /**
     * Verifies data against a signature using an external or historical PublicKey.
     * Detached validation does not depend on the active keypair in memory.
     *
     * @param data original string payload
     * @param base64Signature Base64-encoded signature
     * @param publicKey the specific public key to verify against
     * @return true if valid signature; false otherwise
     */
    public boolean verifyDetached(String data, String base64Signature, PublicKey publicKey) {
        if (data == null || base64Signature == null || base64Signature.isBlank() || publicKey == null) {
            return false;
        }
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(base64Signature.trim());
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(data.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signatureBytes);
        } catch (IllegalArgumentException e) {
            log.warn("Malformed Base64 signature encountered during detached verification: {}", e.getMessage());
            return false;
        } catch (GeneralSecurityException e) {
            log.warn("Cryptographic error during detached verification: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifies data against a signature using a Base64-encoded external public key.
     *
     * @param data original string payload
     * @param base64Signature Base64-encoded signature
     * @param base64PublicKey Base64-encoded X.509 public key
     * @return true if valid signature; false otherwise
     */
    public boolean verifyDetached(String data, String base64Signature, String base64PublicKey) {
        if (base64PublicKey == null || base64PublicKey.isBlank()) {
            return false;
        }
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(base64PublicKey.trim());
            PublicKey publicKey = loadPublicKeyFromDer(publicKeyBytes);
            return verifyDetached(data, base64Signature, publicKey);
        } catch (Exception e) {
            log.warn("Failed to decode external Base64 public key during detached verification: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifies data against a signature using an external PEM-encoded public key.
     *
     * @param data original string payload
     * @param base64Signature Base64-encoded signature
     * @param pemPublicKey PEM-encoded public key with header and footer
     * @return true if valid signature; false otherwise
     */
    public boolean verifyDetachedPem(String data, String base64Signature, String pemPublicKey) {
        if (pemPublicKey == null || pemPublicKey.isBlank()) {
            return false;
        }
        try {
            PublicKey publicKey = loadPublicKeyFromPem(pemPublicKey);
            return verifyDetached(data, base64Signature, publicKey);
        } catch (Exception e) {
            log.warn("Failed to parse external PEM public key during detached verification: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Computes the SHA-256 fingerprint of any given PublicKey instance.
     *
     * @param publicKey the public key to fingerprint
     * @return 64-character lowercase hexadecimal SHA-256 fingerprint
     */
    public static String computeFingerprint(PublicKey publicKey) {
        if (publicKey == null) {
            return "UNKNOWN";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey.getEncoded());
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

    // =========================================================================
    // CORE SIGNING & PINNED VERIFICATION (Sprint 1: S1-T2)
    // =========================================================================

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
     * Verifies data against a signature using the active public key.
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
        return verifyDetached(data, base64Signature, keyPair.getPublic());
    }

    /**
     * Resolves a public key by its fingerprint (publicKeyId), checking the active key,
     * the in-memory historical cache, and archived keys on disk under keys/archive/.
     *
     * @param fingerprint the 64-char SHA-256 fingerprint to resolve
     * @return Optional containing the resolved PublicKey if found
     */
    public Optional<PublicKey> resolvePublicKeyByFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return Optional.empty();
        }
        String normalizedFingerprint = fingerprint.trim().toLowerCase();

        // 1. Check active key
        if (keyPair != null && keyPair.getPublic() != null) {
            String activeFingerprint = getKeyFingerprint();
            if (activeFingerprint.equalsIgnoreCase(normalizedFingerprint)) {
                return Optional.of(keyPair.getPublic());
            }
        }

        // 2. Check in-memory historical cache
        PublicKey cached = historicalKeyCache.get(normalizedFingerprint);
        if (cached != null) {
            return Optional.of(cached);
        }

        // 3. Scan disk archive: keys/archive/<timestamp>/ed25519_public.pub
        Path dir = Paths.get(keyDirectoryPath);
        Path archiveBase = dir.resolve(ARCHIVE_DIRECTORY);
        if (Files.exists(archiveBase) && Files.isDirectory(archiveBase)) {
            try (Stream<Path> archiveFolders = Files.list(archiveBase)) {
                for (Path folder : archiveFolders.toList()) {
                    Path pubFile = folder.resolve(PUBLIC_KEY_FILENAME);
                    if (Files.exists(pubFile)) {
                        try {
                            String pubBase64 = Files.readString(pubFile).trim();
                            byte[] pubBytes = Base64.getDecoder().decode(pubBase64);
                            PublicKey pubKey = loadPublicKeyFromDer(pubBytes);
                            String fp = computeFingerprint(pubKey).toLowerCase();
                            historicalKeyCache.put(fp, pubKey);
                            if (fp.equalsIgnoreCase(normalizedFingerprint)) {
                                return Optional.of(pubKey);
                            }
                        } catch (Exception ex) {
                            log.debug("Skipping unreadable/corrupted archived key at {}: {}", pubFile, ex.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to scan key archive directory: {}", e.getMessage());
            }
        }

        return Optional.empty();
    }

    /**
     * Verifies data against a signature using the public key and asserts that
     * the signature was produced under the expected public key fingerprint.
     * If the fingerprint does not match the active key, it attempts to resolve
     * historical archived keys to preserve verification across key rotations.
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
        String normalizedExpectedId = expectedPublicKeyId.trim().toLowerCase();
        String currentFingerprint = getKeyFingerprint();

        if (currentFingerprint.equalsIgnoreCase(normalizedExpectedId)) {
            return verify(data, base64Signature);
        }

        // Resolve historical public keys before rejecting baselines after key rotation
        Optional<PublicKey> historicalKey = resolvePublicKeyByFingerprint(normalizedExpectedId);
        if (historicalKey.isPresent()) {
            log.info("Verifying baseline using historical public key for fingerprint {}", expectedPublicKeyId);
            return verifyDetached(data, base64Signature, historicalKey.get());
        }

        log.warn("Public key fingerprint mismatch! Expected: {}, Current: {}, and no matching archived key found",
                expectedPublicKeyId, currentFingerprint);
        return false;
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
        return computeFingerprint(keyPair != null ? keyPair.getPublic() : null);
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

    // =========================================================================
    // HELPER UTILITIES
    // =========================================================================

    private static String formatPem(String header, String footer, byte[] encodedBytes) {
        String base64 = Base64.getEncoder().encodeToString(encodedBytes);
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        int length = base64.length();
        for (int i = 0; i < length; i += 64) {
            sb.append(base64, i, Math.min(i + 64, length)).append("\n");
        }
        sb.append(footer).append("\n");
        return sb.toString();
    }

    private static byte[] cleanAndDecodePem(String pem, String header, String footer) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("PEM content cannot be null or empty");
        }
        String cleaned = pem
                .replace(header, "")
                .replace(footer, "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(cleaned);
    }
}
