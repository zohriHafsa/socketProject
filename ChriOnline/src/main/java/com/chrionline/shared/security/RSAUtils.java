package com.chrionline.shared.security;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;

public class RSAUtils {

    /**
     * Generates a new 2048-bit RSA KeyPair
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair();
    }

    /**
     * Encodes a PublicKey to a Base64 string for transmission/storage.
     */
    public static String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Decodes a Base64 string back into an RSA PublicKey.
     */
    public static PublicKey decodePublicKey(String base64PubKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PubKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    /**
     * Signs a challenge using SHA256withRSA.
     */
    public static byte[] sign(String challenge, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(challenge.getBytes());
        return signature.sign();
    }

    /**
     * Verifies a signature against a challenge using SHA256withRSA.
     */
    public static boolean verify(String challenge, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(challenge.getBytes());
        return signature.verify(signatureBytes);
    }

    /**
     * Encrypts the PrivateKey securely using AES-256 derived from a passphrase and saves it to a file.
     * This protects the admin's private key at rest on their desktop.
     */
    public static void savePrivateKeyEncrypted(PrivateKey privateKey, File file, String pass) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(pass.toCharArray(), salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = cipher.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        
        byte[] encryptedKey = cipher.doFinal(privateKey.getEncoded());
        
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.write(salt);
            dos.write(iv);
            dos.write(encryptedKey);
        }
    }

    /**
     * Loads an encrypted PrivateKey from a file and decrypts it using the passphrase.
     */
    public static PrivateKey loadPrivateKeyDecrypted(File file, String pass) throws Exception {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            byte[] salt = new byte[16];
            dis.readFully(salt);
            
            byte[] iv = new byte[16];
            dis.readFully(iv);
            
            byte[] encryptedKey = dis.readAllBytes();
            
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(pass.toCharArray(), salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            byte[] pkcs8Bytes = cipher.doFinal(encryptedKey);
            
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec);
        }
    }
}
