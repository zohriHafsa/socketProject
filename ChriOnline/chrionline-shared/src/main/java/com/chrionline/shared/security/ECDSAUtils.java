package com.chrionline.shared.security;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public class ECDSAUtils {

    /**
     * Generates a new ECDSA KeyPair using the secp256r1 curve.
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        return generator.generateKeyPair();
    }

    /**
     * Encodes an EC PublicKey to a Base64 string for transmission/storage.
     */
    public static String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Decodes a Base64 string back into an EC PublicKey.
     */
    public static PublicKey decodePublicKey(String base64PubKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PubKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePublic(spec);
    }

    /**
     * Signs a challenge using SHA256withECDSA.
     */
    public static byte[] sign(String challenge, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(challenge.getBytes());
        return signature.sign();
    }

    /**
     * Verifies a signature against a challenge using SHA256withECDSA.
     */
    public static boolean verify(String challenge, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(publicKey);
        signature.update(challenge.getBytes());
        return signature.verify(signatureBytes);
    }

    /**
     * Generates an EC keypair into a PKCS12 KeyStore file (.p12) using keytool,
     * then returns the PublicKey for server registration.
     * Replaces the old manual AES/PBKDF2 encryption approach.
     */
    public static PublicKey generateAndSaveKeyStore(String username, File file, String pass) throws Exception {
        String keytoolPath = System.getProperty("java.home")
                + File.separator + "bin" + File.separator + "keytool";

        if (file.exists()) {
            file.delete();
        }

        // Sanitize CN to prevent DN injection
        String cn = username.replaceAll("[,=\"+<>;\\\\]", "");

        ProcessBuilder pb = new ProcessBuilder(
            keytoolPath, "-genkeypair",
            "-alias",     "admin-key",
            "-keyalg",    "EC",
            "-keysize",   "256",
            "-storetype", "PKCS12",
            "-keystore",  file.getAbsolutePath(),
            "-storepass", pass,
            "-validity",  "3650",
            "-dname",     "CN=" + cn + ", OU=Admin, O=ChriOnline"
        );
        pb.redirectErrorStream(true);

        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            java.util.Scanner s = new java.util.Scanner(p.getInputStream()).useDelimiter("\\A");
            String output = s.hasNext() ? s.next() : "(no output)";
            throw new Exception("keytool failed (exit " + exitCode + "): " + output);
        }

        // Read back the KeyStore and extract the public key
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(file)) {
            ks.load(fis, pass.toCharArray());
        }
        java.security.cert.Certificate cert = ks.getCertificate("admin-key");
        return cert.getPublicKey();
    }

    /**
     * Loads the PrivateKey from a PKCS12 KeyStore file (.p12).
     * Replaces the old manual AES/PBKDF2 decryption approach.
     */
    public static PrivateKey loadPrivateKeyFromKeyStore(File file, String pass) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(file)) {
            ks.load(fis, pass.toCharArray());
        }
        return (PrivateKey) ks.getKey("admin-key", pass.toCharArray());
    }
}
