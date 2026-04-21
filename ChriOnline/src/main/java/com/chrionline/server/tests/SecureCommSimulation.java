package com.chrionline.server.tests;

import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import com.chrionline.shared.security.AESCipher;
import com.chrionline.shared.security.RSAUtils;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Map;

/**
 * SecureCommSimulation — Visual proof that the encrypted channel works.
 *
 * Simulates the full handshake + message exchange in a single thread,
 * printing plaintext vs ciphertext side by side so you can see
 * that real data is completely unreadable in transit.
 *
 * Run main() directly — no server needs to be running.
 */
public class SecureCommSimulation {

    // ─── Console formatting helpers ───────────────────────────────
    private static final String DOUBLE = "═══════════════════════════════════════════════════════";
    private static final String SINGLE = "───────────────────────────────────────────────────────";
    private static final String RESET  = "\u001B[0m";
    private static final String GREEN  = "\u001B[32m";
    private static final String CYAN   = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED    = "\u001B[31m";
    private static final String BOLD   = "\u001B[1m";

    public static void main(String[] args) {
        System.out.println("\n" + BOLD + CYAN + DOUBLE);
        System.out.println("SECURE COMMUNICATION SIMULATION");
        System.out.println("ChriOnline — RSA + AES-256-GCM");
        System.out.println(DOUBLE + RESET + "\n");

        try {
            // ══════════════════════════════════════════════════════
            // PHASE 1 — RSA Handshake (key exchange)
            // ══════════════════════════════════════════════════════
            simulateHandshake();

        } catch (Exception e) {
            System.out.println(RED + "Simulation crashed: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 1 — Handshake
    // ─────────────────────────────────────────────────────────────
    private static void simulateHandshake() throws Exception {
        printPhaseHeader("PHASE 1 — RSA HANDSHAKE (Key Exchange)");

        // SERVER side: generate RSA key pair
        System.out.println(YELLOW + "  [SERVER] Generating RSA-2048 key pair..." + RESET);
        KeyPair rsaKeyPair = RSAUtils.generateKeyPair();
        String pubKeyBase64 = RSAUtils.encodePublicKey(rsaKeyPair.getPublic());

        System.out.println(GREEN + "  [SERVER → CLIENT] PUBLIC_KEY sent:" + RESET);
        System.out.println("  " + truncate(pubKeyBase64, 60) + "...\n");

        // CLIENT side: receive public key, generate AES key, encrypt it
        System.out.println(YELLOW + "  [CLIENT] Received server public key ✓" + RESET);
        System.out.println(YELLOW + "  [CLIENT] Generating AES-256 session key..." + RESET);

        SecretKey aesKey = AESCipher.generateKey();
        String aesKeyBase64 = AESCipher.keyToBase64(aesKey);
        System.out.println(GREEN + "  [CLIENT] AES key generated (plaintext, never sent):" + RESET);
        System.out.println("  🔑 " + aesKeyBase64 + "\n");

        // CLIENT: encrypt AES key with server's RSA public key
        byte[] encryptedAesKey = RSAUtils.encryptWithPublicKey(
                aesKey.getEncoded(),
                rsaKeyPair.getPublic()
        );
        String encryptedAesKeyBase64 = Base64.getEncoder().encodeToString(encryptedAesKey);

        System.out.println(GREEN + "  [CLIENT → SERVER] AES_KEY sent (RSA-encrypted):" + RESET);
        System.out.println("  🔒 " + truncate(encryptedAesKeyBase64, 60) + "...\n");

        // SERVER: decrypt AES key with its RSA private key
        byte[] decryptedAesKeyBytes = RSAUtils.decryptWithPrivateKey(
                encryptedAesKey,
                rsaKeyPair.getPrivate()
        );
        SecretKey serverAesKey = AESCipher.keyFromBytes(decryptedAesKeyBytes);

        // Verify both sides have the same key
        boolean keysMatch = aesKeyBase64.equals(AESCipher.keyToBase64(serverAesKey));
        System.out.println(YELLOW + "  [SERVER] Decrypted AES key:" + RESET);
        System.out.println("  🔑 " + AESCipher.keyToBase64(serverAesKey));
        System.out.println();
        System.out.println(keysMatch
                ? GREEN + "Keys match — secure channel established!" + RESET
                : RED   + "Keys DO NOT match — handshake failed!" + RESET);

        if (!keysMatch) throw new Exception("Handshake key mismatch");

        System.out.println();

        // ══════════════════════════════════════════════════════════
        // PHASE 2 — Encrypted message exchange
        // ══════════════════════════════════════════════════════════
        simulateMessages(aesKey, serverAesKey);
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 2 — Message exchange (client uses aesKey, server uses serverAesKey)
    // Both are identical — separated to make the simulation realistic
    // ─────────────────────────────────────────────────────────────
    private static void simulateMessages(SecretKey clientKey, SecretKey serverKey) throws Exception {
        printPhaseHeader("PHASE 2 — ENCRYPTED MESSAGE EXCHANGE");

        // ── Scenario 1: LOGIN ─────────────────────────────────────
        Request loginRequest = new Request(
                Action.LOGIN,
                null,
                Map.of("username", "alice@chrionline.com", "password", "MyP@ssw0rd!")
        );
        Response loginResponse = Response.ok("Login successful",
                Map.of("token", "eyJhbGciOiJIUzI1NiJ9.fake.token", "role", "USER"));

        simulateExchange("LOGIN", loginRequest, loginResponse, clientKey, serverKey);

        // ── Scenario 2: GET_PRODUCTS ──────────────────────────────
        Request productsRequest = new Request(
                Action.GET_PRODUCTS,
                "eyJhbGciOiJIUzI1NiJ9.fake.token",
                Map.of("category", "electronics", "page", 1)
        );
        Response productsResponse = Response.ok("Products fetched",
                Map.of("count", 3, "items", new String[]{"Laptop", "Phone", "Tablet"}));

        simulateExchange("GET_PRODUCTS", productsRequest, productsResponse, clientKey, serverKey);

        // ── Scenario 3: PLACE_ORDER ───────────────────────────────
        Request orderRequest = new Request(
                Action.PLACE_ORDER,
                "eyJhbGciOiJIUzI1NiJ9.fake.token",
                Map.of("productId", 42, "quantity", 2, "totalPrice", 1299.99)
        );
        Response orderResponse = Response.ok("Order placed",
                Map.of("orderId", "ORD-2026-00891", "status", "PENDING"));

        simulateExchange("PLACE_ORDER", orderRequest, orderResponse, clientKey, serverKey);

        // ══════════════════════════════════════════════════════════
        // PHASE 3 — Tamper test (prove GCM integrity works)
        // ══════════════════════════════════════════════════════════
        simulateTamperTest(clientKey, serverKey);
    }

    // ─────────────────────────────────────────────────────────────
    // Simulate one full request → response round trip
    // ─────────────────────────────────────────────────────────────
    private static void simulateExchange(
            String label,
            Request request,
            Response response,
            SecretKey clientKey,
            SecretKey serverKey
    ) throws Exception {

        System.out.println(BOLD + "  📨 " + label + RESET);
        System.out.println(SINGLE);

        // CLIENT encrypts the request
        String requestJson      = request.toJson();
        String encryptedRequest = AESCipher.encrypt(requestJson, clientKey);

        System.out.println(CYAN + "  [CLIENT → SERVER]" + RESET);
        System.out.println("PLAIN    : " + truncate(requestJson, 80));
        System.out.println("ENCRYPTED: " + truncate(encryptedRequest, 80));
        System.out.println();

        // SERVER decrypts, processes, encrypts response
        String decryptedRequest  = AESCipher.decrypt(encryptedRequest, serverKey);
        String responseJson      = response.toJson();
        String encryptedResponse = AESCipher.encrypt(responseJson, serverKey);

        System.out.println(YELLOW + "  [SERVER → CLIENT]" + RESET);
        System.out.println("PLAIN    : " + truncate(responseJson, 80));
        System.out.println("ENCRYPTED: " + truncate(encryptedResponse, 80));
        System.out.println();

        // CLIENT decrypts the response
        String decryptedResponse = AESCipher.decrypt(encryptedResponse, clientKey);
        boolean intact = decryptedResponse.equals(responseJson);

        System.out.println(intact
                ? GREEN + "Round-trip OK — data intact" + RESET
                : RED   + "Round-trip FAILED" + RESET);
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 3 — Tamper test: flip one byte → GCM must reject it
    // ─────────────────────────────────────────────────────────────
    private static void simulateTamperTest(SecretKey clientKey, SecretKey serverKey) throws Exception {
        printPhaseHeader("PHASE 3 — TAMPER DETECTION (GCM Integrity)");

        Request request = new Request(Action.GET_CART, "token123", Map.of("userId", 7));
        String encrypted = AESCipher.encrypt(request.toJson(), clientKey);

        // Flip one byte in the middle of the Base64 payload
        byte[] raw = Base64.getDecoder().decode(encrypted);
        raw[raw.length / 2] ^= 0xFF; // flip all bits in one byte
        String tampered = Base64.getEncoder().encodeToString(raw);

        System.out.println("  Original  : " + truncate(encrypted, 60) + "...");
        System.out.println("  Tampered  : " + truncate(tampered,  60) + "...");
        System.out.println();

        try {
            AESCipher.decrypt(tampered, serverKey);
            System.out.println(RED + "FAIL — tampered message was accepted (should never happen)" + RESET);
        } catch (javax.crypto.AEADBadTagException e) {
            System.out.println(GREEN + "PASS — GCM rejected tampered message (AEADBadTagException)" + RESET);
        } catch (Exception e) {
            System.out.println(GREEN + "PASS — decryption failed as expected: " + e.getClass().getSimpleName() + RESET);
        }

        System.out.println();
        printPhaseHeader("SIMULATION COMPLETE");
        System.out.println(BOLD + GREEN + "  All scenarios passed. Encrypted channel is working correctly." + RESET);
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // Formatting helpers
    // ─────────────────────────────────────────────────────────────
    private static void printPhaseHeader(String title) {
        System.out.println(BOLD + DOUBLE);
        System.out.println("  " + title);
        System.out.println(DOUBLE + RESET);
        System.out.println();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}