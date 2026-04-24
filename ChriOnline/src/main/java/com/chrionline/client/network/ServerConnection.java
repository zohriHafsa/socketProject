package com.chrionline.client.network;

import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.security.AESCipher;
import com.chrionline.shared.security.RSAUtils;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerConnection {

    private static ServerConnection instance;

    private static final String HOST = "localhost";
    private static final int    PORT = 9090;

    private Socket         socket;
    private BufferedReader  in;
    private PrintWriter     out;
    private SecretKey sessionKey;
    private boolean secureChannel;


    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Network-IO");
        t.setDaemon(true);
        return t;
    });

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out    = new PrintWriter(socket.getOutputStream(), true);
            performHandshake();
            return true;
        } catch (IOException e) {
            System.err.println("[CLIENT] Connection failed: " + e.getMessage());
            return false;
        }
    }

    public synchronized Response send(Request request) {
        try {
            String json = request.toJson();
            if (secureChannel) {
                out.println(AESCipher.encrypt(json,sessionKey));
            }
            else {
                out.println(json);
            }
            String line = in.readLine();
            if (line==null) {
                return Response.error("Server closed connection.");
            }
            if(secureChannel) {
                line = AESCipher.decrypt(line,sessionKey);
            }
            return Response.fromJson(line);

        } catch (javax.crypto.AEADBadTagException e) {
            return Response.error("Security error: response was tampered.");
        } catch (Exception e) {
            return Response.error("Communication error: " + e.getMessage());
        }

    }

    /**
     * Envoi asynchrone pour ne pas bloquer le thread JavaFX.
     * Le résultat est retourné via CompletableFuture.
     */
    public CompletableFuture<Response> sendAsync(Request request) {
        return CompletableFuture.supplyAsync(() -> send(request), networkExecutor);
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[CLIENT] Disconnect error: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    public String getHost() { return HOST; }
    public int    getPort() { return PORT; }
    private void performHandshake() {
        try {
            String line =in.readLine();
            if (line == null || !line.startsWith("PUBLIC_KEY:")) {
                throw new RuntimeException("Handchake failed: expected PUBLIC_KEY");
            }
            String pubKeyBase64 = line.substring("PUBLIC_KEY:".length());
            PublicKey serverPubKey = RSAUtils.decodePublicKey(pubKeyBase64);
            sessionKey = AESCipher.generateKey();
            byte[] encryptedKey = RSAUtils.encryptWithPublicKey(sessionKey.getEncoded(),serverPubKey);
            String encKeyBase64 = Base64.getEncoder().encodeToString(encryptedKey);
            out.println("AES_KEY:" + encKeyBase64);
            secureChannel = true;
            System.out.println("[HANDSHAKE] Secure channel established.");
        } catch (Exception e) {
        throw new RuntimeException("Handshake failed: " + e.getMessage(), e);
        }
    }
}