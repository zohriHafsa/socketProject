package com.chrionline.client.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.function.Consumer;

public class UDPListener implements Runnable {

    private static final int BUFFER_SIZE = 4096;
    private static final Type MAP_TYPE =
            new TypeToken<Map<String, Object>>() {}.getType();

    private final int port;
    private final Gson gson = new Gson();

    private DatagramSocket socket;
    private volatile boolean running;
    private Consumer<Map<String, Object>> onNotification;

    public UDPListener(int port) {
        this.port = port;
    }

    /**
     * Callback appelé à chaque notification reçue.
     */
    public void setOnNotification(Consumer<Map<String, Object>> callback) {
        this.onNotification = callback;
    }

    /**
     * Retourne le vrai port UDP utilisé.
     * Si port = 0, le système choisit un port libre.
     */
    public int getPort() {
        if (socket != null && socket.isBound()) {
            return socket.getLocalPort();
        }
        return port > 0 ? port : -1;
    }

    /**
     * Attend que le socket soit bien initialisé.
     */
    public boolean awaitReady(long timeoutMillis) {
        long end = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < end) {
            if (getPort() > 0) {
                return true;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return getPort() > 0;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);
            running = true;

            System.out.println("[UDP] Listener démarré sur le port " + socket.getLocalPort());

            while (running) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String json = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[UDP] Notification reçue : " + json);

                try {
                    Map<String, Object> data = gson.fromJson(json, MAP_TYPE);

                    if (data != null && onNotification != null) {
                        Platform.runLater(() -> onNotification.accept(data));
                    }
                } catch (Exception e) {
                    System.err.println("[UDP] Erreur parsing : " + e.getMessage());
                }
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("[UDP] Listener error : " + e.getMessage());
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public void stopListening() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("[UDP] Listener arrêté.");
    }
}