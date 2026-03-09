package com.chrionline.server.core;

import com.chrionline.shared.dto.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("[HANDLER] Client connected: "
                    + socket.getInetAddress().getHostAddress());

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[HANDLER] Received: " + message);
                Response response = RequestRouter.route(message);
                out.println(response.toJson());
            }

        } catch (IOException e) {
            System.err.println("[HANDLER] Connection lost: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("[HANDLER] Client disconnected.");
        } catch (IOException e) {
            System.err.println("[HANDLER] Error closing socket: " + e.getMessage());
        }
    }
}
