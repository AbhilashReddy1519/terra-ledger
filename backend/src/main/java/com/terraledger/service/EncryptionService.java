package com.terraledger.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Service for AES-256-GCM symmetric encryption and decryption of land record files
 * before storage in IPFS.
 *
 * Payload wire format:
 * [12-byte IV] + [Ciphertext + 16-byte GCM Authentication Tag]
 */
@Service
public class EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(EncryptionService.class);

    public static final String ALGORITHM = "AES";
    public static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    public static final int GCM_IV_LENGTH_BYTES = 12; // 96 bits recommended for GCM
    public static final int GCM_TAG_LENGTH_BITS = 128; // 16 bytes
    public static final int KEY_LENGTH_BITS = 256;

    private final SecretKey masterKey;
    private final SecureRandom secureRandom;

    public EncryptionService(@Value("${app.encryption.master-key:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef}") String configuredKey) {
        this.masterKey = parseKey(configuredKey);
        this.secureRandom = new SecureRandom();
        log.info("EncryptionService initialized with AES-256-GCM master key.");
    }

    /**
     * Encrypts plaintext bytes using the master key and a freshly generated 12-byte IV.
     *
     * @param plaintext Original unencrypted bytes
     * @return [12-byte IV] + [Ciphertext + 16-byte GCM tag]
     */
    public byte[] encrypt(byte[] plaintext) {
        return encryptWithKey(plaintext, this.masterKey);
    }

    /**
     * Encrypts plaintext bytes using a specific SecretKey.
     */
    public byte[] encryptWithKey(byte[] plaintext, SecretKey key) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            byte[] cipherTextWithTag = cipher.doFinal(plaintext);

            // Output: IV || ciphertextWithTag
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherTextWithTag.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherTextWithTag);
            return byteBuffer.array();

        } catch (Exception e) {
            log.error("Failed to encrypt data with AES-256-GCM: {}", e.getMessage());
            throw new SecurityException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts an encrypted payload using the master key.
     * Validates GCM authentication tag to detect tampering or corruption.
     *
     * @param encryptedPayload [12-byte IV] + [Ciphertext + 16-byte GCM tag]
     * @return Decrypted plaintext bytes
     * @throws SecurityException if decryption or authentication tag check fails
     */
    public byte[] decrypt(byte[] encryptedPayload) {
        return decryptWithKey(encryptedPayload, this.masterKey);
    }

    /**
     * Decrypts an encrypted payload using a specific SecretKey.
     */
    public byte[] decryptWithKey(byte[] encryptedPayload, SecretKey key) {
        if (encryptedPayload == null || encryptedPayload.length < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
            throw new IllegalArgumentException("Encrypted payload is missing or too short to be valid AES-GCM ciphertext");
        }

        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedPayload);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherTextWithTag = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherTextWithTag);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            return cipher.doFinal(cipherTextWithTag);

        } catch (Exception e) {
            log.warn("AES-256-GCM decryption failed: {}", e.getMessage());
            throw new SecurityException("Decryption failed: integrity check failed or wrong encryption key", e);
        }
    }

    /**
     * Rotates key: decrypts with oldKey and re-encrypts with newKey.
     */
    public byte[] rotateKey(byte[] encryptedPayload, SecretKey oldKey, SecretKey newKey) {
        byte[] plaintext = decryptWithKey(encryptedPayload, oldKey);
        return encryptWithKey(plaintext, newKey);
    }

    /**
     * Generates a new 256-bit AES key.
     */
    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_LENGTH_BITS, new SecureRandom());
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES algorithm not supported", e);
        }
    }

    /**
     * Exports a SecretKey as Base64 string.
     */
    public static String exportKey(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Parses a key string (Hex, Base64, or text) into a 256-bit SecretKey.
     */
    public static SecretKey parseKey(String keyStr) {
        if (keyStr == null || keyStr.isBlank()) {
            throw new IllegalArgumentException("Encryption key string cannot be blank");
        }

        byte[] keyBytes;
        String trimmed = keyStr.trim();

        // 64-char Hex format (32 bytes = 256 bits)
        if (trimmed.length() == 64 && trimmed.matches("^[0-9a-fA-F]+$")) {
            keyBytes = HexFormat.of().parseHex(trimmed);
        }
        // Base64 format (44 chars with padding for 32 bytes)
        else {
            try {
                byte[] decoded = Base64.getDecoder().decode(trimmed);
                if (decoded.length == 32) {
                    keyBytes = decoded;
                } else {
                    keyBytes = sha256(trimmed.getBytes(StandardCharsets.UTF_8));
                }
            } catch (IllegalArgumentException e) {
                // If not valid Base64, derive 256-bit key via SHA-256
                keyBytes = sha256(trimmed.getBytes(StandardCharsets.UTF_8));
            }
        }

        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
