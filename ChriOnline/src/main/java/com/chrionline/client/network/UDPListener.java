package com.chrionline.client.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.function.Consumer;

public class UDPListener implements Runnable {

    private static final int BUFFER_SIZE = 4096;

    private DatagramSocket socket;
    private boolean running;
    private final int port;
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

    public int getPort() {
        if (socket != null) return socket.getLocalPort();
        return port;
    }

    @Override
    public void run() {
        try {
            socket  = new DatagramSocket(port);
            running = true;
            System.out.println("[UDP] Listener démarré sur le port " + socket.getLocalPort());

            while (running) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String json = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[UDP] Notification reçue : " + json);

                try {
                    Map<String, Object> data = new Gson().fromJson(
                            json, new TypeToken<Map<String, Object>>(){}.getType());
                    if (onNotification != null) {
                        onNotification.accept(data);
                    }
                } catch (Exception e) {
                    System.err.println("[UDP] Erreur parsing : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[UDP] Listener error : " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) socket.close();
        System.out.println("[UDP] Listener arrêté.");
    }
}