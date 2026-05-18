package com.chrionline.server.core;

import com.chrionline.server.security.SYNFloodProtection;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {
    private final int port;
    private final int poolSize;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean running;

    public TCPServer(int port, int poolSize) {
        this.port = port;
        this.poolSize = poolSize;
    }

    private SSLServerSocketFactory createSSLServerSocketFactory() throws Exception {
        java.io.InputStream in = TCPServer.class.getResourceAsStream("/security/server.p12");
        if (in == null) {
            // Fallback for IDE development paths if classpath resource is not resolved
            java.io.File file = new java.io.File("src/main/resources/security/server.p12");
            if (!file.exists()) {
                file = new java.io.File("chrionline-server/src/main/resources/security/server.p12");
            }
            if (file.exists()) {
                in = new java.io.FileInputStream(file);
            } else {
                throw new java.io.FileNotFoundException("Keystore server.p12 resource not found on classpath or disk!");
            }
        }

        try (java.io.InputStream keystoreStream = in) {
            java.security.KeyStore keystore = java.security.KeyStore.getInstance("PKCS12");
            keystore.load(keystoreStream, "chrionline2026".toCharArray());

            javax.net.ssl.KeyManagerFactory kmf =
                    javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, "chrionline2026".toCharArray());

            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            return sslContext.getServerSocketFactory();
        }
    }

    public void start() {
        try {
            // ── TLS : In-memory creation of SSLServerSocket using custom SSLContext ──
            SSLServerSocketFactory factory = createSSLServerSocketFactory();
            serverSocket = factory.createServerSocket(port);
            ServerLogger.info("[TLS] SSLServerSocket créé sur le port " + port);
 
            threadPool = Executors.newFixedThreadPool(poolSize);
            running = true;
            System.out.println("[SERVER] Started on port " + port + " (TLS enabled - In-Memory Keystore)");
            acceptConnections();
        } catch (Exception e) {
            System.err.println("[SERVER] Failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();

                // ── Protection SYN Flood ──
                if (!SYNFloodProtection.getInstance().allowConnection(clientIp)) {
                    ServerLogger.warn("[SECURITY] SYN Flood détecté, connexion rejetée : " + clientIp);
                    clientSocket.close(); // Fermer immédiatement
                    continue;
                }

                System.out.println("[SERVER] New client connected: " + clientIp);
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler);

            } catch (IOException e) {
                if (running) {
                    System.err.println("[SERVER] Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (threadPool != null) {
                threadPool.shutdown();
            }
            System.out.println("[SERVER] Stopped.");
        } catch (IOException e) {
            System.err.println("[SERVER] Error stopping server: " + e.getMessage());
        }
    }
}
