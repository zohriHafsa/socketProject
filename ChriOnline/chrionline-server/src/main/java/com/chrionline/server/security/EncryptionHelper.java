package com.chrionline.server.security;

import com.chrionline.shared.security.AESCipher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class EncryptionHelper {

    // Encrypt a value for storage — returns null if input is null
    public static String encrypt(String value) {
        if (value == null || value.isEmpty()) return value;
        try {
            return AESCipher.encrypt(value, ServerMasterKey.get());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    // Decrypt a value from DB — returns null if input is null
    public static String decrypt(String value) {
        if (value == null || value.isEmpty()) return value;
        try {
            return AESCipher.decrypt(value, ServerMasterKey.get());
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    // SHA-256 hash of email for WHERE clause searching
    // Deterministic — same email always produces same hash
    public static String hashEmail(String email) {
        if (email == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    email.toLowerCase().trim().getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed: " + e.getMessage(), e);
        }
    }
}