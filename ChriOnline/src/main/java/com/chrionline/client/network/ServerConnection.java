package com.chrionline.client.network;

import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
            return true;
        } catch (IOException e) {
            System.err.println("[CLIENT] Connection failed: " + e.getMessage());
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