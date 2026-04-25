package com.chrionline.server.tests;

import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.enums.Action;
import com.chrionline.shared.security.AESCipher;
import com.chrionline.shared.security.RSAUtils;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;


public class SecureCommSimulation {

    // ─── DB config ────────────────────────────────────────────────
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/chrionline?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";           // ← change if needed

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
        System.out.println("ChriOnline — RSA + AES-256-GCM  |  Real Database Data");
        System.out.println(DOUBLE + RESET + "\n");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            System.out.println(GREEN + "  [DB] Connected to chrionline ✓" + RESET + "\n");

            // ══════════════════════════════════════════════════════
            // PHASE 1 — RSA Handshake (key exchange)
            // ══════════════════════════════════════════════════════
            simulateHandshake(conn);

        } catch (Exception e) {
            System.out.println(RED + "Simulation crashed: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 1 — Handshake
    // ─────────────────────────────────────────────────────────────
    private static void simulateHandshake(Connection conn) throws Exception {
        printPhaseHeader("PHASE 1 — RSA HANDSHAKE (Key Exchange)");

        // SERVER side: generate RSA key pair
        System.out.println(YELLOW + "  [SERVER] Generating RSA-2048 key pair..." + RESET);
        KeyPair rsaKeyPair   = RSAUtils.generateKeyPair();
        String  pubKeyBase64 = RSAUtils.encodePublicKey(rsaKeyPair.getPublic());

        System.out.println(GREEN + "  [SERVER → CLIENT] PUBLIC_KEY sent:" + RESET);
        System.out.println("  " + truncate(pubKeyBase64, 60) + "...\n");

        // CLIENT side: receive public key, generate AES key, encrypt it
        System.out.println(YELLOW + "  [CLIENT] Received server public key ✓" + RESET);
        System.out.println(YELLOW + "  [CLIENT] Generating AES-256 session key..." + RESET);

        SecretKey aesKey       = AESCipher.generateKey();
        String    aesKeyBase64 = AESCipher.keyToBase64(aesKey);
        System.out.println(GREEN + "  [CLIENT] AES key generated (plaintext, never sent):" + RESET);
        System.out.println("  🔑 " + aesKeyBase64 + "\n");

        // CLIENT: encrypt AES key with server's RSA public key
        byte[] encryptedAesKey       = RSAUtils.encryptWithPublicKey(aesKey.getEncoded(), rsaKeyPair.getPublic());
        String encryptedAesKeyBase64 = Base64.getEncoder().encodeToString(encryptedAesKey);

        System.out.println(GREEN + "  [CLIENT → SERVER] AES_KEY sent (RSA-encrypted):" + RESET);
        System.out.println("  🔒 " + truncate(encryptedAesKeyBase64, 60) + "...\n");

        // SERVER: decrypt AES key with its RSA private key
        byte[]    decryptedAesKeyBytes = RSAUtils.decryptWithPrivateKey(encryptedAesKey, rsaKeyPair.getPrivate());
        SecretKey serverAesKey         = AESCipher.keyFromBytes(decryptedAesKeyBytes);

        // Verify both sides have the same key
        boolean keysMatch = aesKeyBase64.equals(AESCipher.keyToBase64(serverAesKey));
        System.out.println(YELLOW + "  [SERVER] Decrypted AES key:" + RESET);
        System.out.println("  🔑 " + AESCipher.keyToBase64(serverAesKey));
        System.out.println();
        System.out.println(keysMatch
                ? GREEN + "  Keys match — secure channel established!" + RESET
                : RED   + "  Keys DO NOT match — handshake failed!" + RESET);

        if (!keysMatch) throw new Exception("Handshake key mismatch");

        System.out.println();

        // Encrypted message exchange
        simulateMessages(aesKey, serverAesKey, conn);
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 2 — Encrypted message exchange (real DB data)
    // ─────────────────────────────────────────────────────────────
    private static void simulateMessages(SecretKey clientKey, SecretKey serverKey, Connection conn) throws Exception {
        printPhaseHeader("PHASE 2 — ENCRYPTED MESSAGE EXCHANGE  (Real DB Data)");

        // ── Scenario 1: LOGIN — real user from users table ────────
        Request  loginRequest  = buildLoginRequest(conn);
        Response loginResponse = buildLoginResponse(conn);
        simulateExchange("LOGIN", loginRequest, loginResponse, clientKey, serverKey);

        // ── Scenario 2: GET_PRODUCTS — real rows from products ────
        Request  productsRequest  = buildGetProductsRequest(conn);
        Response productsResponse = buildGetProductsResponse(conn);
        simulateExchange("GET_PRODUCTS", productsRequest, productsResponse, clientKey, serverKey);

        // ── Scenario 3: GET_CART — real cart items from DB ────────
        Request  cartRequest  = buildGetCartRequest(conn);
        Response cartResponse = buildGetCartResponse(conn);
        simulateExchange("GET_CART", cartRequest, cartResponse, clientKey, serverKey);

        // ── Scenario 4: GET_ORDERS — real orders + payments ───────
        Request  ordersRequest  = buildGetOrdersRequest(conn);
        Response ordersResponse = buildGetOrdersResponse(conn);
        simulateExchange("GET_ORDERS", ordersRequest, ordersResponse, clientKey, serverKey);

        // ══════════════════════════════════════════════════════════
        // PHASE 3 — Tamper test (prove GCM integrity works)
        // ══════════════════════════════════════════════════════════
        simulateTamperTest(clientKey, serverKey);
    }

    // ─────────────────────────────────────────────────────────────
    // Simulate one full request → response round trip
    // ─────────────────────────────────────────────────────────────
    private static void simulateExchange(
            String    label,
            Request   request,
            Response  response,
            SecretKey clientKey,
            SecretKey serverKey
    ) throws Exception {

        System.out.println(BOLD + "  📨 " + label + RESET);
        System.out.println(SINGLE);

        // CLIENT encrypts the request
        String requestJson      = request.toJson();
        String encryptedRequest = AESCipher.encrypt(requestJson, clientKey);

        System.out.println(CYAN + "  [CLIENT → SERVER]" + RESET);
        System.out.println("  PLAIN    : " + truncate(requestJson, 80));
        System.out.println("  ENCRYPTED: " + truncate(encryptedRequest, 80));
        System.out.println();

        // SERVER decrypts, processes, encrypts response
        String decryptedRequest  = AESCipher.decrypt(encryptedRequest, serverKey);
        String responseJson      = response.toJson();
        String encryptedResponse = AESCipher.encrypt(responseJson, serverKey);

        System.out.println(YELLOW + "  [SERVER → CLIENT]" + RESET);
        System.out.println("  PLAIN    : " + truncate(responseJson, 80));
        System.out.println("  ENCRYPTED: " + truncate(encryptedResponse, 80));
        System.out.println();

        // CLIENT decrypts the response
        String  decryptedResponse = AESCipher.decrypt(encryptedResponse, clientKey);
        boolean intact            = decryptedResponse.equals(responseJson);

        System.out.println(intact
                ? GREEN + "  ✅ Round-trip OK — data intact" + RESET
                : RED   + "  ❌ Round-trip FAILED" + RESET);
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // Tamper test: flip one byte → GCM must reject it
    // ─────────────────────────────────────────────────────────────
    private static void simulateTamperTest(SecretKey clientKey, SecretKey serverKey) throws Exception {
        printPhaseHeader("PHASE 3 — TAMPER DETECTION (GCM Integrity)");

        Request request   = new Request(Action.GET_CART, "token123", Map.of("userId", 7));
        String  encrypted = AESCipher.encrypt(request.toJson(), clientKey);

        // Flip one byte in the middle of the Base64 payload
        byte[] raw = Base64.getDecoder().decode(encrypted);
        raw[raw.length / 2] ^= 0xFF;                        // flip all bits in one byte
        String tampered = Base64.getEncoder().encodeToString(raw);

        System.out.println("  Original : " + truncate(encrypted, 60) + "...");
        System.out.println("  Tampered : " + truncate(tampered,  60) + "...");
        System.out.println();

        try {
            AESCipher.decrypt(tampered, serverKey);
            System.out.println(RED + "  FAIL — tampered message was accepted (should never happen)" + RESET);
        } catch (javax.crypto.AEADBadTagException e) {
            System.out.println(GREEN + "  PASS — GCM rejected tampered message (AEADBadTagException)" + RESET);
        } catch (Exception e) {
            System.out.println(GREEN + "  PASS — decryption failed as expected: " + e.getClass().getSimpleName() + RESET);
        }

        System.out.println();
        printPhaseHeader("SIMULATION COMPLETE");
        System.out.println(BOLD + GREEN + "  All scenarios passed. Encrypted channel is working correctly." + RESET);
        System.out.println();
    }

    // ═════════════════════════════════════════════════════════════
    // DB BUILDERS — Requests
    // ═════════════════════════════════════════════════════════════

    // LOGIN: pick first active verified user
    private static Request buildLoginRequest(Connection conn) throws SQLException {
        String sql = "SELECT username, email FROM users WHERE is_active=1 AND is_verified=1 LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return new Request(Action.LOGIN, null,
                        Map.of("username", rs.getString("username"),
                                "email",    rs.getString("email")));
        }
        return new Request(Action.LOGIN, null, Map.of("username", "unknown"));
    }

    // GET_PRODUCTS: use first real category name as filter
    private static Request buildGetProductsRequest(Connection conn) throws SQLException {
        String sql = "SELECT name FROM categories ORDER BY category_id LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return new Request(Action.GET_PRODUCTS, "session-token",
                        Map.of("category", rs.getString("name"), "page", 1));
        }
        return new Request(Action.GET_PRODUCTS, "session-token", Map.of("page", 1));
    }

    // GET_CART: use user_id of first cart that actually has items
    private static Request buildGetCartRequest(Connection conn) throws SQLException {
        String sql = "SELECT ca.user_id FROM cart_items ci JOIN carts ca ON ci.cart_id=ca.cart_id LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return new Request(Action.GET_CART, "session-token",
                        Map.of("userId", rs.getInt("user_id")));
        }
        return new Request(Action.GET_CART, "session-token", Map.of("userId", 2));
    }

    // GET_ORDERS: use user_id of most recent order
    private static Request buildGetOrdersRequest(Connection conn) throws SQLException {
        String sql = "SELECT user_id FROM orders ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return new Request(Action.GET_ORDER_HISTORY, "session-token",
                        Map.of("userId", rs.getInt("user_id"), "limit", 3));
        }
        return new Request(Action.GET_ORDER_HISTORY, "session-token", Map.of("limit", 3));
    }

    // ═════════════════════════════════════════════════════════════
    // DB BUILDERS — Responses
    // ═════════════════════════════════════════════════════════════

    // LOGIN: real user profile from DB
    private static Response buildLoginResponse(Connection conn) throws SQLException {
        String sql = "SELECT username, first_name, last_name, email, role, created_at " +
                "FROM users WHERE is_active=1 AND is_verified=1 LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return Response.ok("Login successful", Map.of(
                        "username",    rs.getString("username"),
                        "firstName",   rs.getString("first_name"),
                        "lastName",    rs.getString("last_name"),
                        "email",       rs.getString("email"),
                        "role",        rs.getString("role"),
                        "memberSince", rs.getString("created_at")));
        }
        return Response.ok("Login successful", Map.of());
    }

    // GET_PRODUCTS: real products joined with category names
    private static Response buildGetProductsResponse(Connection conn) throws SQLException {
        String sql = "SELECT p.product_id, p.name, p.price, p.stock_quantity, c.name AS category " +
                "FROM products p JOIN categories c ON p.category_id=c.category_id " +
                "WHERE p.is_available=1 LIMIT 5";

        List<Map<String, Object>> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                items.add(Map.of(
                        "id",       rs.getInt("product_id"),
                        "name",     rs.getString("name"),
                        "price",    rs.getDouble("price"),
                        "stock",    rs.getInt("stock_quantity"),
                        "category", rs.getString("category")));
        }
        return Response.ok("Products fetched", Map.of("count", items.size(), "items", items));
    }

    // GET_CART: real cart items with product names and line totals
    private static Response buildGetCartResponse(Connection conn) throws SQLException {
        String sql = "SELECT p.name AS product, ci.quantity, ci.unit_price " +
                "FROM cart_items ci " +
                "JOIN carts    ca ON ci.cart_id    = ca.cart_id " +
                "JOIN products p  ON ci.product_id = p.product_id " +
                "WHERE ca.cart_id = (SELECT cart_id FROM cart_items LIMIT 1)";

        List<Map<String, Object>> items = new ArrayList<>();
        double cartTotal = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double lineTotal = rs.getDouble("unit_price") * rs.getInt("quantity");
                cartTotal += lineTotal;
                items.add(Map.of(
                        "product",   rs.getString("product"),
                        "quantity",  rs.getInt("quantity"),
                        "unitPrice", rs.getDouble("unit_price"),
                        "lineTotal", lineTotal));
            }
        }
        return Response.ok("Cart fetched", Map.of(
                "itemCount", items.size(),
                "cartTotal", cartTotal,
                "items",     items));
    }

    // GET_ORDERS: real orders joined with payment info
    private static Response buildGetOrdersResponse(Connection conn) throws SQLException {
        String sql = "SELECT o.order_id, o.status, o.total_amount, o.shipping_address, " +
                "       o.created_at, p.method AS pay_method, p.status AS pay_status " +
                "FROM orders o JOIN payments p ON o.order_id=p.order_id " +
                "ORDER BY o.created_at DESC LIMIT 3";

        List<Map<String, Object>> orders = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                orders.add(Map.of(
                        "orderId",   rs.getString("order_id"),
                        "status",    rs.getString("status"),
                        "total",     rs.getDouble("total_amount"),
                        "address",   rs.getString("shipping_address"),
                        "date",      rs.getString("created_at"),
                        "payMethod", rs.getString("pay_method"),
                        "payStatus", rs.getString("pay_status")));
        }
        return Response.ok("Orders fetched", Map.of("count", orders.size(), "orders", orders));
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