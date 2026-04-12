package com.chrionline.client.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPHeartbeatSender implements Runnable {

    private static final String SERVER_HOST     = "localhost"; // change if server is remote
    private static final int    SERVER_UDP_PORT = 9091;
    private static final int    INTERVAL_MS     = 30_000; // every 30 seconds

    private final int     userId;
    private volatile boolean running = false;

    public UDPHeartbeatSender(int userId) {
        this.userId = userId;
    }

    @Override
    public void run() {
        running = true;
        System.out.println("[UDP Heartbeat] Started for userId=" + userId);

        while (running) {
            try {
                sendHeartbeat();
                Thread.sleep(INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[UDP Heartbeat] Error : " + e.getMessage());
            }
        }

        System.out.println("[UDP Heartbeat] Stopped for userId=" + userId);
    }

    private void sendHeartbeat() throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            String msg    = "HEARTBEAT:" + userId;
            byte[] buffer = msg.getBytes();
            InetAddress address = InetAddress.getByName(SERVER_HOST);
            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, address, SERVER_UDP_PORT);
            socket.send(packet);
            System.out.println("[UDP Heartbeat] Sent → " + msg);
        }
    }

    public void stop() {
        running = false;
    }
}