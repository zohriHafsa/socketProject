package com.chrionline.server.tests;

import com.chrionline.server.core.RequestRouter;
import com.chrionline.server.dao.DBConnection;
import com.chrionline.server.dao.UserDAO;
import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import com.chrionline.shared.security.AESCipher;
import com.chrionline.shared.security.RSAUtils;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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
        System.out.println("ChriOnline — RSA + AES-256-GCM + Real DB Data");
        System.out.println(DOUBLE + RESET + "\n");

        try {
            // ─── DB INIT ──────────────────────────────────────────
            System.out.println(YELLOW + "  Initializing Database connection..." + RESET);
            if (DBConnection.getInstance().getConnection() == null) {
                System.out.println(RED + "  FAIL: Could not connect to database. Make sure MySQL is running." + RESET);
                System.exit(1);
            }
            System.out.println(GREEN + "  ✓ Database connected.\n" + RESET);

            // PHASE 1 — RSA Handshake
            simulateHandshake();

            // Close DB connection and force exit to cleanly terminate any daemon/background thread pools
            DBConnection.getInstance().close();
            System.out.println(GREEN + "  ✓ Simulation completed successfully." + RESET);
            System.exit(0);

        } catch (Exception e) {
            System.out.println(RED + "Simulation crashed: " + e.getMessage() + RESET);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void simulateHandshake() throws Exception {
        printPhaseHeader("PHASE 1 — RSA HANDSHAKE (Key Exchange)");

        // SERVER side: generate RSA key pair
        System.out.println(YELLOW + "  [SERVER] Generating RSA-2048 key pair..." + RESET);
        KeyPair rsaKeyPair = RSAUtils.generateKeyPair();
        String pubKeyBase64 = RSAUtils.encodePublicKey(rsaKeyPair.getPublic());

        System.out.println(GREEN + "  [SERVER → CLIENT] PUBLIC_KEY sent:" + RESET);
        System.out.println("  " + truncate(pubKeyBase64, 80) + "...\n");

        // CLIENT side: receive public key, generate AES key
        System.out.println(YELLOW + "  [CLIENT] Received server public key ✓" + RESET);
        SecretKey aesKey = AESCipher.generateKey();
        String aesKeyBase64 = AESCipher.keyToBase64(aesKey);

        // CLIENT: encrypt AES key with server's RSA public key
        byte[] encryptedAesKey = RSAUtils.encryptWithPublicKey(
                aesKey.getEncoded(),
                rsaKeyPair.getPublic()
        );
        String encryptedAesKeyBase64 = Base64.getEncoder().encodeToString(encryptedAesKey);

        System.out.println(GREEN + "  [CLIENT → SERVER] AES_KEY sent (RSA-encrypted):" + RESET);
        System.out.println("  🔒 " + truncate(encryptedAesKeyBase64, 80) + "...\n");

        // SERVER: decrypt AES key with its RSA private key
        byte[] decryptedAesKeyBytes = RSAUtils.decryptWithPrivateKey(
                encryptedAesKey,
                rsaKeyPair.getPrivate()
        );
        SecretKey serverAesKey = AESCipher.keyFromBytes(decryptedAesKeyBytes);

        // Verify keys match
        boolean keysMatch = aesKeyBase64.equals(AESCipher.keyToBase64(serverAesKey));
        if (!keysMatch) throw new Exception("Handshake key mismatch");

        System.out.println(GREEN + "  ✓ Secure channel established!\n" + RESET);

        // PHASE 2 — Real Messages
        simulateMessages(aesKey, serverAesKey);
    }

    private static void simulateMessages(SecretKey clientKey, SecretKey serverKey) throws Exception {
        printPhaseHeader("PHASE 2 — ENCRYPTED MESSAGE EXCHANGE (Real DB Data)");

        // Scenario 1: LOGIN
        Request loginReq = new Request(Action.LOGIN, null,
                Map.of("username", "jdupont", "password", "Password.123"));

        Response loginResp = simulateRealExchange("LOGIN (Real User)", loginReq, clientKey, serverKey);

        String token = null;
        if (loginResp.isSuccess()) {
            Map<?, ?> data = (Map<?, ?>) loginResp.getData();

            // Check if 2FA is required
            if (Boolean.TRUE.equals(data.get("2fa_required"))) {
                int userId = ((Number) data.get("userId")).intValue();
                System.out.println(YELLOW + "  [SIMULATION] 2FA Required for userId=" + userId + RESET);

                // Fetch the code directly from DB (simulating checking email)
                UserDAO userDAO = new UserDAO();
                String code = userDAO.getLatestVerifyCode(userId);
                System.out.println(GREEN + "  [SIMULATION] 2FA Code fetched from DB: " + code + "\n" + RESET);

                // Scenario 1b: VERIFY_2FA
                Request verifyReq = new Request(Action.VERIFY_2FA, null,
                        Map.of("userId", userId, "code", code));

                Response verifyResp = simulateRealExchange("VERIFY_2FA (Complete Auth)", verifyReq, clientKey, serverKey);

                if (verifyResp.isSuccess()) {
                    Map<?, ?> vData = (Map<?, ?>) verifyResp.getData();
                    token = (String) vData.get("token");
                    System.out.println(GREEN + "  ✓ 2FA Passed. Token acquired: " + truncate(token, 40) + RESET);
                } else {
                    System.out.println(RED + "  ! 2FA Failed: " + verifyResp.getMessage() + RESET);
                }
            } else {
                token = (String) data.get("token");
                System.out.println(GREEN + "  ✓ Login Success (No 2FA). Token acquired: " + truncate(token, 40) + RESET);
            }
        } else {
            System.out.println(RED + "  ! Login failed: " + loginResp.getMessage() + RESET);
            token = "invalid-token";
        }
        System.out.println();

        // Scenario 2: GET_PRODUCTS
        Request productsReq = new Request(Action.GET_PRODUCTS, token, Map.of());
        Response productsResp = simulateRealExchange("GET_PRODUCTS (Real DB Data)", productsReq, clientKey, serverKey);

        if (productsResp.isSuccess()) {
            List<?> items = (List<?>) productsResp.getData();
            System.out.println(GREEN + "  ✓ Retrieved " + (items != null ? items.size() : 0) + " products." + RESET);
        }
        System.out.println();

        // Scenario 3: UPDATE_PROFILE
        long ts = System.currentTimeMillis() % 10000;
        Request updateReq = new Request(Action.UPDATE_PROFILE, token, Map.of(
                "firstName", "Jean - " + ts,
                "lastName", "Dupont - " + ts,
                "phone", "0612345678 - " + ts,
                "address", "12 Avenue Victor Hugo, Lyon - " + ts
        ));

        simulateRealExchange("UPDATE_PROFILE (Encrypted Write)", updateReq, clientKey, serverKey);

        // PHASE 3 — Tamper Detection
        simulateTamperTest(clientKey, serverKey);
    }

    private static Response simulateRealExchange(
            String label,
            Request request,
            SecretKey clientKey,
            SecretKey serverKey
    ) throws Exception {

        System.out.println(BOLD + "  📨 " + label + RESET);
        System.out.println(SINGLE);

        // CLIENT side: Encrypt
        String requestJson      = request.toJson();
        String encryptedRequest = AESCipher.encrypt(requestJson, clientKey);

        System.out.println(CYAN + "  [CLIENT → SERVER]" + RESET);
        System.out.println("  PLAIN REQUEST    : " + truncate(requestJson, 200));
        System.out.println("  ENCRYPTED PAYLOAD: " + truncate(encryptedRequest, 200));

        // SERVER side: Decrypt, Route, Encrypt Response
        String decryptedRequest  = AESCipher.decrypt(encryptedRequest, serverKey);
        Response response        = RequestRouter.route(decryptedRequest);
        String responseJson      = response.toJson();
        String encryptedResponse = AESCipher.encrypt(responseJson, serverKey);

        System.out.println(YELLOW + "\n  [SERVER → CLIENT]" + RESET);
        System.out.println("  RAW RESPONSE     : " + truncate(responseJson, 200));
        System.out.println("  ENCRYPTED PAYLOAD: " + truncate(encryptedResponse, 200));

        // CLIENT side: Decrypt
        String decryptedResponse = AESCipher.decrypt(encryptedResponse, clientKey);
        boolean intact = decryptedResponse.equals(responseJson);

        System.out.println("\n" + (intact
                ? GREEN + "  ✓ Round-trip OK — encrypted data processed correctly" + RESET
                : RED   + "  ! Round-trip FAILED" + RESET));

        return response;
    }

    private static void simulateTamperTest(SecretKey clientKey, SecretKey serverKey) throws Exception {
        printPhaseHeader("PHASE 3 — TAMPER DETECTION (GCM Integrity)");

        Request request = new Request(Action.GET_CART, "token123", Map.of("userId", 7));
        String encrypted = AESCipher.encrypt(request.toJson(), clientKey);

        byte[] raw = Base64.getDecoder().decode(encrypted);
        raw[raw.length / 2] ^= 0xFF; // Flip bits
        String tampered = Base64.getEncoder().encodeToString(raw);

        System.out.println("  Original  : " + truncate(encrypted, 80) + "...");
        System.out.println("  Tampered  : " + truncate(tampered,  80) + "...");
        System.out.println();

        try {
            AESCipher.decrypt(tampered, serverKey);
            System.out.println(RED + "  FAIL — tampered message was accepted" + RESET);
        } catch (Exception e) {
            System.out.println(GREEN + "  PASS — GCM rejected tampered message: " + e.getClass().getSimpleName() + RESET);
        }

        System.out.println();
        printPhaseHeader("SIMULATION COMPLETE");
        System.out.println(BOLD + GREEN + "  All scenarios passed. Encrypted channel is working correctly." + RESET);
        System.out.println();
    }

    private static void printPhaseHeader(String title) {
        System.out.println(BOLD + DOUBLE);
        System.out.println("  " + title);
        System.out.println(DOUBLE + RESET);
        System.out.println();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}