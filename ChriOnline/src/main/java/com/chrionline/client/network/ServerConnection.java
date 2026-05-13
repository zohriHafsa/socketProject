package com.chrionline.client.network;

import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.Socket;
import java.security.KeyStore;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerConnection {

    private static ServerConnection instance;

    private static final String HOST = "localhost";
    private static final int    PORT = 9090;

    // ── Chemin du TrustStore client (contient le certificat du serveur) ──
    private static final String TRUSTSTORE_PATH     = "src/main/resources/security/client_truststore.p12";
    private static final String TRUSTSTORE_PASSWORD  = "chrionline2026";

    private Socket         socket;
    private BufferedReader  in;
    private PrintWriter     out;

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
            // ── Configuration TLS via TrustStore ──
            // Charger le TrustStore PKCS12 contenant le certificat du serveur
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            File trustStoreFile = new File(TRUSTSTORE_PATH);
            try (FileInputStream fis = new FileInputStream(trustStoreFile.getAbsolutePath())) {
                trustStore.load(fis, TRUSTSTORE_PASSWORD.toCharArray());
            }

            // Initialiser le TrustManager avec le TrustStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // Créer le SSLContext TLS
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            // Créer le SSLSocket
            SSLSocketFactory factory = context.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) factory.createSocket(HOST, PORT);

            // Forcer le handshake TLS pour vérifier immédiatement
            sslSocket.startHandshake();
            System.out.println("[CLIENT] TLS Handshake réussi avec " + HOST + ":" + PORT);

            socket = sslSocket;
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out    = new PrintWriter(socket.getOutputStream(), true);
            return true;

        } catch (Exception e) {
            System.err.println("[CLIENT] Connexion TLS échouée: " + e.getMessage());
            return false;
        }
    }

    public synchronized Response send(Request request) {
        try {
            out.println(request.toJson());
            String line = in.readLine();
            if (line != null) {
                return Response.fromJson(line);
            }
            return Response.error("Aucune réponse du serveur.");
        } catch (IOException e) {
            return Response.error("Erreur réseau : " + e.getMessage());
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
}