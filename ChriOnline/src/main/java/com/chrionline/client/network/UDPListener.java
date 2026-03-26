package com.chrionline.client.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.function.Consumer;

/**
 * TÂCHE 1 — UDP Listener (thread daemon côté client)
 * ═══════════════════════════════════════════════════
 *
 * RÔLE :
 *   Écoute en permanence sur le port UDP 9091 les notifications push
 *   envoyées par UDPNotifier.java (serveur). Dès qu'un paquet arrive,
 *   il est parsé en JSON et le callback est appelé sur le thread JavaFX.
 *
 * LANCEMENT dans ChriOnlineApp.start() :
 *   UDPListener udp = new UDPListener(notif -> {
 *       String type = notif.get("type").getAsString();
 *       String msg  = notif.get("message").getAsString();
 *       NavigationManager.showNotifications();
 *   });
 *   udp.start();
 *
 * LIEN AVEC REGISTER_UDP :
 *   Après login, le client envoie REGISTER_UDP au serveur pour lui
 *   indiquer son adresse IP et son port UDP 9091. Le serveur peut
 *   alors envoyer des paquets UDP directement à ce client.
 *   Voir AuthController.registerUdp().
 */
public class UDPListener extends Thread {

    public static final int  UDP_PORT    = 9091;
    private static final int BUFFER_SIZE = 4096;

    private final Consumer<JsonObject> onNotification;
    private final Gson                 gson    = new Gson();
    private volatile boolean           running = true;

    public UDPListener(Consumer<JsonObject> onNotification) {
        this.onNotification = onNotification;
        setDaemon(true);
        setName("chrionline-udp-listener");
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            System.out.println("[UDP] Écoute des notifications sur le port " + UDP_PORT);

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);  // bloquant

                String raw = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[UDP] Reçu : " + raw);

                try {
                    JsonObject notification = gson.fromJson(raw, JsonObject.class);
                    // Toujours passer par Platform.runLater pour modifier l'UI
                    Platform.runLater(() -> onNotification.accept(notification));
                } catch (Exception e) {
                    System.err.println("[UDP] JSON invalide : " + raw);
                }
            }
        } catch (Exception e) {
            if (running) System.err.println("[UDP] Erreur : " + e.getMessage());
        }
    }

    public void stopListening() {
        running = false;
        interrupt();
    }
}