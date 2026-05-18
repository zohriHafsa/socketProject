package com.chrionline.client.network;

import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;
import com.chrionline.shared.security.AESCipher;
import com.chrionline.shared.security.RSAUtils;

import javax.crypto.SecretKey;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.Socket;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerConnection {

    private static ServerConnection instance;

    private static final String HOST = "localhost";
    private static final int    PORT = 9090;
    private static final String TRUSTSTORE_PASSWORD = "chrionline2026";

    private Socket         socket;
    private BufferedReader  in;
    private PrintWriter     out;
    private SecretKey       sessionKey;
    private boolean         secureChannel;

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
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            InputStream trustStream = openTruststoreStream();
            if (trustStream == null) {
                throw new FileNotFoundException("client_truststore.p12 introuvable");
            }
            try (InputStream fis = trustStream) {
                trustStore.load(fis, TRUSTSTORE_PASSWORD.toCharArray());
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            SSLSocketFactory factory = context.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) factory.createSocket(HOST, PORT);

            sslSocket.startHandshake();
            System.out.println("[CLIENT] TLS Handshake réussi avec " + HOST + ":" + PORT);

            socket = sslSocket;
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out    = new PrintWriter(socket.getOutputStream(), true);

            performHandshake();

            return true;

        } catch (Exception e) {
            System.err.println("[CLIENT] Connexion sécurisée échouée: " + e.getMessage());
            return false;
        }
    }

    public synchronized Response send(Request request) {
        try {
            String json = request.toJson();
            if (secureChannel) {
                out.println(AESCipher.encrypt(json, sessionKey));
            } else {
                out.println(json);
            }

            String line = in.readLine();
            if (line == null) {
                return Response.error("Server closed connection.");
            }
            if (secureChannel) {
                line = AESCipher.decrypt(line, sessionKey);
            }
            return Response.fromJson(line);

        } catch (javax.crypto.AEADBadTagException e) {
            return Response.error("Security error: response was tampered.");
        } catch (Exception e) {
            return Response.error("Communication error: " + e.getMessage());
        }
    }

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

    private static InputStream openTruststoreStream() {
        String[] paths = {
                "/security/client_truststore.p12",
                "chrionline-client/src/main/resources/security/client_truststore.p12",
                "chrionline-admin/src/main/resources/security/client_truststore.p12",
                "src/main/resources/security/client_truststore.p12"
        };
        for (String path : paths) {
            if (path.startsWith("/")) {
                InputStream in = ServerConnection.class.getResourceAsStream(path);
                if (in != null) return in;
            } else {
                File f = new File(path);
                if (f.isFile()) {
                    try {
                        return new FileInputStream(f);
                    } catch (FileNotFoundException ignored) {
                    }
                }
            }
        }
        return null;
    }

    private void performHandshake() {
        try {
            String line = in.readLine();
            if (line == null || !line.startsWith("PUBLIC_KEY:")) {
                throw new RuntimeException("Handshake failed: expected PUBLIC_KEY");
            }
            String pubKeyBase64 = line.substring("PUBLIC_KEY:".length());
            PublicKey serverPubKey = RSAUtils.decodePublicKey(pubKeyBase64);
            sessionKey = AESCipher.generateKey();
            byte[] encryptedKey = RSAUtils.encryptWithPublicKey(sessionKey.getEncoded(), serverPubKey);
            String encKeyBase64 = Base64.getEncoder().encodeToString(encryptedKey);
            out.println("AES_KEY:" + encKeyBase64);
            secureChannel = true;
            System.out.println("[HANDSHAKE] Secure channel established (AES/RSA).");
        } catch (Exception e) {
            throw new RuntimeException("Handshake failed: " + e.getMessage(), e);
        }
    }
}
