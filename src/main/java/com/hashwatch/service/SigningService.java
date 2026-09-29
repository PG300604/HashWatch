package com.hashwatch.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class SigningService {

    private static final Logger log = LoggerFactory.getLogger(SigningService.class);
    private static final String ALGORITHM = "Ed25519";

    @Value("${hashwatch.crypto.key-directory:keys/}")
    private String keyDirectoryPath;

    private KeyPair keyPair;

    @PostConstruct
    public void init() {
        try {
            ensureKeysLoaded();
        } catch (Exception e) {
            log.error("Failed to initialize or load Ed25519 keys: {}", e.getMessage(), e);
        }
    }

    /**
     * Ensures keys exist on disk; if not, generates a fresh Ed25519 keypair.
     */
    public synchronized void ensureKeysLoaded() throws NoSuchAlgorithmException, IOException, GeneralSecurityException {
        File dir = new File(keyDirectoryPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File privateKeyFile = new File(dir, "ed25519_private.key");
        File publicKeyFile = new File(dir, "ed25519_public.pub");

        if (privateKeyFile.exists() && publicKeyFile.exists()) {
            byte[] privBytes = Files.readAllBytes(privateKeyFile.toPath());
            byte[] pubBytes = Files.readAllBytes(publicKeyFile.toPath());

            KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privBytes)));
            PublicKey pubKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pubBytes)));

            this.keyPair = new KeyPair(pubKey, privKey);
            log.info("Loaded existing Ed25519 keypair from {}", dir.getAbsolutePath());
        } else {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM);
            this.keyPair = kpg.generateKeyPair();

            try (FileOutputStream fos = new FileOutputStream(privateKeyFile)) {
                fos.write(Base64.getEncoder().encode(this.keyPair.getPrivate().getEncoded()));
            }
            try (FileOutputStream fos = new FileOutputStream(publicKeyFile)) {
                fos.write(Base64.getEncoder().encode(this.keyPair.getPublic().getEncoded()));
            }
            log.info("Generated new Ed25519 keypair and saved to {}", dir.getAbsolutePath());
        }
    }

    /**
     * Signs data (such as a file hash) using the Ed25519 private key.
     */
    public String sign(String data) throws GeneralSecurityException {
        if (keyPair == null || keyPair.getPrivate() == null) {
            throw new IllegalStateException("Private key is not initialized");
        }
        Signature signature = Signature.getInstance(ALGORITHM);
        signature.initSign(keyPair.getPrivate());
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] sigBytes = signature.sign();
        return Base64.getEncoder().encodeToString(sigBytes);
    }

    /**
     * Verifies data against a signature using the provided public key or the system's public key.
     */
    public boolean verify(String data, String base64Signature, PublicKey pubKey) throws GeneralSecurityException {
        PublicKey keyToUse = (pubKey != null) ? pubKey : this.keyPair.getPublic();
        if (keyToUse == null) {
            throw new IllegalStateException("Public key is not available for verification");
        }
        Signature signature = Signature.getInstance(ALGORITHM);
        signature.initVerify(keyToUse);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getDecoder().decode(base64Signature));
    }

    public boolean verify(String data, String base64Signature) throws GeneralSecurityException {
        return verify(data, base64Signature, this.keyPair != null ? this.keyPair.getPublic() : null);
    }

    public String getPublicKeyBase64() {
        if (keyPair == null || keyPair.getPublic() == null) return "";
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }
}
