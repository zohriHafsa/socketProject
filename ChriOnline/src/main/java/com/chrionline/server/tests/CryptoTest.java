package com.chrionline.server.tests;

import com.chrionline.shared.security.AESCipher;
import com.chrionline.shared.security.RSAUtils;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

/**
 * CryptoTest — standalone test runner for AESCipher + RSAUtils
 * Run main() directly, no server needed.
 */
public class CryptoTest {

    // ── Console colors ──────────────────────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BOLD   = "\u001B[1m";
    private static final String LINE   = "─────────────────────────────────────────────────";

    private static int passed = 0;
    private static int failed = 0;

    // ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        header("CHRIONLINE — CRYPTO TEST SUITE");

        // ── AESCipher tests ──────────────────────────────────
        section("AESCipher Tests");
        testAesEncryptDecrypt();
        testAesDecryptProducesSamePlaintext();
        testAesUniqueIvEachEncrypt();
        testAesTamperDetection();
        testAesWrongKeyFails();
        testAesKeySerializationRoundtrip();

        // ── RSAUtils tests ───────────────────────────────────
        section("RSAUtils Tests");
        testRsaKeyPairGeneration();
        testRsaEncryptDecryptAesKey();
        testRsaWrongPrivateKeyFails();
        testRsaPublicKeyEncodeDecode();

        // ── Full handshake simulation ────────────────────────
        section("Full Handshake Simulation");
        testFullHandshake();

        // ── Summary ──────────────────────────────────────────
        summary();
    }

    // ══════════════════════════════════════════════════════════
    // AESCipher Tests
    // ══════════════════════════════════════════════════════════

    private static void testAesEncryptDecrypt() {
        String name = "AES encrypt() returns non-empty Base64 string";
        try {
            SecretKey key = AESCipher.generateKey();
            String encrypted = AESCipher.encrypt("hello world", key);
            assertTrue(name,
                    encrypted != null && !encrypted.isEmpty() && isBase64(encrypted)
            );
        } catch (Exception e) { fail(name, e); }
    }

    private static void testAesDecryptProducesSamePlaintext() {
        String name = "AES decrypt(encrypt(x)) == x";
        try {
            SecretKey key = AESCipher.generateKey();
            String original  = "{\"action\":\"LOGIN\",\"data\":{\"username\":\"alice\"}}";
            String encrypted = AESCipher.encrypt(original, key);
            String decrypted = AESCipher.decrypt(encrypted, key);
            assertTrue(name, original.equals(decrypted));
        } catch (Exception e) { fail(name, e); }
    }

    private static void testAesUniqueIvEachEncrypt() {
        String name = "AES produces different ciphertext each call (unique IV)";
        try {
            SecretKey key = AESCipher.generateKey();
            String plain = "same plaintext";
            String enc1  = AESCipher.encrypt(plain, key);
            String enc2  = AESCipher.encrypt(plain, key);
            // Same plaintext → different ciphertext (different IV each time)
            assertTrue(name, !enc1.equals(enc2));
        } catch (Exception e) { fail(name, e); }
    }

    private static void testAesTamperDetection() {
        String name = "AES-GCM rejects tampered ciphertext (AEADBadTagException)";
        try {
            SecretKey key = AESCipher.generateKey();
            String encrypted = AESCipher.encrypt("sensitive data", key);

            // Flip one byte in the middle of the raw bytes
            byte[] raw = Base64.getDecoder().decode(encrypted);
            raw[raw.length / 2] ^= 0xFF;
            String tampered = Base64.getEncoder().encodeToString(raw);

            try {
                AESCipher.decrypt(tampered, key);
                fail(name, "Expected exception but decryption succeeded");
            } catch (AEADBadTagException e) {
                pass(name, "AEADBadTagException thrown as expected");
            } catch (Exception e) {
                // Some JVMs wrap AEADBadTagException — still a pass
                pass(name, e.getClass().getSimpleName() + " thrown (GCM rejected tamper)");
            }
        } catch (Exception e) { fail(name, e); }
    }

    private static void testAesWrongKeyFails() {
        String name = "AES decryption with wrong key fails";
        try {
            SecretKey correctKey = AESCipher.generateKey();
            SecretKey wrongKey   = AESCipher.generateKey();
            String encrypted = AESCipher.encrypt("secret message", correctKey);

            try {
                AESCipher.decrypt(encrypted, wrongKey);
                fail(name, "Expected exception but decryption succeeded");
            } catch (Exception e) {
                pass(name, "Exception thrown with wrong key: " + e.getClass().getSimpleName());
            }
        } catch (Exception e) { fail(name, e); }
    }

    private static void testAesKeySerializationRoundtrip() {
        String name = "AES keyToBase64 → keyFromBytes roundtrip preserves key";
        try {
            SecretKey original   = AESCipher.generateKey();
            String    base64     = AESCipher.keyToBase64(original);
            SecretKey restored   = AESCipher.keyFromBytes(Base64.getDecoder().decode(base64));

            // Encrypt with original, decrypt with restored
            String plain     = "roundtrip test payload";
            String encrypted = AESCipher.encrypt(plain, original);
            String decrypted = AESCipher.decrypt(encrypted, restored);

            assertTrue(name, plain.equals(decrypted));
        } catch (Exception e) { fail(name, e); }
    }

    // ══════════════════════════════════════════════════════════
    // RSAUtils Tests
    // ══════════════════════════════════════════════════════════

    private static void testRsaKeyPairGeneration() {
        String name = "RSA generateKeyPair() returns non-null keypair";
        try {
            KeyPair kp = RSAUtils.generateKeyPair();
            assertTrue(name, kp != null && kp.getPublic() != null && kp.getPrivate() != null);
        } catch (Exception e) { fail(name, e); }
    }

    private static void testRsaEncryptDecryptAesKey() {
        String name = "RSA encrypt(AES key) → decrypt → same bytes";
        try {
            KeyPair   kp      = RSAUtils.generateKeyPair();
            SecretKey aesKey  = AESCipher.generateKey();
            byte[]    raw     = aesKey.getEncoded();

            byte[] encrypted  = RSAUtils.encryptWithPublicKey(raw, kp.getPublic());
            byte[] decrypted  = RSAUtils.decryptWithPrivateKey(encrypted, kp.getPrivate());

            assertTrue(name, java.util.Arrays.equals(raw, decrypted));
        } catch (Exception e) { fail(name, e); }
    }

    private static void testRsaWrongPrivateKeyFails() {
        String name = "RSA decrypt with wrong private key fails";
        try {
            KeyPair kp1 = RSAUtils.generateKeyPair();
            KeyPair kp2 = RSAUtils.generateKeyPair(); // different pair
            SecretKey aesKey = AESCipher.generateKey();

            byte[] encrypted = RSAUtils.encryptWithPublicKey(
                    aesKey.getEncoded(), kp1.getPublic()
            );

            try {
                RSAUtils.decryptWithPrivateKey(encrypted, kp2.getPrivate());
                fail(name, "Expected exception but decryption succeeded");
            } catch (Exception e) {
                pass(name, "Exception thrown with wrong private key: "
                        + e.getClass().getSimpleName());
            }
        } catch (Exception e) { fail(name, e); }
    }

    private static void testRsaPublicKeyEncodeDecode() {
        String name = "RSA encodePublicKey → decodePublicKey roundtrip";
        try {
            KeyPair    kp          = RSAUtils.generateKeyPair();
            String     encoded     = RSAUtils.encodePublicKey(kp.getPublic());
            PublicKey  decoded     = RSAUtils.decodePublicKey(encoded);

            // Verify the decoded key actually works for encryption
            SecretKey aesKey    = AESCipher.generateKey();
            byte[]    encrypted = RSAUtils.encryptWithPublicKey(aesKey.getEncoded(), decoded);
            byte[]    decrypted = RSAUtils.decryptWithPrivateKey(encrypted, kp.getPrivate());

            assertTrue(name, java.util.Arrays.equals(aesKey.getEncoded(), decrypted));
        } catch (Exception e) { fail(name, e); }
    }

    // ══════════════════════════════════════════════════════════
    // Full Handshake Simulation
    // ══════════════════════════════════════════════════════════

    private static void testFullHandshake() {
        String name = "Full handshake: RSA key exchange → AES channel → message roundtrip";
        try {
            // ── SERVER side ──
            KeyPair serverRsa      = RSAUtils.generateKeyPair();
            String  pubKeyBase64   = RSAUtils.encodePublicKey(serverRsa.getPublic());

            // ── CLIENT side ──
            PublicKey serverPubKey = RSAUtils.decodePublicKey(pubKeyBase64);
            SecretKey clientAes    = AESCipher.generateKey();
            byte[]    encAesKey    = RSAUtils.encryptWithPublicKey(
                    clientAes.getEncoded(), serverPubKey);
            String    encAesB64    = Base64.getEncoder().encodeToString(encAesKey);

            // ── SERVER recovers AES key ──
            byte[]    decAesBytes  = RSAUtils.decryptWithPrivateKey(
                    Base64.getDecoder().decode(encAesB64),
                    serverRsa.getPrivate());
            SecretKey serverAes    = AESCipher.keyFromBytes(decAesBytes);

            // ── Verify both sides have the same key ──
            boolean keysMatch = AESCipher.keyToBase64(clientAes)
                    .equals(AESCipher.keyToBase64(serverAes));
            assertTrue(name + " [keys match]", keysMatch);

            // ── CLIENT sends encrypted request ──
            String request       = "{\"action\":\"LOGIN\",\"data\":{\"user\":\"alice\"}}";
            String encRequest    = AESCipher.encrypt(request, clientAes);
            String decRequest    = AESCipher.decrypt(encRequest, serverAes);
            assertTrue(name + " [request intact]", request.equals(decRequest));

            // ── SERVER sends encrypted response ──
            String response      = "{\"success\":true,\"token\":\"abc123\"}";
            String encResponse   = AESCipher.encrypt(response, serverAes);
            String decResponse   = AESCipher.decrypt(encResponse, clientAes);
            assertTrue(name + " [response intact]", response.equals(decResponse));

        } catch (Exception e) { fail(name, e); }
    }

    // ══════════════════════════════════════════════════════════
    // Test helpers
    // ══════════════════════════════════════════════════════════

    private static void assertTrue(String name, boolean condition) {
        if (condition) pass(name, null);
        else           fail(name, "Assertion failed");
    }

    private static void pass(String name, String detail) {
        passed++;
        System.out.println(GREEN + "  ✅ PASS" + RESET
                + " — " + name
                + (detail != null ? CYAN + "  (" + detail + ")" + RESET : ""));
    }

    private static void fail(String name, String reason) {
        failed++;
        System.out.println(RED + "  ❌ FAIL" + RESET
                + " — " + name
                + RED + "  → " + reason + RESET);
    }

    private static void fail(String name, Exception e) {
        failed++;
        System.out.println(RED + "  ❌ FAIL" + RESET
                + " — " + name
                + RED + "  → " + e.getClass().getSimpleName()
                + ": " + e.getMessage() + RESET);
    }

    private static boolean isBase64(String s) {
        try { Base64.getDecoder().decode(s); return true; }
        catch (Exception e) { return false; }
    }

    private static void header(String title) {
        System.out.println("\n" + BOLD + CYAN);
        System.out.println("  ╔═══════════════════════════════════════════════╗");
        System.out.printf ("  ║  %-45s║%n", title);
        System.out.println("  ╚═══════════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    private static void section(String title) {
        System.out.println("\n" + BOLD + YELLOW + "  ▶ " + title + RESET);
        System.out.println("  " + LINE);
    }

    private static void summary() {
        int total = passed + failed;
        System.out.println("\n" + BOLD + "  " + LINE);
        System.out.printf("  Results: %s%d/%d passed%s",
                failed == 0 ? GREEN : YELLOW, passed, total, RESET);
        if (failed > 0)
            System.out.printf("  %s%d failed%s", RED, failed, RESET);
        System.out.println("\n  " + LINE + RESET + "\n");
    }
}