package com.chrionline.server.core;

import com.chrionline.server.security.SYNFloodProtection;
import com.chrionline.shared.dto.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter    out;

    public ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        String clientAddr = socket.getInetAddress().getHostAddress();
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            ServerLogger.info("Client connected: " + clientAddr);

            String message;
            while ((message = in.readLine()) != null) {
                ServerLogger.info("Received from " + clientAddr + ": " + message);

                if (!RateLimiter.getInstance().allowRequest(clientAddr)) {
                    ServerLogger.warn("Rate limit exceeded for: " + clientAddr);
                    out.println(Response.error("Too many requests.").toJson());
                    continue;
                }

                // ── Passe l'adresse IP au routeur pour REGISTER_UDP ──
                Response response = RequestRouter.route(message,
                        socket.getInetAddress());

                String responseJson = response.toJson();
                ServerLogger.info("Response to " + clientAddr + ": " + responseJson);
                out.println(responseJson);
            }

        } catch (IOException e) {
            ServerLogger.error("Connection lost (" + clientAddr + "): " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    public void disconnect() {
        try {
            String ip = socket.getInetAddress().getHostAddress();
            SYNFloodProtection.getInstance().releaseConnection(ip); // ← AJOUTER

            if (socket != null && !socket.isClosed()) socket.close();
            ServerLogger.info("Client disconnected.");
        } catch (IOException e) {
            ServerLogger.error("Error closing socket: " + e.getMessage());
        }
    }
}