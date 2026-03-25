package com.chrionline.server.core;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UDPNotifier {

    private static UDPNotifier instance;
    private DatagramSocket socket;

    /** userId → (address, port) */
    private final Map<Integer, ClientInfo> clients = new ConcurrentHashMap<>();

    // ── Inner class ────────────────────────────
    private static class ClientInfo {
        final InetAddress address;
        final int port;
        ClientInfo(InetAddress address, int port) {
            this.address = address;
            this.port    = port;
        }
    }

    // ── Singleton ──────────────────────────────
    private UDPNotifier() {
        try {
            socket = new DatagramSocket();
            ServerLogger.info("UDP Notifier initialisé");
        } catch (IOException e) {
            ServerLogger.error("UDP Notifier init échoué : " + e.getMessage());
        }
    }

    public static synchronized UDPNotifier getInstance() {
        if (instance == null) instance = new UDPNotifier();
        return instance;
    }

    // ── Enregistrement ─────────────────────────
    public void registerClient(int userId, InetAddress address, int port) {
        clients.put(userId, new ClientInfo(address, port));
        ServerLogger.info("UDP client enregistré : userId=" + userId
                + " → " + address.getHostAddress() + ":" + port);
    }

    public void unregisterClient(int userId) {
        clients.remove(userId);
        ServerLogger.info("UDP client désenregistré : userId=" + userId);
    }

    // ── Notifications ──────────────────────────
    public void notifyOrderStatusChange(int userId, String orderId, String newStatus) {
        send(userId, Map.of(
                "type",      "ORDER_STATUS_CHANGE",
                "orderId",   orderId,
                "newStatus", newStatus,
                "message",   "Commande " + orderId + " → " + newStatus
        ));
    }

    public void notifyOrderConfirmation(int userId, String orderId, double totalAmount) {
        send(userId, Map.of(
                "type",        "ORDER_CONFIRMED",
                "orderId",     orderId,
                "totalAmount", totalAmount,
                "message",     "Commande " + orderId + " confirmée ! Total : " + totalAmount + " €"
        ));
    }

    // ── Envoi ──────────────────────────────────
    private void send(int userId, Map<String, Object> data) {
        ClientInfo ci = clients.get(userId);
        if (ci == null) {
            ServerLogger.warn("Pas de client UDP pour userId=" + userId);
            return;
        }
        try {
            String json   = new Gson().toJson(data);
            byte[] buffer = json.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, ci.address, ci.port);
            socket.send(packet);
            ServerLogger.info("UDP envoyé à userId=" + userId + " : " + json);
        } catch (IOException e) {
            ServerLogger.error("UDP envoi échoué : " + e.getMessage());
        }
    }

    public void close() {
        if (socket != null && !socket.isClosed()) socket.close();
    }
}