package com.passport.common;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Enterprise AES-256-GCM Cryptographic Engine
 * Provides authenticated encryption for sensitive student PII, portfolio tokens, and confidential records.
 */
@Component
public class CryptoEngine {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH_BIT = 256;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String masterSecret = "SkillsEvidencePassport_AES256_MasterKey_2026";

    /**
     * Encrypts plaintext string using AES-256-GCM with a fresh random IV and salt.
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;
        try {
            byte[] salt = new byte[SALT_LENGTH_BYTE];
            secureRandom.nextBytes(salt);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            SecretKey secretKey = deriveKey(masterSecret, salt);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Format: Base64(salt + iv + cipherText)
            byte[] combined = new byte[salt.length + iv.length + cipherText.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(cipherText, 0, combined, salt.length + iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new SecurityException("Cryptographic encryption failure", e);
        }
    }

    /**
     * Decrypts AES-256-GCM ciphertext payload.
     */
    public String decrypt(String base64Combined) {
        if (base64Combined == null || base64Combined.isBlank()) return base64Combined;
        try {
            byte[] combined = Base64.getDecoder().decode(base64Combined);
            if (combined.length < (SALT_LENGTH_BYTE + IV_LENGTH_BYTE + 1)) {
                return base64Combined; // Return as-is if not in cipher envelope
            }

            byte[] salt = new byte[SALT_LENGTH_BYTE];
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] cipherText = new byte[combined.length - SALT_LENGTH_BYTE - IV_LENGTH_BYTE];

            System.arraycopy(combined, 0, salt, 0, salt.length);
            System.arraycopy(combined, SALT_LENGTH_BYTE, iv, 0, iv.length);
            System.arraycopy(combined, SALT_LENGTH_BYTE + IV_LENGTH_BYTE, cipherText, 0, cipherText.length);

            SecretKey secretKey = deriveKey(masterSecret, salt);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // If decryption fails, safely fallback or throw
            return base64Combined;
        }
    }

    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BIT);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}
