package com.chrionline.server.core;

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

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            threadPool = Executors.newFixedThreadPool(poolSize);
            running = true;
            System.out.println("[SERVER] Started on port " + port);
            acceptConnections();
        } catch (IOException e) {
            System.err.println("[SERVER] Failed to start: " + e.getMessage());
        }
    }

    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] New client connected: "
                        + clientSocket.getInetAddress().getHostAddress());
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
