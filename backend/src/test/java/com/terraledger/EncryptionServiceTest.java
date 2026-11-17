package com.terraledger;

import com.terraledger.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private final String masterKeyHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(masterKeyHex);
    }

    @Test
    @DisplayName("Encrypt and decrypt round-trip matches original bytes and SHA-256 hash")
    void testEncryptAndDecryptMatchesOriginal() throws Exception {
        byte[] originalData = "SURVEY NO: 45/A, OWNER: John Doe, AREA: 2.5 Acres".getBytes(StandardCharsets.UTF_8);
        String originalHash = sha256(originalData);

        byte[] encrypted = encryptionService.encrypt(originalData);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > originalData.length);

        byte[] decrypted = encryptionService.decrypt(encrypted);
        assertArrayEquals(originalData, decrypted);
        assertEquals(originalHash, sha256(decrypted));
    }

    @Test
    @DisplayName("Ciphertext is randomized and does not leak plaintext")
    void testCiphertextDoesNotLeakPlaintext() {
        byte[] originalData = "SENSITIVE LAND TITLE RECORD DATA".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted1 = encryptionService.encrypt(originalData);
        byte[] encrypted2 = encryptionService.encrypt(originalData);

        // Ciphertext should not contain plaintext
        String cipherString = new String(encrypted1, StandardCharsets.ISO_8859_1);
        assertFalse(cipherString.contains("SENSITIVE"));

        // Different IVs produce different ciphertexts
        assertFalse(Arrays.equals(encrypted1, encrypted2));

        // Both decrypt to the same original data
        assertArrayEquals(originalData, encryptionService.decrypt(encrypted1));
        assertArrayEquals(originalData, encryptionService.decrypt(encrypted2));
    }

    @Test
    @DisplayName("Tampered ciphertext fails GCM authentication tag check with SecurityException")
    void testTamperedCiphertextFailsTagAuthentication() {
        byte[] originalData = "UNTOUCHED DOCUMENT PAYLOAD".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encryptionService.encrypt(originalData);

        // Flip one bit in ciphertext body (after 12-byte IV)
        byte[] tamperedCiphertext = Arrays.copyOf(encrypted, encrypted.length);
        tamperedCiphertext[15] ^= 0x01;

        assertThrows(SecurityException.class, () -> encryptionService.decrypt(tamperedCiphertext));
    }

    @Test
    @DisplayName("Tampered IV fails GCM authentication check with SecurityException")
    void testTamperedIvFailsTagAuthentication() {
        byte[] originalData = "UNTOUCHED DOCUMENT PAYLOAD".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encryptionService.encrypt(originalData);

        // Flip one bit in the 12-byte IV header
        byte[] tamperedIv = Arrays.copyOf(encrypted, encrypted.length);
        tamperedIv[2] ^= 0x01;

        assertThrows(SecurityException.class, () -> encryptionService.decrypt(tamperedIv));
    }

    @Test
    @DisplayName("Decrypting with wrong key fails with SecurityException")
    void testWrongKeyFailsDecryption() {
        byte[] originalData = "SECRET LAND RECORD".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encryptionService.encrypt(originalData);

        SecretKey wrongKey = EncryptionService.generateKey();
        assertThrows(SecurityException.class, () -> encryptionService.decryptWithKey(encrypted, wrongKey));
    }

    @Test
    @DisplayName("Key rotation and restore: decrypt with Old Key, re-encrypt with New Key, restore from backup")
    void testKeyRotationAndRestore() throws Exception {
        byte[] originalData = "RESTORE AND ROTATION TEST DEED".getBytes(StandardCharsets.UTF_8);
        String expectedHash = sha256(originalData);

        SecretKey keyA = EncryptionService.generateKey();
        SecretKey keyB = EncryptionService.generateKey();

        // 1. Initial encryption with Key A
        byte[] encryptedWithA = encryptionService.encryptWithKey(originalData, keyA);

        // 2. Key rotation: re-encrypt from Key A to Key B
        byte[] encryptedWithB = encryptionService.rotateKey(encryptedWithA, keyA, keyB);

        // Old key cannot decrypt rotated payload
        assertThrows(SecurityException.class, () -> encryptionService.decryptWithKey(encryptedWithB, keyA));

        // New key decrypts successfully
        byte[] decryptedWithB = encryptionService.decryptWithKey(encryptedWithB, keyB);
        assertArrayEquals(originalData, decryptedWithB);
        assertEquals(expectedHash, sha256(decryptedWithB));

        // 3. Backup and restore procedure
        String exportedBackup = EncryptionService.exportKey(keyB);
        assertNotNull(exportedBackup);

        SecretKey restoredKey = EncryptionService.parseKey(exportedBackup);
        byte[] restoredDecrypted = encryptionService.decryptWithKey(encryptedWithB, restoredKey);
        assertArrayEquals(originalData, restoredDecrypted);
        assertEquals(expectedHash, sha256(restoredDecrypted));
    }

    private String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }
}
