package com.chrionline.shared.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AESCipher {

    private static final String ALGORITHM      = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH      = 12;   // 96 bits — GCM standard
    private static final int    TAG_LENGTH     = 128;  // bits — GCM auth tag
    private static final int    KEY_SIZE       = 256;  // bits — AES-256


    //Generate a random AES-256 session key

    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(KEY_SIZE, new SecureRandom());
        return keyGen.generateKey();
    }

    // Encrypt — returns Base64(IV + ciphertext + tag)
    public static String encrypt(String plaintext, SecretKey key) throws Exception {
        // Fresh random IV for every single message — CRITICAL for GCM security
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Prepend IV so the receiver can extract it
        byte[] ivPlusCipher = new byte[IV_LENGTH + ciphertext.length];
        System.arraycopy(iv,         0, ivPlusCipher, 0,         IV_LENGTH);
        System.arraycopy(ciphertext, 0, ivPlusCipher, IV_LENGTH, ciphertext.length);

        return Base64.getEncoder().encodeToString(ivPlusCipher);
    }

    // 3. Decrypt — splits IV, decrypts, returns plaintext
    //    Throws AEADBadTagException if data was tampered
    public static String decrypt(String base64Data, SecretKey key) throws Exception {
        byte[] ivPlusCipher = Base64.getDecoder().decode(base64Data);

        // Extract the 12-byte IV from the front
        byte[] iv         = new byte[IV_LENGTH];
        byte[] ciphertext = new byte[ivPlusCipher.length - IV_LENGTH];
        System.arraycopy(ivPlusCipher, 0,         iv,         0, IV_LENGTH);
        System.arraycopy(ivPlusCipher, IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, "UTF-8");
    }

    // Serialize key → Base64 string (for logging/debug only)
    public static String keyToBase64(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // Rebuild SecretKey from raw bytes (after RSA decrypt)
    public static SecretKey keyFromBytes(byte[] raw) {
        return new SecretKeySpec(raw, "AES");
    }
}