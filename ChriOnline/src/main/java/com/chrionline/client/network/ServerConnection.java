package com.chrionline.client.network;

import com.chrionline.shared.dto.Request;
import com.chrionline.shared.dto.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection {

    private static ServerConnection instance;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private static final String HOST = "localhost";
    private static final int PORT = 9090;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    // Connect to server
    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("[CLIENT] Connected to server at " + HOST + ":" + PORT);
            return true;
        } catch (IOException e) {
            System.err.println("[CLIENT] Could not connect to server: " + e.getMessage());
            return false;
        }
    }

    // Send Request → get Response
    public Response send(Request request) {
        if (!isConnected()) {
            return Response.error("Not connected to server.");
        }
        try {
            out.println(request.toJson());
            String raw = in.readLine();
            if (raw == null) return Response.error("Server closed the connection.");
            return Response.fromJson(raw);
        } catch (IOException e) {
            System.err.println("[CLIENT] Send error: " + e.getMessage());
            return Response.error("Communication error: " + e.getMessage());
        }
    }

    // Disconnect
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("[CLIENT] Disconnected from server.");
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Error disconnecting: " + e.getMessage());
        }
    }

    // Status
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String getHost() { return HOST; }
    public int getPort() { return PORT; }
}
