package com.chrionline.server.tests;

import com.chrionline.server.security.AntiReplayFilter;
import com.chrionline.shared.security.AESCipher;
import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import java.util.Base64;

public class SecurityTests {

    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        System.out.println("=== SECURITY TESTS ===");
        testInterception();
        testReplayAttack();
        testIntegrityTamper();
        testWrongKey();
        System.out.println("Results: " + passed + "/"
                + (passed+failed) + " passed");
    }

    private static void testInterception() {
        String name = "Interception — encrypted data is unreadable";
        try {
            SecretKey key = AESCipher.generateKey();
            String enc = AESCipher.encrypt("secret", key);
            // Attacker does NOT have the key, tries to read raw Base64
            // The raw Base64 is not the plaintext — test passes
            boolean isReadable = enc.contains("secret");
            if (!isReadable) pass(name);
            else            fail(name, "Plaintext visible in ciphertext!");
        } catch (Exception e) { fail(name, e.getMessage()); }
    }

    private static void testReplayAttack() {
        String name = "Replay Attack — same nonce rejected";
        try {
            AntiReplayFilter filter = AntiReplayFilter.getInstance();
            String nonce = java.util.UUID.randomUUID().toString();
            long ts = System.currentTimeMillis();
            boolean first  = filter.validate(nonce, ts);
            boolean second = filter.validate(nonce, ts); // replay
            if (first && !second) pass(name);
            else fail(name, "Replay was accepted! first=" + first
                    + " second=" + second);
        } catch (Exception e) { fail(name, e.getMessage()); }
    }

    private static void testIntegrityTamper() {
        String name = "Integrity — tampered message rejected by GCM";
        try {
            SecretKey key = AESCipher.generateKey();
            String enc = AESCipher.encrypt("important data", key);
            byte[] raw = Base64.getDecoder().decode(enc);
            raw[raw.length / 2] ^= 0xFF; // flip bits
            String tampered = Base64.getEncoder().encodeToString(raw);
            try {
                AESCipher.decrypt(tampered, key);
                fail(name, "Tampered message was accepted!");
            } catch (AEADBadTagException e) {
                pass(name);
            } catch (Exception e) {
                pass(name); // wrapped AEADBadTagException
            }
        } catch (Exception e) { fail(name, e.getMessage()); }
    }

    private static void testWrongKey() {
        String name = "Wrong Key — decryption fails with different key";
        try {
            SecretKey keyA = AESCipher.generateKey();
            SecretKey keyB = AESCipher.generateKey();
            String enc = AESCipher.encrypt("classified", keyA);
            try {
                AESCipher.decrypt(enc, keyB);
                fail(name, "Wrong key accepted!");
            } catch (Exception e) {
                pass(name);
            }
        } catch (Exception e) { fail(name, e.getMessage()); }
    }

    private static void pass(String name) {
        passed++;
        System.out.println("PASS [OK] — " + name);
    }
    private static void fail(String name, String reason) {
        failed++;
        System.out.println("FAIL [XX] — " + name + " -> " + reason);
    }
}
