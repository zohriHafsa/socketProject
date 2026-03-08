package com.chrionline.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// THIS IS A TEST CLASS :D
public class TestClient {

    public static void main(String[] args) {

        String host = "localhost";
        int port = 8081;

        try (
                Socket socket = new Socket(host, port);
                BufferedReader console =
                        new BufferedReader(new InputStreamReader(System.in));
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out =
                        new PrintWriter(socket.getOutputStream(), true);
        ) {

            System.out.println("[CLIENT] Connected to server.");

            String message;

            while (true) {
                System.out.print("Enter message: ");
                message = console.readLine();

                if (message.equalsIgnoreCase("exit")) {
                    break;
                }

                out.println(message);

                String response = in.readLine();
                System.out.println("[SERVER RESPONSE] " + response);
            }

        } catch (Exception e) {
            System.err.println("[CLIENT] Error: " + e.getMessage());
        }

        System.out.println("[CLIENT] Disconnected.");
    }
}