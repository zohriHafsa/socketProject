package com.chrionline.server.core;

import com.chrionline.server.security.SYNFloodProtection;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.security.AESCipher;
import com.chrionline.shared.security.RSAUtils;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyPair;
import java.util.Base64;

public class ClientHandler implements Runnable {

    private final Socket    socket;
    private BufferedReader  in;
    private PrintWriter     out;
    private SecretKey       sessionKey;         // ← NEW: AES session key for this client
    private boolean         handshakeCompleted; // ← NEW: guard flag

    public ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        String clientAddr = socket.getInetAddress().getHostAddress();
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            ServerLogger.info("Client connected: " + clientAddr);

            // ── HANDSHAKE (must happen before the message loop) ──
            performHandshake(clientAddr);

            // ── MESSAGE LOOP (now encrypted) ──
            String message;
            while ((message = in.readLine()) != null) {

                // 1. Decrypt the incoming AES-encrypted message
                String decrypted;
                try {
                    decrypted = AESCipher.decrypt(message, sessionKey);
                } catch (Exception e) {
                    ServerLogger.error("Decryption failed from " + clientAddr + ": " + e.getMessage());
                    // Tampered or corrupted message — GCM tag mismatch
                    // Encrypt the error response before sending back
                    sendEncrypted(Response.error("Invalid or tampered message.").toJson());
                    continue;
                }

                ServerLogger.info("Received from " + clientAddr + " [DECRYPTED]: " + decrypted);

                // 2. Rate limiting
                if (!RateLimiter.getInstance().allowRequest(clientAddr)) {
                    ServerLogger.warn("Rate limit exceeded for: " + clientAddr);
                    sendEncrypted(Response.error("Too many requests.").toJson());
                    continue;
                }

                // 3. Route the decrypted request
                Response response = RequestRouter.route(decrypted, socket.getInetAddress());

                // 4. Encrypt the response before sending
                String responseJson = response.toJson();
                ServerLogger.info("Response to " + clientAddr + " [BEFORE ENCRYPT]: " + responseJson);
                sendEncrypted(responseJson);
            }

        } catch (IOException e) {
            ServerLogger.error("Connection lost (" + clientAddr + "): " + e.getMessage());
        } catch (Exception e) {
            ServerLogger.error("Handshake failed (" + clientAddr + "): " + e.getMessage());
        } finally {
            disconnect();
        }
    }


    private void performHandshake(String clientAddr) throws Exception {
        ServerLogger.info("[HANDSHAKE] Starting with " + clientAddr);

        // Generate a fresh RSA key pair for this session
        KeyPair rsaKeyPair = RSAUtils.generateKeyPair();
        ServerLogger.info("[HANDSHAKE] RSA key pair generated");

        // Send public key to client
        String pubKeyBase64 = RSAUtils.encodePublicKey(rsaKeyPair.getPublic());
        out.println("PUBLIC_KEY:" + pubKeyBase64);
        ServerLogger.info("[HANDSHAKE] Public key sent to client");

        // Wait for client's AES key (encrypted with our RSA public key)
        String line = in.readLine();
        if (line == null || !line.startsWith("AES_KEY:")) {
            throw new Exception("Handshake failed: expected AES_KEY, got: " + line);
        }

        String encryptedAesKeyBase64 = line.substring("AES_KEY:".length());

        // Decrypt the AES key using our RSA private key
        byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(encryptedAesKeyBase64);
        byte[] aesKeyBytes = RSAUtils.decryptWithPrivateKey(
                encryptedAesKeyBytes,
                rsaKeyPair.getPrivate()
        );

        // Reconstruct the SecretKey
        sessionKey = AESCipher.keyFromBytes(aesKeyBytes);
        handshakeCompleted = true;

        ServerLogger.info("[HANDSHAKE] Secure channel established with " + clientAddr);
    }

    // Helper — encrypts and sends a response in one call
    private void sendEncrypted(String plaintext) {
        try {
            out.println(AESCipher.encrypt(plaintext, sessionKey));
        } catch (Exception e) {
            ServerLogger.error("Encryption failed: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    public void disconnect() {
        try {
            String ip = socket.getInetAddress().getHostAddress();
            SYNFloodProtection.getInstance().releaseConnection(ip);

            if (socket != null && !socket.isClosed()) socket.close();
            ServerLogger.info("Client disconnected.");
        } catch (IOException e) {
            ServerLogger.error("Error closing socket: " + e.getMessage());
        }
    }
}